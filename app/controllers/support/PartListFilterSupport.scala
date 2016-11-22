package controllers.support

import com.wordnik.swagger.annotations.ApiModelProperty
import play.api.data.Form
import play.api.data.Forms._
import scala.annotation.meta.field

trait PartListFilterSupport {

  val partListFilterForm: Form[PartListFilter] = Form(
    mapping(
      "ids" -> seq(nonEmptyText)
    )(PartListFilter.apply)(PartListFilter.unapply)
  )

}

case class PartListFilter(
  @(ApiModelProperty @field)(required = true) ids: Seq[String]
)
