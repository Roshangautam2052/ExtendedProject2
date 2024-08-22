package controllers

import models.CreateFileModel.createForm
import models.DeleteModel.deleteForm
import models.FileContent.editForm
import models.UpdateFileModel.updateForm
import models.{DeleteModel, FileContent, UpdateFileModel}
import play.api
import play.api.libs.json.Json
import play.api.mvc._
import play.filters.csrf.CSRF
import services.GitHubServiceTrait

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubRepoController @Inject()(val controllerComponents: ControllerComponents,
                                     val gitService: GitHubServiceTrait)
                                    (implicit val ex: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {


  /** ---------------------------------- Delete File */
  def accessToken(implicit request: Request[_]): Option[CSRF.Token] = {
    CSRF.getToken
  }

  def displayDeleteForm(userName: String, repoName: String, sha: String, path: String, fileName: String): Action[AnyContent] = Action.async { implicit request =>
    val filledForm = deleteForm.fill(DeleteModel("", sha))
    Future.successful(Ok(views.html.deleteRepoOrFile(userName, repoName, path, filledForm, fileName)))
  }

  def deleteDirectoryOrFile(userName: String, repo: String, path: String, fileName: String): Action[AnyContent] = Action.async { implicit request =>
    deleteForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful((BadRequest(views.html.errorPage(BAD_REQUEST, " Error in the delete form."))))
      },
      formData => {
        gitService.deleteDirectoryOrFile(userName, repo, path, formData).value.map {
          case Right(delete) => Created(Json.toJson(s"$fileName has been deleted, returned data is $delete"))
          case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
        }
      })
  }

  /** ------------------------- Get Dirs & Folders */

  def getGitDirsAndFiles(userName: String, repoName: String): Action[AnyContent] = Action.async { implicit request =>
    accessToken
    gitService.getGitDirsAndFiles(userName, repoName).value.map {
      case Right(contents) => Ok(views.html.displayRepoContent(Some(contents), userName, repoName))
      case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
    }
  }

  def getGitRepoFileContent(userName: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    gitService.getGitRepoFileContent(userName, repoName, path).value.map {
      case Right(contents) =>
        val filledForm = editForm.fill(FileContent(contents.content, contents.sha, contents.path))
        Ok(views.html.viewPageContent(filledForm, userName, repoName, path))
      case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
    }
  }

  def openGitDir(userName: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    gitService.openGitDir(userName, repoName, path).value.map {
      case Right(contents) => Ok(views.html.displayRepoContent(Some(contents), userName, repoName, Some(path)))
      case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
    }
  }

  /** ------------------------- GithubRepos */

  def getGitHubRepos(userName: String): Action[AnyContent] = Action.async { implicit request =>
    gitService.getGitHubRepo(userName).value.map {
      case Right(publicRepos) => Ok(views.html.displayUserRepos(Some(publicRepos)))
      case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
    }
  }

  /** ---------------------------------- Create File Form */

  def displayCreateFileForm(userName: String, repoName: String, path: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.createFileForm(userName, repoName, createForm, path)))
  }

  def createFile(userName: String, repo: String, path: Option[String]): Action[AnyContent] = Action.async { implicit request =>

    createForm.bindFromRequest().fold(
      formWithErrors => {
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful((BadRequest(views.html.errorPage(BAD_REQUEST, " Error in the delete form."))))
      },
      formData => {

        gitService.createFile(userName, repo, formData.fileName, formData, path).value.map {
          case Right(content) => Created(Json.toJson(content))
          case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))
        }
      })
  }

  /** ---------------------------------- Update File Form */

  def editContent(userName: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    updateForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful((BadRequest(views.html.errorPage(BAD_REQUEST, " Error in the delete form."))))
      },
      formData => {
        gitService.editContent(userName, repoName, path, formData).value.map {
          case Right(contents) => Ok(Json.toJson(contents))
          case Left(error) => Status(error.httpResponseStatus)(views.html.errorPage(error.httpResponseStatus, error.reason))

        }


      })
  }

  def displayEditContent(userName: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    editForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        api.Logger(s"Form submission errors: ${formWithErrors.errors}")
        Future.successful((BadRequest(views.html.errorPage(BAD_REQUEST, " Error in the delete form."))))
      },
      formData => {
        val filledForm = updateForm.fill(UpdateFileModel("", formData.content, formData.sha, formData.path))
        Future.successful(Ok(views.html.updateFileContent(filledForm, userName, repoName, path)))

      })
  }
}

