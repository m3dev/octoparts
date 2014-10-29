package controllers

import com.m3.octoparts.cache.CacheOps
import com.m3.octoparts.model.config._
import com.m3.octoparts.model.config.json.{ HttpPartConfig => JsonHttpPartConfig }
import com.m3.octoparts.repository.MutableConfigsRepository
import controllers.support.{ AuthSupport, LoggingSupport }
import org.joda.time.DateTime
import play.api.data._
import play.api.data.validation.ValidationError
import play.api.http.MediaType
import play.api.i18n.{ Lang, Messages }
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import presentation.{ HttpPartConfigView, NavbarLinks, ParamView }

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{ Success, Failure, Try }
import scala.util.control.NonFatal

class AdminController(cacheOps: CacheOps, repository: MutableConfigsRepository)(implicit val navbarLinks: NavbarLinks = NavbarLinks(None, None, None, None))
    extends Controller with AuthSupport with LoggingSupport {

  import controllers.AdminForms._
  import AdminController._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  /**
   * A sample authorized endpoint for testing that authentication/authorization works.
   */
  def authTest = AuthorizedAction { req =>
    Ok(s"Hi, ${req.principal.nickname}! You're authorized.")
  }

  /*
   * Parts
   */

  def listParts = AuthorizedAction.async { implicit req =>
    val partsView = repository.findAllConfigs().map { configs => configs.map(HttpPartConfigView) }
    partsView.map(ps => Ok(views.html.part.list(ps)))
  }

  def showPart(partId: String) = AuthorizedAction.async { implicit req =>
    findAndUsePart(partId) { part => Future.successful(Ok(views.html.part.show(HttpPartConfigView(part)))) }
  }

  def newPart = AuthorizedAction.async { implicit req =>
    val formWithDefaults = partForm.bind(Map(
      "timeoutInMs" -> "5000",
      "alertInterval" -> "60",
      "alertPercentThreshold" -> "0.5"
    ))
    showPartForm(formWithDefaults, maybePart = None, errorMsg = None)
  }

  def editPart(partId: String) = AuthorizedAction.async { implicit req =>
    findAndUsePart(partId) { part =>
      val data = PartData.fromHttpPartConfig(part)
      showPartForm(partForm.fill(data), maybePart = Some(part), errorMsg = None)
    }
  }

  def createPart = AuthorizedAction.async { implicit req =>
    val form = partForm.bindFromRequest
    form.fold({ formWithErrors =>
      showPartForm(formWithErrors, None, Some(Messages("form.hasErrors")))
    }, { data =>
      repository.findAllCacheGroupsByName(data.cacheGroupNames: _*).flatMap { cacheGroups =>
        val part = data.toNewHttpPartConfig(owner = req.principal.nickname, cacheGroups = cacheGroups.toSet)
        repository.save(part).map { id =>
          Found(controllers.routes.AdminController.showPart(part.partId).url)
        }.recoverWith {
          case NonFatal(e) =>
            // Problem with save: display form again with an error message
            errorRc(e)
            showPartForm(form, None, Some(e.getMessage))
        }
      }
    })
  }

  def updatePart(partId: String) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      val form = partForm.bindFromRequest
      form.fold({ formWithErrors =>
        showPartForm(formWithErrors, Some(part), Some(Messages("form.hasErrors")))
      }, { data =>
        repository.findAllCacheGroupsByName(data.cacheGroupNames: _*).flatMap { cacheGroups =>
          loadParams(part).flatMap { params =>
            val updatedPart = data.toUpdatedHttpPartConfig(part, params.flatten, cacheGroups = cacheGroups.toSet)
            val saveResult = repository.save(updatedPart)
            saveResult.onComplete(_ => if (shouldBustCache(part, updatedPart)) cacheOps.increasePartVersion(partId))
            saveResult.map { id =>
              Found(controllers.routes.AdminController.showPart(updatedPart.partId).url)
            }.recoverWith {
              case NonFatal(e) =>
                // Problem with save: display form again with an error message
                errorRc(e)
                showPartForm(form, Some(part), Some(e.getMessage))
            }
          }
        }
      })
    }
  }

  def copyPart(partId: String) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      repository.findAllConfigs().flatMap { allParts =>
        val existingPartIds = allParts.map(_.partId).toSet
        val existingCommandKeys = allParts.flatMap(_.hystrixConfig).map(_.commandKey).toSet
        val newPart = part.copy(
          id = None,
          parameters = part.parameters.map(_.copy(id = None)),
          hystrixConfig = part.hystrixConfig.map(c => c.copy(id = None, commandKey = AdminController.makeNewName(c.commandKey, existingCommandKeys))),
          partId = AdminController.makeNewName(part.partId, existingPartIds),
          owner = req.principal.nickname
        )
        saveAndRedirect(repository.save(newPart))(routes.AdminController.listParts, _ => routes.AdminController.editPart(newPart.partId))
      }
    }
  }

  def deletePart(partId: String) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      simpleSaveAndRedirect {
        val deleteResult = repository.deleteConfigByPartId(partId)
        deleteResult.onComplete(_ => cacheOps.increasePartVersion(partId))
        deleteResult
      }(routes.AdminController.listParts)
    }
  }

  def showImportParts() = AuthorizedAction { implicit req =>
    Ok(views.html.part.importForm())
  }

  private def importFlashReport(extractedConfigs: Seq[JsonHttpPartConfig], insertedPartIds: Seq[String])(implicit lang: Lang): Flash = {
    val flashInfo = if (insertedPartIds.nonEmpty) {
      Map(BootstrapFlashStyles.success.toString -> Messages("admin.import.successful", insertedPartIds.size, extractedConfigs.size, insertedPartIds.mkString(", ")))
    } else Map.empty[String, String]

    val notInsertedPartIds = for (config <- extractedConfigs if !insertedPartIds.contains(config.partId)) yield config.partId
    val flashWarn = if (notInsertedPartIds.nonEmpty) {
      Map(BootstrapFlashStyles.warning.toString -> Messages("admin.import.failed", notInsertedPartIds.size, extractedConfigs.size, notInsertedPartIds.mkString(", ")))
    } else Map.empty[String, String]

    Flash(flashWarn ++ flashInfo)
  }

  private def extractDataFromImportFile(jsonFile: FilePart[Files.TemporaryFile])(implicit req: RequestHeader): Seq[JsonHttpPartConfig] = {
    import com.m3.octoparts.json.format.ConfigModel._
    val fileContentType = for {
      contentType <- jsonFile.contentType
      mediaType <- MediaType.parse(contentType)
      mimeParameter <- mediaType.parameters.find(_._1 == "charset")
      charset <- mimeParameter._2
    } yield {
      charset
    }
    val fileData = java.nio.file.Files.readAllBytes(jsonFile.ref.file.toPath)
    val jsonData = new String(fileData, fileContentType.headOption.orElse(req.charset).getOrElse("UTF-8"))
    val tryExtract = for {
      json <- Try(Json.parse(jsonData))
      a <- mapJson[Seq[JsonHttpPartConfig]](json)
    } yield {
      a
    }
    tryExtract.get // throws if it was a failure
  }

  def doImportParts() = AuthorizedAction.async(parse.multipartFormData) { implicit req =>
    infoRc
    try {
      req.body.file("jsonfile").fold(Future.successful(BadRequest("Import file not provided"))) { jsonFile =>
        val extractedConfigs = extractDataFromImportFile(jsonFile)
        if (extractedConfigs.isEmpty) {
          Future.successful(Found(routes.AdminController.showImportParts().url).flashing(BootstrapFlashStyles.warning.toString -> Messages("admin.import.none")))
        } else {
          repository.importConfigs(extractedConfigs).map {
            insertedPartIds =>
              val flash = importFlashReport(extractedConfigs, insertedPartIds)
              Found(routes.AdminController.listParts().url).flashing(flash)
          }.recover {
            case NonFatal(e) => handleException(e, routes.AdminController.showImportParts())
          }
        }
      }
    } catch {
      case NonFatal(e) => Future.successful(handleException(e, routes.AdminController.showImportParts()))
    }
  }

  def testPart(partId: String) = AuthorizedAction.async { implicit req =>
    findAndUsePart(partId) { part => Future.successful(Ok(views.html.part.test(HttpPartConfigView(part)))) }
  }

  /*
   * Params
   */

  def newParam(partId: String) = AuthorizedAction.async { implicit req =>
    findAndUsePart(partId) { part =>
      repository.findAllCacheGroups().map { cgs =>
        Ok(views.html.param.edit(partId = part.partId, cacheGroups = cgs, selectedCacheGroupIds = Set.empty, maybeParam = None))
      }
    }
  }

  def editParam(partId: String, paramId: Long) = AuthorizedAction.async { implicit req =>
    findAndUseParam(partId, paramId) { param =>
      repository.findAllCacheGroups().map { cgs =>
        Ok(views.html.param.edit(
          partId = partId, cacheGroups = cgs, selectedCacheGroupIds = param.cacheGroups.map(_.id.get).toSet, maybeParam = Some(ParamView(param))))
      }
    }
  }

  def createParam(partId: String) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      paramForm.bindFromRequest.fold({ formWithErrors =>
        Future.successful(flashError(routes.AdminController.newParam(partId), Messages("admin.validationErrors", formWithErrors.errors)))
      }, { data =>
        repository.findAllCacheGroupsByName(data.cacheGroupNames: _*).flatMap { cacheGroups =>
          val param = PartParam(
            httpPartConfigId = part.id,
            required = data.required,
            versioned = data.versioned,
            paramType = ParamType.withName(data.paramType),
            outputName = data.outputName,
            inputNameOverride = data.inputNameOverride.filterNot(_.isEmpty),
            description = data.description.filterNot(_.isEmpty),
            cacheGroups = cacheGroups.toSet,
            createdAt = DateTime.now,
            updatedAt = DateTime.now)
          saveAndRedirect {
            saveParamAndClearPartResponseCache(partId, param)
          }(routes.AdminController.newParam(partId), id => routes.AdminController.showPart(partId))
        }
      })
    }
  }

  def updateParam(partId: String, paramId: Long) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      paramForm.bindFromRequest.fold({ formWithErrors =>
        Future.successful(flashError(routes.AdminController.editParam(partId, paramId), Messages("admin.validationErrors", formWithErrors.errors)))
      }, { data =>
        findAndUseParam(partId, paramId) { param =>
          repository.findAllCacheGroupsByName(data.cacheGroupNames: _*).flatMap { cacheGroups =>
            val newParam = param.copy(
              httpPartConfigId = part.id,
              required = data.required,
              versioned = data.versioned,
              paramType = ParamType.withName(data.paramType),
              outputName = data.outputName,
              inputNameOverride = data.inputNameOverride.filterNot(_.isEmpty),
              description = data.description.filterNot(_.isEmpty),
              cacheGroups = cacheGroups.toSet,
              updatedAt = DateTime.now)
            saveAndRedirect {
              saveParamAndClearPartResponseCache(partId, newParam)
            }(routes.AdminController.editParam(partId, paramId), _ => routes.AdminController.showPart(partId))
          }
        }
      })
    }
  }

  def copyParam(partId: String, paramId: Long) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      findAndUseParam(partId, paramId) { param =>
        simpleSaveAndRedirect {
          // Create a new param with a unique name based on this param's name
          val otherParamNamesWithSameType = part.parameters.collect {
            case otherParam if otherParam.paramType == param.paramType => otherParam.outputName
          }
          saveParamAndClearPartResponseCache(partId, param.copy(id = None, outputName = AdminController.makeNewName(param.outputName, otherParamNamesWithSameType)))
        }(routes.AdminController.showPart(partId))
      }
    }
  }

  def deleteParam(partId: String, paramId: Long) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUsePart(partId) { part =>
      simpleSaveAndRedirect {
        val deleteResult = repository.deletePartParamById(paramId)
        deleteResult.onComplete(_ => cacheOps.increasePartVersion(partId))
        deleteResult
      }(routes.AdminController.showPart(partId))
    }
  }

  /*
   * Thread pools
   */

  def listThreadPools = AuthorizedAction.async { implicit req =>
    repository.findAllThreadPoolConfigs().map(tpcs => Ok(views.html.threadpool.list(tpcs)))
  }

  def newThreadPool = AuthorizedAction { implicit req =>
    Ok(views.html.threadpool.edit(None))
  }

  def editThreadPool(id: Long) = AuthorizedAction.async { implicit req =>
    findAndUseThreadPool(id) { tpc => Future.successful(Ok(views.html.threadpool.edit(Some(tpc)))) }
  }

  def createThreadPool = AuthorizedAction.async { implicit req =>
    infoRc
    threadPoolForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful {
        flashError(routes.AdminController.newThreadPool, Messages("admin.validationErrors", formWithErrors.errors))
      }
    }, { data =>
      val tpc = ThreadPoolConfig(threadPoolKey = data.threadPoolKey, coreSize = data.coreSize, createdAt = DateTime.now, updatedAt = DateTime.now)
      saveAndRedirect {
        repository.save(tpc)
      }(routes.AdminController.listThreadPools, id => routes.AdminController.editThreadPool(id))
    })
  }

  def updateThreadPool(id: Long) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUseThreadPool(id) { tpc =>
      threadPoolForm.bindFromRequest.fold({ formWithErrors =>
        Future.successful {
          flashError(routes.AdminController.editThreadPool(id), Messages("admin.validationErrors", formWithErrors.errors))
        }
      }, { data =>
        val updatedTpc = tpc.copy(threadPoolKey = data.threadPoolKey, coreSize = data.coreSize, updatedAt = DateTime.now)
        saveAndRedirect {
          repository.save(updatedTpc)
        }(routes.AdminController.listThreadPools, _ => routes.AdminController.editThreadPool(id))
      })
    }
  }

  def deleteThreadPool(id: Long) = AuthorizedAction.async { implicit req =>
    infoRc
    simpleSaveAndRedirect {
      repository.deleteThreadPoolConfigById(id)
    }(routes.AdminController.listThreadPools)
  }

  /*
   * Cache groups
   */

  def listCacheGroups = AuthorizedAction.async { implicit req =>
    repository.findAllCacheGroups().map(cgs => Ok(views.html.cachegroup.list(cgs)))
  }

  def newCacheGroup = AuthorizedAction { implicit req =>
    Ok(views.html.cachegroup.edit(None))
  }

  def editCacheGroup(name: String) = AuthorizedAction.async { implicit req =>
    findAndUseCacheGroup(name) { cg => Future.successful(Ok(views.html.cachegroup.edit(Some(cg)))) }
  }

  def createCacheGroup = AuthorizedAction.async { implicit req =>
    infoRc
    cacheGroupForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful {
        flashError(routes.AdminController.newCacheGroup, Messages("admin.validationErrors", formWithErrors.errors))
      }
    }, { data =>
      val owner = req.principal.nickname // Make the logged-in user the owner
      val cacheGroup = CacheGroup(name = data.name, description = data.description, owner = owner, createdAt = DateTime.now, updatedAt = DateTime.now)
      saveAndRedirect {
        repository.save(cacheGroup)
      }(routes.AdminController.listCacheGroups, id => routes.AdminController.editCacheGroup(cacheGroup.name))
    })
  }

  def updateCacheGroup(name: String) = AuthorizedAction.async { implicit req =>
    infoRc
    findAndUseCacheGroup(name) { cg =>
      cacheGroupForm.bindFromRequest.fold({ formWithErrors =>
        Future.successful {
          flashError(routes.AdminController.editCacheGroup(name), Messages("admin.validationErrors", formWithErrors.errors))
        }
      }, { data =>
        val updatedCacheGroup = cg.copy(name = data.name, description = data.description, updatedAt = DateTime.now)
        saveAndRedirect {
          repository.save(updatedCacheGroup)
        }(routes.AdminController.listCacheGroups, _ => routes.AdminController.editCacheGroup(updatedCacheGroup.name))
      })
    }
  }

  def deleteCacheGroup(name: String) = AuthorizedAction.async { implicit req =>
    infoRc
    simpleSaveAndRedirect {
      repository.deleteCacheGroupByName(name)
    }(routes.AdminController.listCacheGroups)
  }

  /*
   * Helper methods
   */

  private def findAndUsePart(partId: String)(f: HttpPartConfig => Future[Result])(implicit req: RequestHeader): Future[Result] = {
    repository.findConfigByPartId(partId).flatMap { maybePart =>
      maybePart.map(f).getOrElse {
        Future.successful(handlePartNotFound(partId))
      }
    }
  }

  private def loadParams(part: HttpPartConfig): Future[Set[Option[PartParam]]] = {
    /*
    When updating a HttpPartConfig in the current UI, the CacheGroups for the child PartParams
    are left intact (this may change in the future).

    This is because:
    1. PartParams for a HttpPartConfig are saved/edited separately one-by-one
    2. CacheGroups for a PartParam need to be configured from PartParams

    As a result, we need to load the Params for an HttpPartConfig to make sure we have the Params'
    CacheGroup information resolved.
    */
    val paramsWithCacheGroups = for {
      param <- part.parameters
      paramId <- param.id
    } yield {
      repository.findParamById(paramId) // Has CacheGroups loaded
    }
    Future.sequence(paramsWithCacheGroups)
  }

  private def findAndUseParam(partId: String, paramId: Long)(f: PartParam => Future[Result])(implicit req: RequestHeader): Future[Result] = {
    repository.findParamById(paramId).flatMap { maybeParam =>
      maybeParam.map { param =>
        // Sanity check: Check parent's partId matches what was supplied in URL
        if (param.httpPartConfig.exists(_.partId != partId)) {
          warnRc("Param ID" -> paramId.toString, "Error" -> "not found")
          Future.successful(flashError(routes.AdminController.showPart(partId), Messages("admin.partParameterMismatch", paramId, partId)))
        } else {
          f(param)
        }
      } getOrElse {
        warnRc("Param ID" -> paramId.toString, "Error" -> "not found")
        Future.successful(flashError(routes.AdminController.showPart(partId), Messages("admin.parameterNotFound", paramId)))
      }
    }
  }

  private def findAndUseThreadPool(id: Long)(f: (ThreadPoolConfig) => Future[Result])(implicit req: RequestHeader): Future[Result] = {
    repository.findThreadPoolConfigById(id).flatMap { maybeTpc =>
      maybeTpc.map(f).getOrElse {
        warnRc("Thread Pool ID" -> id.toString, "Error" -> "not found")
        Future.successful(flashError(routes.AdminController.listThreadPools, Messages("admin.threadPoolNotFound", id)))
      }
    }
  }

  private def findAndUseCacheGroup(name: String)(f: (CacheGroup) => Future[Result])(implicit req: RequestHeader): Future[Result] = {
    // Type-twiddling to turn an Option[Future[_]] into a Future[Option[_]]
    def populate(maybeCacheGroup: Option[CacheGroup]): Future[Option[CacheGroup]] = {
      val maybeF: Option[Future[CacheGroup]] = maybeCacheGroup.map[Future[CacheGroup]](populateCacheGroup)
      maybeF.fold[Future[Option[CacheGroup]]](Future.successful(None)) { fCacheGroup => fCacheGroup.map(Some(_)) }
    }

    val fMaybeCacheGroup: Future[Option[CacheGroup]] =
      repository.findCacheGroupByName(name).flatMap(populate)

    fMaybeCacheGroup.flatMap { maybeCacheGroup =>
      maybeCacheGroup.map(f).getOrElse {
        warnRc("Cache group name" -> name, "Error" -> "not found")
        Future.successful(flashError(routes.AdminController.listCacheGroups, Messages("admin.cacheGroupNotFound", name)))
      }
    }
  }

  /**
   * Must populate PartParam.httpPartConfig, so that for a given CacheGroup,
   * for each of the PartParams that it has, we can display the HttpPartParam it belongs to.
   * This is used in the 'show cache group' view.
   */
  private def populateCacheGroup(cacheGroup: CacheGroup): Future[CacheGroup] = {
    repository.findAllConfigs().map { allConfigs =>
      val allConfigsMap = allConfigs.map(c => c.id.get -> c).toMap
      cacheGroup.copy(partParams = cacheGroup.partParams.map { partParam =>
        partParam.copy(httpPartConfig = partParam.httpPartConfigId.flatMap(id => allConfigsMap.get(id)))
      })
    }
  }

  /**
   * Display the page for creating/editing a part.
   * @param form Play form object containing user's previously input data, if any
   * @param maybePart the part, if user is editing an existing part
   * @param errorMsg An error message to show as a flash, if any
   */
  private def showPartForm(form: Form[PartData], maybePart: Option[HttpPartConfig], errorMsg: Option[String] = None)(implicit req: RequestHeader): Future[Result] = {
    val fTps = repository.findAllThreadPoolConfigs()
    val fCgs = repository.findAllCacheGroups()
    for {
      tps <- fTps
      cgs <- fCgs
    } yield {
      val flash = errorMsg.fold(req.flash)(error => req.flash + (BootstrapFlashStyles.danger.toString -> error))
      Ok(views.html.part.edit(form, tps, cgs, maybePart)(flash, navbarLinks, implicitly[Lang]))
    }
  }

  private def handlePartNotFound(partId: String)(implicit req: RequestHeader): Result = {
    warnRc("Part" -> partId, "Error" -> "not found")
    flashError(routes.AdminController.listParts, Messages("admin.partNotFound", partId))
  }

  /**
   * A special case of [[saveAndRedirect]] in which we should redirect to the same page regardless of success or error.
   * (In the case of error, an error message will be set in the flash.)
   *
   * @param updater function that saves/updates the object
   * @param redirect where to redirect after completion
   * @tparam R result type of saving the object
   */
  private def simpleSaveAndRedirect[R](updater: => Future[R])(redirect: => Call)(implicit req: RequestHeader): Future[Result] = {
    saveAndRedirect[R](updater)(redirect, { _ => redirect })
  }

  /**
   * Save or update an object to the DB and then perform a redirect.
   * We can redirect to a different page depending on whether the save succeeded or failed.
   * (In the case of error, an error message will be set in the flash.)
   *
   * @param updater function that saves/updates the object
   * @param onError where to redirect to in case of error
   * @param onSuccess function to decide where to redirect to, based on the result of the successful save
   * @tparam R result type of saving the object
   */
  private def saveAndRedirect[R](updater: => Future[R])(onError: => Call, onSuccess: R => Call)(implicit req: RequestHeader): Future[Result] = {
    updater map { result =>
      Found(onSuccess(result).url)
    } recover {
      case NonFatal(e) => handleException(e, onError)
    }
  }

  private def handleException(e: Throwable, redirectTo: Call)(implicit req: RequestHeader): Result = {
    errorRc(e)
    flashError(redirectTo, Option(e.getMessage).getOrElse(e.getClass.getName))
  }

  private def flashError(redirectTo: Call, errorMsg: String): Result = {
    Found(redirectTo.url).flashing(BootstrapFlashStyles.danger.toString -> errorMsg)
  }

  private def saveParamAndClearPartResponseCache(partId: String, param: PartParam): Future[Long] = {
    val saveResult = repository.save(param)
    saveResult.onComplete(_ => if (shouldBustCache(param)) cacheOps.increasePartVersion(partId))
    saveResult
  }
}

