package models

import play.api.data.Form
import play.api.data.Forms.{localDate, mapping, nonEmptyText, number}
import play.api.libs.json.{Json, OFormat}

case class FileContent(
                        content:String,
                        sha: String,
                        path: String
                      )
object FileContent {
  implicit val formats: OFormat[FileContent] = Json.format[FileContent]

val editForm: Form[FileContent] = Form(
  mapping(
    "content" -> nonEmptyText,
    "sha" -> nonEmptyText,
    "path" -> nonEmptyText,
  )(FileContent.apply)(FileContent.unapply)
  )
}

