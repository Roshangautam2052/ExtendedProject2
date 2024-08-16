package models

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json.{Json, OFormat}

case class CreateFileModel(message: String,
                           content: String,
                           fileName: String
                          )

object CreateFileModel {
  implicit val formats: OFormat[CreateFileModel] = Json.format[CreateFileModel]
  val createForm: Form[CreateFileModel] = Form(
    mapping(
      "message" -> nonEmptyText,
      "content" -> nonEmptyText,
      "fileName" -> nonEmptyText

    )(CreateFileModel.apply)(CreateFileModel.unapply)
  )

}
