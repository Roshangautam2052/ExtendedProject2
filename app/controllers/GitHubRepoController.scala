package controllers

import models.CreateFileModel.createForm
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


  /** ---------------------------------- Delete File */

  def displayDeleteForm(userName:String, repoName:String, sha:String, path:String, fileName: String ): Action[AnyContent] = Action.async { implicit request =>
    val filledForm = deleteForm.fill(DeleteModel("", sha))
    Future.successful(Ok(views.html.deleteRepoOrFile(userName, repoName, path, filledForm, fileName)))
  }

  def deleteDirectoryOrFile(userName:String, repo:String, path:String, fileName: String):Action[AnyContent] = Action.async { implicit request =>
    deleteForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful(BadRequest(s"Data not avail: ${formWithErrors}"))
      },
      formData => {
        gitService.deleteDirectoryOrFile(userName, repo, path, formData).value.map {
          case Right(delete) => Created(Json.toJson(s"$fileName has been deleted, returned data is $delete"))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      })
      }

  def updateDirectoryOrFile():Action[AnyContent] = Action.async { implicit request =>
    ???
  }

  /** ---------------------------------- Create File Form */

  def displayCreateFileForm(userName: String, repoName: String, path:Option[String]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.createFileForm(userName, repoName,createForm, path)))
  }

  def createFile(userName: String, repo: String, path:Option[String]):Action[AnyContent] = Action.async { implicit request =>
    createForm.bindFromRequest().fold(
      formWithErrors => {
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful(BadRequest(s"Data not avail: ${formWithErrors}"))
      },
      formData => {
        gitService.createFile(userName, repo, formData.fileName, formData, path).value.map {
          case Right(content) => Created(views.html.viewPageContent("Page Created Successfully"))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      })
  }


}

