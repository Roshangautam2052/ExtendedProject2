package models

import play.api.data.Form
import play.api.data.Forms.{localDate, mapping, nonEmptyText, number}
import play.api.libs.json.{Json, OFormat}

case class UserSearchParameter(userName:String){

}
case object UserSearchParameter {
  implicit val formats: OFormat[UserSearchParameter] = Json.format[UserSearchParameter]
  val userSearchForm: Form[UserSearchParameter] = Form(
    mapping(
      "userName" -> nonEmptyText
    )(UserSearchParameter.apply)(UserSearchParameter.unapply)
  )
}