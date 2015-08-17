package controllers.support

import play.api.data.Form
import play.api.data.Forms._
import scala.beans.BeanProperty

trait PartListFilterSupport {

  val partListFilterForm: Form[PartListFilter] = Form(
    mapping(
      "ids" -> seq(nonEmptyText)
    )(PartListFilter.apply)(PartListFilter.unapply)
  )

}

case class PartListFilter(@BeanProperty ids: Seq[String])