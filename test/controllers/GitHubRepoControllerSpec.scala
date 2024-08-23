package controllers

import akka.stream.Materializer
import akka.util.ByteString
import cats.data.EitherT
import models.CreateFileModel.createForm
import models.DeleteModel.deleteForm
import models.UpdateFileModel.updateForm
import models.{APIError, CreateFileModel, DeleteModel, FileContent, FilesAndDirsModel, PublicRepoDetails, UpdateFileModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.FormBinding.Implicits.formBinding
import play.api.http.Status._
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.Helpers.{GET, POST, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import services.{GitHubServiceTrait, RepositoryServices}

import scala.concurrent.Future

class GitHubRepoControllerSpec extends PlaySpec with MockitoSugar {
  val mockGitHubServices: GitHubServiceTrait = mock[GitHubServiceTrait]
  val stubbedControllerComponent: ControllerComponents = Helpers.stubControllerComponents()
  val mockRepoServices: RepositoryServices = mock[RepositoryServices]
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val testGitHubRepoController: GitHubRepoController = new GitHubRepoController(stubbedControllerComponent, mockGitHubServices, mockRepoServices)

  "GitHubRepoController.deleteDirectoryOrFile" should {
    " return 400 there is error in the Form " in {
      val invalidFormData = Map("message" -> "", "sha" -> "nonEmptySha")
      val userName = "testUser"
      val repoName = "testRepo"
      val path = "testPath"
      val fileName = "testFileName"
      val request = FakeRequest(GET, s"/GitHub/repos/$userName/$repoName/$path/$fileName")
        .withFormUrlEncodedBody(invalidFormData.toSeq: _*)
      val result = testGitHubRepoController.deleteDirectoryOrFile(userName, repoName, path, fileName)(request)
      status(result) mustBe BAD_REQUEST


    }
    " return 200 when the file is successfully deleted " in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = "path"
      val fileName = "file.txt"

      val formData = DeleteModel("Commit message", "someSha")
      when(mockGitHubServices.deleteDirectoryOrFile(any(), any(), any(), eqTo(formData))(any()))
        .thenReturn(EitherT.rightT("File deleted"))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET, s"/GitHub/repos/$userName/$repoName/$path/$fileName").withFormUrlEncodedBody("message" -> "Commit message", "sha" -> "someSha")
      val boundForm = deleteForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.deleteDirectoryOrFile(userName, repoName,path, fileName)(request)

      // Assert the status and content of the response
      status(result) mustBe CREATED

    }
    "return appropriate status and error message when the service returns an error " in {
      val userName = "user"
      val repoName = "repo"
      val path = "path"
      val fileName = "file.txt"
      val formData = DeleteModel("Commit message", "someSha")
      when(mockGitHubServices.deleteDirectoryOrFile(any(), any(), any(), eqTo(formData))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Error with Github Response Data")))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET, s"/GitHub/repos/$userName/$repoName/$path/$fileName").withFormUrlEncodedBody("message" -> "Commit message", "sha" -> "someSha")
      val boundForm = deleteForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.deleteDirectoryOrFile(userName, repoName, path, fileName)(request)

      // Assert the status and content of the response
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
  }
  "GitHubRepoController.getGitDirsAndFiles" should {
    "return 200 Ok when the contents are successfully retrieved by service " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val topLevelModelSeq:Seq[FilesAndDirsModel] = Seq(FilesAndDirsModel(
        "fileName",
        "nonEmptySha",
        "file",
        "anyPath"
      ))
      when(mockGitHubServices.getGitDirsAndFiles(eqTo("testUserName"), eqTo("testRepoName"))(any()))
        .thenReturn(EitherT.rightT(topLevelModelSeq))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName/$repoName/$path ")
      val result: Future[Result] = testGitHubRepoController.getGitDirsAndFiles(userName, repoName)(request)
      status(result) mustBe OK
      result.map { response =>
        response mustBe topLevelModelSeq
      }
    }
    "return error with error message and status  when there is error in the service " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val topLevelModelSeq:Seq[FilesAndDirsModel] = Seq(FilesAndDirsModel(
        "fileName",
        "nonEmptySha",
        "file",
        "anyPath"
      ))
      when(mockGitHubServices.getGitDirsAndFiles(eqTo("testUserName"), eqTo("testRepoName"))(any()))
        .thenReturn(EitherT.leftT(APIError.NotFoundError(404, "User not found in Github")))

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName/$repoName/$path ")
      val result: Future[Result] = testGitHubRepoController.getGitDirsAndFiles(userName, repoName)(request)
      status(result) mustBe NOT_FOUND
    }
  }
  "GitHubRepoController.getGitRepoFileContent" should{
//    "return 2OO Ok when a FileContent object has been returned by the service" in {
//      val userName = "testUserName"
//      val repoName = "testRepoName"
//      val path = "anyPath"
//      val fileContent: FileContent = FileContent(
//        "FileContent",
//        "nonEmptySha",
//        "path"
//      )
//
//      // Mock the service call to return the file content
//      when(mockGitHubServices.getGitRepoFileContent(eqTo(userName), eqTo(repoName), eqTo(path))(any()))
//        .thenReturn(EitherT.rightT(fileContent))
//
//      // Create a FakeRequest with CSRF token
//      val request = FakeRequest(GET, s"/Github/repo/content/$userName/$repoName/$path")
//        .withCSRFToken // Add CSRF token
//
//      // Execute the controller action with the FakeRequest
//      val result: Accumulator[ByteString, Result] = testGitHubRepoController.getGitRepoFileContent(userName, repoName, path)(request)
//
//      // Assert the result status
//      status(result) mustBe OK
//
//
//    }
    "return error with error message when error is returned by the service" in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"

      when(mockGitHubServices.getGitRepoFileContent(eqTo("testUserName"), eqTo("testRepoName"),eqTo("anyPath"))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Error with Github Response Data")))
      implicit val request: FakeRequest[AnyContentAsEmpty.type]  = FakeRequest(GET,  s"/Github/repo/content/$userName/$repoName/$path")
      val result = testGitHubRepoController.getGitRepoFileContent(userName, repoName, path)(request)
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
  "GitHubRepoController.openGitDir" should {
    "return 200 Ok when the directory  are successfully retrieved by service " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val topLevelModelSeq: Seq[FilesAndDirsModel] = Seq(FilesAndDirsModel(
        "fileName",
        "nonEmptySha",
        "dir",
        "anyPath"
      ))
      when(mockGitHubServices.openGitDir(eqTo("testUserName"), eqTo("testRepoName"), eqTo("anyPath"))(any()))
        .thenReturn(EitherT.rightT(topLevelModelSeq))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName/$repoName/$path ")
      val result: Future[Result] = testGitHubRepoController.openGitDir(userName, repoName, path)(request)
      status(result) mustBe OK
      result.map { response =>
        response mustBe topLevelModelSeq
      }
    }
    "return error with error message and status  when user is not found in GitHub " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val topLevelModelSeq: Seq[FilesAndDirsModel] = Seq(FilesAndDirsModel(
        "fileName",
        "nonEmptySha",
        "file",
        "anyPath"
      ))
      when(mockGitHubServices.openGitDir(eqTo("testUserName"), eqTo("testRepoName"), eqTo("anyPath"))(any()))
        .thenReturn(EitherT.leftT(APIError.NotFoundError(404, "User not found in Github")))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName/$repoName/$path ")
      val result: Future[Result] = testGitHubRepoController.openGitDir(userName, repoName, path)(request)
      status(result) mustBe NOT_FOUND
    }
    "return  500 Internal Server Error  and status  when unexpected JSON format is encountered " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val topLevelModelSeq: Seq[FilesAndDirsModel] = Seq(FilesAndDirsModel(
        "fileName",
        "nonEmptySha",
        "file",
        "anyPath"
      ))
      when(mockGitHubServices.openGitDir(eqTo("testUserName"), eqTo("testRepoName"), eqTo("anyPath"))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Unexpected JSON format")))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName/$repoName/$path ")
      val result: Future[Result] = testGitHubRepoController.openGitDir(userName, repoName, path)(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
  }
  "GitHubRepoController.getGitHubRepos" should {
    "return 200 Ok when the repository are successfully retrieved by service " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val userRepoDetails: Seq[PublicRepoDetails] = Seq(PublicRepoDetails(
        "UserName", "RepoName", Some("language"), "someDate"
      ))
      when(mockGitHubServices.getGitHubRepo(eqTo("testUserName"))(any()))
        .thenReturn(EitherT.rightT(userRepoDetails))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName ")
      val result: Future[Result] = testGitHubRepoController.getGitHubRepos(userName)(request)
      status(result) mustBe OK
      result.map { response =>
        response mustBe userRepoDetails
      }
    }
    "return 404 with error message and status  when no repositories is found in GitHub " in {
      val userName = "testUserName"
      val userRepoDetails: Seq[PublicRepoDetails] = Seq(PublicRepoDetails(
        "UserName", "RepoName", None, "someDate"
      ))
      when(mockGitHubServices.getGitHubRepo(eqTo("testUserName"))(any()))
        .thenReturn(EitherT.leftT(APIError.NotFoundError(404, s"No repositories found for user $userName")))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName")
      val result: Future[Result] = testGitHubRepoController.getGitHubRepos(userName)(request)
      status(result) mustBe NOT_FOUND
    }
    "return  500 Internal Server Error  and status  when unexpected JSON format is encountered " in {
      val userName = "testUserName"
      val repoName = "testRepoName"
      val path = "anyPath"
      val userRepoDetails: Seq[PublicRepoDetails] = Seq(PublicRepoDetails(
        "UserName", "RepoName", None, "someDate"
      ))
      when(mockGitHubServices.getGitHubRepo(eqTo("testUserName"))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Unexpected JSON format")))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/Github/repo/dir/$userName ")
      val result: Future[Result] = testGitHubRepoController.getGitHubRepos(userName)(request)
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
  "GitHubRepoController.createFile" should {
    " return 400 there is error in the Form " in {
      val invalidCreateForm = Map("message" -> "", "content" -> "nonEmptyContent", "fileName" -> "file.txt")
      val userName = "testUser"
      val repoName = "testRepo"
      val path =  Some("testPath")
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, s"/GitHub/createFileController/showForm/$userName/$repoName/")
        .withFormUrlEncodedBody(invalidCreateForm.toSeq: _*)
      val result = testGitHubRepoController.createFile(userName, repoName, path)(request)
      val boundForm = createForm.bindFromRequest()
      boundForm.hasErrors mustBe true
      status(result) mustBe BAD_REQUEST

    }
    " return 200 when the file is successfully created in GitHub" in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = Some("path")
      val fileName = "file.txt"
      val createFileModel:CreateFileModel = CreateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "file.txt"
      )
      when(mockGitHubServices.createFile(eqTo("user"), eqTo("repo"), eqTo("file.txt"), eqTo(createFileModel),eqTo(path))(any()))
        .thenReturn(EitherT.rightT("Some file content"))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, s"/GitHub/createFileController/showForm/$userName/$repoName/")
        .withFormUrlEncodedBody( "message" -> "nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "fileName" -> "file.txt")
      val boundForm = createForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.createFile(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe CREATED
    }
    "return 404 status when the user is not found in GitHub " in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = Some("path")
      val fileName = "file.txt"
      val createFileModel:CreateFileModel = CreateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "file.txt"
      )
      when(mockGitHubServices.createFile(eqTo("user"), eqTo("repo"), eqTo("file.txt"), eqTo(createFileModel),eqTo(path))(any()))
        .thenReturn(EitherT.leftT(APIError.NotFoundError(404, "User not found in Github")))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, s"/GitHub/createFileController/showForm/$userName/$repoName/")
        .withFormUrlEncodedBody( "message" -> "nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "fileName" -> "file.txt")
      val boundForm = createForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.createFile(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe NOT_FOUND
    }
    "return 500 status when there is Error in GitHub Response Data " in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = Some("path")
      val fileName = "file.txt"
      val createFileModel:CreateFileModel = CreateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "file.txt"
      )
      when(mockGitHubServices.createFile(eqTo("user"), eqTo("repo"), eqTo("file.txt"), eqTo(createFileModel),eqTo(path))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Error with Github Response Data")))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(POST, s"/GitHub/createFileController/showForm/$userName/$repoName/")
        .withFormUrlEncodedBody( "message" -> "nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "fileName" -> "file.txt")
      val boundForm = createForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.createFile(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
  "GitHubRepoController.editContent" should {
    " return 400 there is error in the UpdateForm " in {
      val invalidUpdateForm = Map("message" -> "", "content" -> "nonEmptyContent", "sha" -> "file.txt", "path" -> "")
      val userName = "testUser"
      val repoName = "testRepo"
      val path = "testPath"
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET ,s"/GitHub/editFile/editForm/$userName/$repoName/$path")
        .withFormUrlEncodedBody(invalidUpdateForm.toSeq: _*)
      val result = testGitHubRepoController.editContent(userName, repoName, path)(request)
      val boundForm = createForm.bindFromRequest()
      boundForm.hasErrors mustBe true
      status(result) mustBe BAD_REQUEST

    }
    " return 200 when the file is successfully Updated in GitHub" in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = "testPath"
      val fileName = "file.txt"
      val updateFileModel:UpdateFileModel = UpdateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "nonEmptySha",
        "somePath"
      )
      when(mockGitHubServices.editContent(eqTo("user"), eqTo("repo"), eqTo("testPath"), eqTo(updateFileModel))(any()))
        .thenReturn(EitherT.rightT("Some deleted Content"))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET, s"/GitHub/createFileController/showForm/$userName/$repoName/$path")
        .withFormUrlEncodedBody( "message" ->"nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "sha" -> "nonEmptySha",
          "path" -> "somePath")
      val boundForm = updateForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.editContent(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe OK
    }
    "return 404 status when the user is not found in GitHub " in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = "testPath"
      val updateFileModel:UpdateFileModel = UpdateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "nonEmptySha",
        "somePath"
      )
      when(mockGitHubServices.editContent(eqTo("user"), eqTo("repo"), eqTo("testPath"), eqTo(updateFileModel))(any()))
        .thenReturn(EitherT.leftT(APIError.NotFoundError(404, "User not found in Github")))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET, s"/GitHub/createFileController/showForm/$userName/$repoName/$path")
        .withFormUrlEncodedBody( "message" ->"nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "sha" -> "nonEmptySha",
          "path" -> "somePath")
      val boundForm = updateForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.editContent(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe NOT_FOUND
      contentAsString(result) must include("User not found in Github")
    }
    "return 500 status when there is Error in GitHub Response Data " in {
      // Mock the gitService.deleteDirectoryOrFile method to return a successful result
      val userName = "user"
      val repoName = "repo"
      val path = "testPath"
      val updateFileModel:UpdateFileModel = UpdateFileModel(
        "nonEmptyMessage",
        "nonEmptyContent",
        "nonEmptySha",
        "somePath"
      )
      when(mockGitHubServices.editContent(eqTo("user"), eqTo("repo"), eqTo("testPath"), eqTo(updateFileModel))(any()))
        .thenReturn(EitherT.leftT(APIError.BadAPIResponse(500, "Error with Github Response Data")))
      // Simulate a valid form submission
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(GET, s"/GitHub/createFileController/showForm/$userName/$repoName/$path")
        .withFormUrlEncodedBody( "message" ->"nonEmptyMessage",
          "content" -> "nonEmptyContent",
          "sha" -> "nonEmptySha",
          "path" -> "somePath")
      val boundForm = updateForm.bindFromRequest()
      boundForm.hasErrors mustBe false
      val result: Future[Result] = testGitHubRepoController.editContent(userName, repoName,path)(request)
      // Assert the status and content of the response
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Error with Github Response Data")
    }
  }


}

