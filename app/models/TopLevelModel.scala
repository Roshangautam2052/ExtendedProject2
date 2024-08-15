package models

import play.api.libs.json.{Json, OFormat}

case class TopLevelModel(
                       name: String,
                       format: String,
                       )

object TopLevelModel {
  implicit val formats: OFormat[TopLevelModel] = Json.format[TopLevelModel]
}