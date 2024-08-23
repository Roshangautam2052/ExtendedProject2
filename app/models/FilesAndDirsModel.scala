package models

import play.api.libs.json.{Json, OFormat}

case class FilesAndDirsModel(
                          name: String,
                          sha: String,
                          format: String,
                          path: String
                        )

object FilesAndDirsModel {
  implicit val formats: OFormat[FilesAndDirsModel] = Json.format[FilesAndDirsModel]
}