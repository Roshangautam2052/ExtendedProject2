package controllers

import models.APIError
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import services.{GitHubServices, RepositoryServices}

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val service: GitHubServices, val repositoryServices: RepositoryServices)(implicit val ex: ExecutionContext) extends BaseController{

  def createUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGitHubUser(userName).value.flatMap {
      case Right(dataModel) => {
        val response = repositoryServices.createUser(dataModel)
        response.map {
          case Right(createdUser) => Created(Json.toJson(createdUser))
          case Left(APIError.BadAPIResponse(status, upstreamMessage)) =>BadRequest(Json.toJson(upstreamMessage))
          case Left(APIError.NotFoundError(status, upstreamMessage)) =>NotFound(Json.toJson(upstreamMessage))
        }
      }
      case Left(error) => Future.successful(Status(error.httpResponseStatus))
    }
  }

}
