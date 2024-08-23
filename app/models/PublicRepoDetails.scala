package models

import play.api.libs.json.{Json, OFormat, Reads}

case class PublicRepoDetails (userName: String, name: String, language: Option[String], pushedAt: String)

object PublicRepoDetails {
//  implicit val reads: Reads[PublicRepoDetails] = Json.reads[PublicRepoDetails]
  implicit val formats: OFormat[PublicRepoDetails] = Json.format[PublicRepoDetails]
}