package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.GitHubServices

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val service: GitHubServices)(implicit val ex: ExecutionContext) extends BaseController{

  def getUser(userName: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGitHubUser(userName).value.map {
      case Right(dataModel) => Ok {Json.toJson(dataModel)}
      case Left(error) => Status(error.httpResponseStatus)
    }
  }

}