object AdminController {

  /**
   * Creates a unique name, by appending `suffix` until the result is not in `reservedNames`.
   */
  @tailrec
  private[controllers] def makeNewName(currentName: String, reservedNames: Set[String], suffix: String = "_"): String = {
    if (!reservedNames.contains(currentName)) {
      currentName
    } else {
      makeNewName(currentName + suffix, reservedNames, suffix)
    }
  }

  private def mapJson[A: Reads](json: JsValue): Try[A] = {
    val jsResult = Json.fromJson[A](json)
    jsResult.fold(
      invalid => Failure(new IllegalArgumentException(s"Invalid JSON: ${jsonErrorMsg(invalid)}")),
      Success.apply
    )
  }

  private def jsonErrorMsg(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    val messages = for {
      (jsPath, validationErrors) <- errors
    } yield {
      val validationErrorMsg = validationErrors.map { validationError =>
        Messages(validationError.message, validationError.args)
      }.mkString(",")
      s"error at: $jsPath reason: $validationErrorMsg"
    }
    messages.mkString("; ")
  }

  def shouldBustCache(beforeEndpoint: HttpPartConfig, afterEndpoint: HttpPartConfig): Boolean = {
    def hasChangedOn[A](accessorGet: HttpPartConfig => A) = accessorGet(beforeEndpoint) != accessorGet(afterEndpoint)
    def cacheTTLReduced: Boolean =
      hasChangedOn(_.cacheTtl) &&
        ((beforeEndpoint.cacheTtl, afterEndpoint.cacheTtl) match {
          case (None, Some(_)) => true
          case (Some(beforeTtl), Some(afterTtl)) if beforeTtl > afterTtl => true
          case _ => false
        })
    hasChangedOn(_.uriToInterpolate) ||
      hasChangedOn(_.method) ||
      hasChangedOn(_.additionalValidStatuses) ||
      cacheTTLReduced
  }

  def shouldBustCache(param: PartParam): Boolean = {
    param.required || param.versioned
  }

}
