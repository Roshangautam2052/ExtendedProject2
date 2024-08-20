package servicesSpec

import cats.data.EitherT
import connector.GitHubConnector
import models._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsObject, JsValue, Json, OFormat}
import services.{GitHubServices, RepositoryServices}

import java.time.LocalDate
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  val testService = new GitHubServices(mockConnector)
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]


  "getGitHubUser" should {
    val userName = "SpencerCGriffiths"
    val url = s"https://api.github.com/users/$userName"
    val testData: JsValue = Json.obj(
      "login" -> "SpencerCGriffiths",
      "location" -> JsNull,
      "followers" -> 2,
      "following" -> 2,
      "created_at" -> "2023-04-07T12:50:03Z",
    )
    val testNotFoundUser: JsValue = Json.obj(
      "message" -> "Not Found",
      "documentation_url" -> "https://docs.github.com/rest",
      "status" -> "404"
    )
    "return the Data Model" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testData))
        .once()

      whenReady(testService.getGitHubUser(userName = "SpencerCGriffiths").value) {
        result =>
          result shouldBe Right(DataModel(
            userName = "SpencerCGriffiths",
            dateAccount = LocalDate.parse("2023-04-07"),
            location = "",
            numberOfFollowers = 2,
            numberFollowing = 2,
            gitHubAccount = true
          ))
      }
    }
    "return a 500 error" in {
      val apiError: APIError = APIError.BadAPIResponse(500, "Could not connect")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT[Future, JsValue](apiError))
        .once()

      whenReady(testService.getGitHubUser(userName).value) { result =>
        result shouldBe Left(apiError)
      }
    }
    "return a 404 error" in {
      val apiNotFound: APIError = APIError.NotFoundError(404, "User not found in Github")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.getGitHubUser(userName).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
  }

  "getGitHubRepo" should {
    val userName = "jamieletts"
    val url = s"https://api.github.com/users/$userName/repos"
    val testRepo: JsValue = Json.arr(Json.obj(
      "owner" -> Json.obj("login" -> "jamieletts"),
      "name" -> "games-project",
      "language" -> "Scala",
      "pushed_at" -> "2023-01-28T12:23:48Z",
    ))
    "return the users repos" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testRepo))
        .once()

      whenReady(testService.getGitHubRepo(userName = "jamieletts").value) {
        result =>
          result shouldBe Right(Seq(PublicRepoDetails(
            userName = "jamieletts",
            name = "games-project",
            language = Some("Scala"),
            pushedAt = "2023-01-28T12:23:48Z"
          )))
      }
    }
    "return a 500 error for unexpected Json format" in {
      val invalidFormat: JsValue = Json.obj(
        "owner" -> Json.obj("login" -> "jamieletts"),
        "name" -> "games-project",
        "language" -> "Scala",
        "pushed_at" -> "2023-01-28T12:23:48Z",
      )
      val apiError: APIError = APIError.BadAPIResponse(500, "Unexpected JSON format")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](invalidFormat))
        .once()

      whenReady(testService.getGitHubRepo(userName).value) { result =>
        result shouldBe Left(apiError)
      }
    }
    "return a 404 error for empty repository" in {
      val emptyArr: JsValue = Json.arr()
      val apiNotFound: APIError = APIError.NotFoundError(404, s"No repositories found for user $userName")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](emptyArr))
        .once()

      whenReady(testService.getGitHubRepo(userName).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
    "return a 404 error when user not found" in {
      val testNotFoundUser: JsValue = Json.obj(
        "message" -> "Not Found",
        "documentation_url" -> "https://docs.github.com/rest",
        "status" -> "404"
      )
      val apiNotFound: APIError = APIError.NotFoundError(404, "Repository not found in Github")
      val userName: String = "NotAUser"
      val url = s"https://api.github.com/users/$userName/repos"
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.getGitHubRepo(userName).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
  }

  "getGitDirsAndFiles" should {
    val repoName = "testRepo"
    val userName = "jamieletts"
    val url = s"https://api.github.com/repos/$userName/$repoName/contents"
    val testFile: JsValue = Json.arr(Json.obj(
      "name" -> "test file",
      "type" -> "file",
      "path" -> ".html",
      "sha" -> "3465647",
    ))
    "return the public files and folders of the github user" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testFile))
        .once()

      whenReady(testService.getGitDirsAndFiles(userName = "jamieletts", repoName = "testRepo").value) {
        result =>
          result shouldBe Right(Seq(FilesAndDirsModel(
            name = "test file",
            format = "file",
            path = ".html",
            sha = "3465647"
          )))
      }
    }
    "return an empty list if repository is empty" in {
      val emptyArr: JsValue = Json.arr()
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](emptyArr))
        .once()

      whenReady(testService.getGitDirsAndFiles(userName, repoName).value) { result =>
        result shouldBe Right(List())
      }
    }
    "return a 500 error for unexpected Json format" in {
      val invalidFormat: JsValue = Json.obj(
        "name" -> "test file",
        "type" -> "file",
        "path" -> ".html",
        "sha" -> "3465647",
      )
      val apiError: APIError = APIError.BadAPIResponse(500, "Unexpected JSON format")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](invalidFormat))
        .once()

      whenReady(testService.getGitDirsAndFiles(userName, repoName).value) { result =>
        result shouldBe Left(apiError)
      }
    }
    "return a 404 error when Directory not found" in {
      val testNotFoundUser: JsValue = Json.obj(
        "message" -> "Not Found",
        "documentation_url" -> "https://docs.github.com/rest",
        "status" -> "404"
      )
      val apiNotFound: APIError = APIError.NotFoundError(404, "Directory not found in Github")
      val userName: String = "NotAUser"
      val url = s"https://api.github.com/repos/$userName/$repoName/contents"
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.getGitDirsAndFiles(userName, repoName).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
  }

  "openGitDir" should {
    val repoName = "testRepo"
    val userName = "jamieletts"
    val path = ".html"
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
    val testFile: JsValue = Json.arr(Json.obj(
      "name" -> "test file",
      "type" -> "file",
      "path" -> ".html",
      "sha" -> "3465647",
    ))
    "open folder and display its contents" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testFile))
        .once()

      whenReady(testService.openGitDir(userName = "jamieletts", repoName = "testRepo", path = ".html").value) {
        result =>
          result shouldBe Right(Seq(FilesAndDirsModel(
            name = "test file",
            format = "file",
            path = ".html",
            sha = "3465647"
          )))
      }
    }
    "return an empty list if repository is empty" in {
      val emptyArr: JsValue = Json.arr()
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](emptyArr))
        .once()

      whenReady(testService.openGitDir(userName, repoName, path).value) { result =>
        result shouldBe Right(List())
      }
    }
    "return a 500 error for unexpected Json format" in {
      val invalidFormat: JsValue = Json.obj(
        "name" -> "test file",
        "type" -> "file",
        "path" -> ".html",
        "sha" -> "3465647",
      )
      val apiError: APIError = APIError.BadAPIResponse(500, "Unexpected JSON format")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](invalidFormat))
        .once()

      whenReady(testService.openGitDir(userName, repoName, path).value) { result =>
        result shouldBe Left(apiError)
      }
    }
    "return a 404 error when Directory not found" in {
      val testNotFoundUser: JsValue = Json.obj(
        "message" -> "Not Found",
        "documentation_url" -> "https://docs.github.com/rest",
        "status" -> "404"
      )
      val apiNotFound: APIError = APIError.NotFoundError(404, "Directory not found in Github")
      val userName: String = "NotAUser"
      val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.openGitDir(userName, repoName, path).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
  }

  "getGitRepoFileContent" should {
    val repoName = "testRepo"
    val userName = "jamieletts"
    val path = ".html"
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
    val testFile: JsValue = Json.obj(
      "content" -> "PGRpdj5IZWxsbzwvZGl2Pg==",
      "sha" -> "3465647",
      "path" -> ".html"
    )
    "open file and display its contents" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testFile))
        .once()

      whenReady(testService.getGitRepoFileContent(userName = "jamieletts", repoName = "testRepo", path = ".html").value) {
        result =>
          result shouldBe Right(FileContent(
            content = "<div>Hello</div>",
            sha = "3465647",
            path = ".html"
          ))
      }
    }
    "return a 500 error for error in response data" in {
      val invalidFormat: JsValue = Json.arr(Json.obj(
        "content" -> "<PGRpdj5IZWxsbzwvZGl2Pg==>",
        "sha" -> "3465647",
        "path" -> ".html"
      ))
      val apiError: APIError = APIError.BadAPIResponse(500, "Error with Github Response Data")
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](invalidFormat))
        .once()

      whenReady(testService.getGitRepoFileContent(userName, repoName, path).value) { result =>
        result shouldBe Left(apiError)
      }
    }
    "return a 404 error when Directory not found" in {
      val testNotFoundUser: JsValue = Json.obj(
        "message" -> "Not Found",
        "documentation_url" -> "https://docs.github.com/rest",
        "status" -> "404"
      )
      val apiNotFound: APIError = APIError.NotFoundError(404, "Directory not found in Github")
      val userName: String = "NotAUser"
      val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.getGitRepoFileContent(userName, repoName, path).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
  }

  "deleteDirectoryOrFile" should {
    val repo = "testRepo"
    val userName = "jamieletts"
    val path = ".html"
    val url = s"https://api.github.com/repos/$userName/$repo/contents/$path"
    val formData = DeleteModel (
      message = "gone",
      sha = "3465647"
    )
    val body = Json.obj(
      "message" -> formData.message,
      "sha" -> formData.sha
    )

    "return Right with a valid response" in {
      val jsonResponse = Json.obj(
        "status" -> "200",
        "message" -> "File deleted successfully"
      )
      (mockConnector.delete[JsValue](_: String, _: JsObject)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, body, *, *)
        .returning(EitherT.rightT[Future, APIError](jsonResponse))
        .once()

      whenReady(testService.deleteDirectoryOrFile(userName, repo, path, formData).value) { result =>
        result shouldBe Right(jsonResponse.toString())
      }
    }
    "return a 404 when directory not found" in {
      val testNotFoundUser: JsValue = Json.obj(
        "message" -> "Not Found",
        "documentation_url" -> "https://docs.github.com/rest",
        "status" -> "404"
      )
      val apiNotFound: APIError = APIError.NotFoundError(404, "Directory not found in Github")
      (mockConnector.delete[JsValue](_: String, _: JsObject)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, body, *, *)
        .returning(EitherT.rightT[Future, APIError](testNotFoundUser))
        .once()

      whenReady(testService.deleteDirectoryOrFile(userName, repo, path, formData).value) { result =>
        result shouldBe Left(apiNotFound)
      }
    }
    "return a 500 for invalid Json data" in {
      val invalidJsonResponse = Json.arr(Json.obj(
        "message" -> "I'm wrong",
        "sha" -> "007",
      ))
      val apiError: APIError = APIError.BadAPIResponse(500, "Error with Github Response Data")
      (mockConnector.delete[JsValue](_: String, _: JsObject)(_: OFormat[JsValue], _: ExecutionContext))
        .expects(url, body, *, *)
        .returning(EitherT.rightT[Future, APIError](invalidJsonResponse))
        .once()

      whenReady(testService.deleteDirectoryOrFile(userName, repo, path, formData).value) { result =>
        result shouldBe Left(apiError)
      }
    }
  }

  "createFile" should {
    "create a new file in a repository" in {
      ???
    }
  }
}

