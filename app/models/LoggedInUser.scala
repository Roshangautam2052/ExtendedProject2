package models

import play.api.libs.json.{Json, OFormat}
import play.api.data._
import play.api.data.Forms._

import java.time.LocalDate

case class LoggedInUser(
                      userName: String,
                      dateAccount: LocalDate,
                      location : String,
                      numberOfFollowers: Int,
                      numberFollowing: Int,
                      gitHubAccount: Boolean
                    ) {
}
object LoggedInUser {
  implicit val formats: OFormat[LoggedInUser] = Json.format[LoggedInUser]

  private var currentUser: Option[LoggedInUser] = None


  def logIn(user: LoggedInUser): Unit = {
    currentUser = Some(user)
  }

  def getCurrentUser: Option[LoggedInUser] = currentUser

  def logOut(): Unit = {
    currentUser = None
  }

  val userForm: Form[LoggedInUser] = Form(
    mapping(
      "userName" -> nonEmptyText,
      "dateAccount" -> localDate("yyyy-MM-dd"),
      "location" -> nonEmptyText,
      "numberOfFollowers" -> number,
      "numberFollowing" -> number(min = 0),
      "gitHubAccount" -> boolean
    )(LoggedInUser.apply)(LoggedInUser.unapply)
  )
}
