package services

import cats.data.EitherT
import com.google.inject.ImplementedBy
import models._

import scala.concurrent.{ExecutionContext, Future}
@ImplementedBy(classOf[GitHubServices])
trait GitHubServiceTrait {

  def getGitHubUser(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, DataModel]

  def getGitHubRepo(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[PublicRepoDetails]]

  def getGitDirsAndFiles(userName: String, repoName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[FilesAndDirsModel]]

  def openGitDir(userName: String, repoName: String, path: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[FilesAndDirsModel]]

  def getGitRepoFileContent(userName: String, repoName: String, path: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, FileContent]

  def deleteDirectoryOrFile(userName: String, repo: String, path: String, formData: DeleteModel)(implicit ex: ExecutionContext): EitherT[Future, APIError, String]

  def createFile(userName: String, repo: String, fileName: String, formData: CreateFileModel, path: Option[String])(implicit ex: ExecutionContext): EitherT[Future, APIError, String]

  def editContent(userName: String, repoName: String, path: String, formData: UpdateFileModel)(implicit ex: ExecutionContext): EitherT[Future, APIError, String]

}

