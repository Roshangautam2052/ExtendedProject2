package models

import play.api.libs.json.{Json, OFormat}

case class TopLeveModel(
                       name: String,
                       format: String,
                       )

object TopLeveModel {
  implicit val formats: OFormat[TopLeveModel] = Json.format[TopLeveModel]
}