package models

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json.{Json, OFormat}

case class UpdateFileModel( message: String,
                            content:String,
                            sha: String,
                            path: String
)

object UpdateFileModel {
  implicit val formats: OFormat[UpdateFileModel] = Json.format[UpdateFileModel]
  val updateForm: Form[UpdateFileModel] = Form(
    mapping(
      "message" -> nonEmptyText,
      "content" -> nonEmptyText,
      "sha" -> nonEmptyText,
      "path" -> nonEmptyText

    )(UpdateFileModel.apply)(UpdateFileModel.unapply)
  )

}

