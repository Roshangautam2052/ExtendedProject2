package controllers

import models.DataModel.userForm
import models.UserSearchParameter.userSearchForm
import models.{DataModel, UserSearchParameter}
import play.api
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import play.filters.csrf.CSRF
import services.{GitHubServices, RepositoryServices}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val gitService: GitHubServices, val repoService: RepositoryServices)
                                     (implicit val ex: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {

  /** ---------------------------------------Display Views------------ */

  def accessToken(implicit request: Request[_]) = {
    CSRF.getToken
  }

  def displayForm(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.adduser(userForm)))
  }


  def findUser(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.finduser(userSearchForm, None)))
  }

  def createDatabaseUserForm(): Action[AnyContent] = Action.async { implicit request =>
    accessToken //call the accessToken method
    userForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful(BadRequest(s"Data not avail: ${formWithErrors}"))
      },
      formData => {
        repoService.createUser(formData).map {
          case Right(createdUser) => Created(Json.toJson(createdUser))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      }
    )
  }

  /** --------------------------------------- Create Search Bar */

  def readDatabaseOrAddFromGithub(): Action[AnyContent] = Action.async { implicit request =>
    UserSearchParameter.userSearchForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.finduser(formWithErrors, None))),
      userData => {
        val userName = userData.userName
        repoService.readUser(userName).flatMap {
          case Right(user) => Future.successful(Ok(views.html.finduser(UserSearchParameter.userSearchForm, Some(user))))
          case Left(error) if error.httpResponseStatus == 404 =>
            gitService.getGitHubUser(userName).value.flatMap {
              case Right(dataModel) =>
                repoService.createUser(dataModel).map {
                  case Right(createdUser) => Ok(views.html.finduser(UserSearchParameter.userSearchForm, Some(createdUser)))
                  case Left(error) => Status(error.httpResponseStatus)(views.html.finduser(UserSearchParameter.userSearchForm, None))
                }
              case Left(error) => Future.successful(Status(error.httpResponseStatus)(views.html.finduser(UserSearchParameter.userSearchForm, None)))
            }
          case Left(error) => Future.successful(Status(error.httpResponseStatus)(views.html.finduser(UserSearchParameter.userSearchForm, None)))
        }
      }
    )
  }

  def createDatabaseUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(userModel, _) =>
        repoService.createUser(userModel).map {
          case Right(createdUser) => Created(Json.toJson(createdUser))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      case JsError(_) => Future(BadRequest {
        Json.toJson(s"Invalid Body: ${request.body}")
      })
    }
  }

  def deleteDatabaseUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.deleteDatabaseUser(userName).map {
      case Right(deletedUser) => Accepted(Json.toJson(s"Successfully deleted ${userName}"))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  def updateDatabaseUser(userName: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(userModel, _) =>
        repoService.updateUser(userName, userModel).map {
          case Right(updatedUser) => Accepted(Json.toJson(s"Successfully updated ${userName}"))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      case JsError(_) => Future(BadRequest {
        Json.toJson(s"Invalid Body: ${request.body}")
      })
    }
  }

  /** ---------------------------------------Demo Routes ------------ */

  /** -----*
   * Routes not used in the applications demo purpose only
   */
  def getGitHubUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    gitService.getGitHubUser(userName).value.map {
      case Right(dataModel) => Ok(Json.toJson(dataModel))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  def readDataBaseUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.readUser(userName).map {
      case Right(user) => Ok(Json.toJson(user))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

}

