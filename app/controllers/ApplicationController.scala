package controllers

import models.DataModel.userForm
import models.{APIError, DataModel}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import play.filters.csrf.CSRF
import services.{GitHubServices, RepositoryServices}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val gitService: GitHubServices, val repoService: RepositoryServices)
                                     (implicit val ex: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport{

  def getGitHubUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    gitService.getGitHubUser(userName).value.map {
      case Right(dataModel) => Ok(views.html.displayuser(dataModel))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  /** --------------------------------------- Create Form */

  def accessToken(implicit request: Request[_]) = {
    CSRF.getToken
  }

    def displayForm(): Action[AnyContent] = Action.async { implicit request =>
      accessToken
      Future.successful(Ok(views.html.adduser(userForm)))
    }


  def readDataBaseUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.readUser(userName).map {
      case Right(user) => Ok(Json.toJson(user))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  def readDatabaseOrAddFromGithub(userName: String): Action[AnyContent]= Action.async { implicit request =>
    repoService.readUser(userName).flatMap {
      // Find in DataBase and Return- OR- Go to Github
    case Right(user) => Future.successful(Ok(Json.toJson(user)))
    // If User not in Database condition
    case Left(error) if error.httpResponseStatus == 404 =>

      gitService.getGitHubUser(userName).value.flatMap {
        // Find in Github - Create in Database
        case Right(dataModel) =>
          // Create in the database:
          repoService.createUser(dataModel).map {
            // Display Created User
            case Right(createdUser) => Created(Json.toJson(createdUser))

            // Database create errors/ Repository Service Errors
            case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
          }
          // Github Errors searching for User/ Github Service
        case Left(error) => Future.successful(Status(error.httpResponseStatus))
      }
      // Mongo Database errors/ API Errors in RepoService
    case Left(error) => Future.successful(Status(error.httpResponseStatus)(Json.toJson(error.reason)))
  }
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


}
