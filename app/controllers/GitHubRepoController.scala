package controllers

import models.DataModel.userForm
import models.DeleteModel
import models.DeleteModel.deleteForm
import play.api
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.{GitHubServices, RepositoryServices}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubRepoController @Inject()(val controllerComponents: ControllerComponents,
                                      val gitService: GitHubServices, val repoService: RepositoryServices)
                                     (implicit val ex: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {


  def displayDeleteForm(userName:String, repoName:String, sha:String, path:String ): Action[AnyContent] = Action.async { implicit request =>
    val filledForm = deleteForm.fill(DeleteModel("", sha))
    Future.successful(Ok(views.html.deleteRepoOrFile(userName, repoName, path, filledForm)))
  }

  def deleteDirectoryOrFile(owner:String, repo:String, path:String):Action[AnyContent] = Action.async { implicit request =>
    deleteForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful(BadRequest(s"Data not avail: ${formWithErrors}"))
      },
      formData => {
        Future.successful(Ok(   Json.toJson("error handling")))
        })
      }

  def updateDirectoryOrFile():Action[AnyContent] = Action.async { implicit request =>
    ???
  }

  def createDirectoryOrFile():Action[AnyContent] = Action.async { implicit request =>
    ???
  }


}

