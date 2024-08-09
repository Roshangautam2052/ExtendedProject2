package models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class DataModel(
                    userName: String,
                    dateAccount: LocalDate,
                    location : String,
                    numberOfFollowers: Int,
                    numberFollowing: Int,
                    gitHubAccount: Boolean
                    ) {
}
object DataModel{
  implicit val formats: OFormat[DataModel] = Json.format[DataModel]
}

