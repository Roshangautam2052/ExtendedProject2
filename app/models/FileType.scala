package models

import play.api.libs.json.{Json, OFormat}

trait FileType

object FileType {
  implicit val formats: OFormat[FileType] = Json.format[FileType]
  case object dir extends FileType
  case object file extends FileType
}

