package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config._
import scalikejdbc.DBSession
import skinny.orm.SkinnyCRUDMapper
import skinny.{ PermittedStrongParameters, StrongParameters, ParamType => SkinnyParamType }

/**
 * Corresponding Mapper base trait for the above model
 *
 * Contains helper methods to direct how a model should be saved by a
 * Mapper companion object
 */
trait ConfigMapper[A <: ConfigModel[A]] extends SkinnyCRUDMapper[A] {

  /**
   * Converts a given case class-like thing into a Map[Symbol, Any]
   * @param obj case class AnyRef
   * @return Map[Symbol, Any]
   */
  final private def modelToMap(obj: AnyRef): Map[String, Any] = {
    obj.getClass.getDeclaredFields.foldLeft(Map.empty: Map[String, Any]) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(obj))
    }
  }

  /**
   * The fields that can be mass-assigned when saving
   *
   * For more info, see the Strong Parameters section of SkinnyORM
   */
  protected def permittedFields: Seq[(String, SkinnyParamType)]

  /**
   * Returns a PermittedStrongParameters that can be passed to createWithPermittedAttributes
   *
   * @param model
   * @return PermittedStrongParameters sanitised/permitted by permittedFields
   */
  protected def permitted(model: A): PermittedStrongParameters =
    StrongParameters(modelToMap(model)).permit(permittedFields: _*)

  /**
   * By default, simply matches on the Option[Long] id field, and if it is defined,
   * calls updateById using the sanitised version of the models' fields. If it is NOT
   * defined, then calls createWithPermittedAttributes, passing in the same sanitised
   * verison of the fields.
   *
   * Should be overridden to handle more complicated use-cases (such as creating/updating
   * child objects)
   *
   * @param model
   * @return Long, the id of the model that was saved
   */
  def save(model: A)(implicit s: DBSession = autoSession): Long = {
    model.id.fold(createWithPermittedAttributes(permitted(model))) { id =>
      val updatedCount = updateById(id).withPermittedAttributes(permitted(model))
      if (updatedCount != 1) throw new IllegalStateException(s"Save for $model did not update 1 row")
      id
    }
  }

}

/**
 * This companion object exists to hold the type class instances of the [[ConfigMapper]]
 * type class.
 */
object ConfigMapper {
  implicit val CacheGroupMapper: ConfigMapper[CacheGroup] = CacheGroupRepository
  implicit val HttpPartConfigMapper: ConfigMapper[HttpPartConfig] = HttpPartConfigRepository
  implicit val HystrixConfigMapper: ConfigMapper[HystrixConfig] = HystrixConfigRepository
  implicit val PartParamMapper: ConfigMapper[PartParam] = PartParamRepository
  implicit val ThreadPoolConfigMapper: ConfigMapper[ThreadPoolConfig] = ThreadPoolConfigRepository
}