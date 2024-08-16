package models

import play.api.data.Form
import play.api.data.Forms.{localDate, mapping, nonEmptyText, number}
import play.api.libs.json.{Json, OFormat}

case class DeleteModel(message: String,
                       sha: String,
                     )

object DeleteModel {
  implicit val formats: OFormat[DeleteModel] = Json.format[DeleteModel]
  val deleteForm: Form[DeleteModel] = Form(
    mapping(
      "message" -> nonEmptyText,
      "sha" -> nonEmptyText

    )(DeleteModel.apply)(DeleteModel.unapply)
  )

}