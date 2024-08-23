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

  private var currentUser: Option[DataModel] = None


  def logIn(user: DataModel): Unit = {
    currentUser = Some(user)
  }

  def getCurrentUser: Option[DataModel] = currentUser

  def logOut(): Unit = {
    currentUser = None
  }

  val userForm: Form[DataModel] = Form(
    mapping(
      "userName" -> nonEmptyText,
      "dateAccount" -> localDate("yyyy-MM-dd"),
      "location" -> optional(text).transform(_.getOrElse(""), (str: String) => Some(str)),
      "numberOfFollowers" -> number,
      "numberFollowing" -> number(min=0),
      "gitHubAccount" -> boolean
    )(DataModel.apply)(DataModel.unapply)
  )

}
