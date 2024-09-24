package controllers

import models.DataModel
import models.UserSearchParameter.userSearchForm

import javax.inject._
import play.api._
import play.api.mvc._
import repository.LoginRepositoryTrait

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val loginRepoService: LoginRepositoryTrait
                              )(implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    loginRepoService.findCurrentUser().map{
      case Right(user) =>  Ok(views.html.index(Some(userSearchForm), user))
      case Left(error) => Ok(views.html.index(Some(userSearchForm), None))
    }
    // TODO:// Handle an immediate error back from the database for login

  }
}
