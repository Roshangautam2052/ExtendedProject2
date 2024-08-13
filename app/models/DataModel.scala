package models

import play.api.libs.json.{Json, OFormat}
import play.api.data._
import play.api.data.Forms._

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
  val userForm: Form[DataModel] = Form(
    mapping(
      "userName" -> nonEmptyText,
      "dateAccount" -> localDate("yyyy-MM-dd"),
      "location" -> nonEmptyText,
      "numberOfFollowers" -> number,
      "numberFollowing" -> number(min=0),
      "gitHubAccount" -> boolean
    )(DataModel.apply)(DataModel.unapply)
  )

}
