package servicesSpec

import cats.data.EitherT
import connector.GitHubConnector
import models._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsValue, Json, OFormat}
import services.{GitHubServices, RepositoryServices}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  val testService = new GitHubServices(mockConnector)
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]



  "getGitHubUser" should {
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
      val userName = "SpencerCGriffiths"
      val url = s"https://api.github.com/users/$userName"
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
      val userName: String = "testUserName"
      val url: String = s"https://api.github.com/users/$userName"
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
      val userName: String = "NotAUser"
      val url: String = s"https://api.github.com/users/$userName"
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
  val testRepo: JsValue = Json.arr(Json.obj(
    "owner" -> Json.obj("login" -> "jamieletts"),
    "name" -> "games-project",
    "language" -> "Scala",
    "pushed_at" -> "2023-01-28T12:23:48Z",
  ))
  "return the users repos" in {
    val userName = "jamieletts"
    val url = s"https://api.github.com/users/$userName/repos"
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
    val userName: String = "jamieletts"
    val url: String = s"https://api.github.com/users/$userName/repos"
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
    val userName: String = "jamieletts"
    val apiNotFound: APIError = APIError.NotFoundError(404, s"No repositories found for user $userName")
    val url: String = s"https://api.github.com/users/$userName/repos"
    (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
      .expects(url, *, *)
      .returning(EitherT.rightT[Future, APIError](emptyArr))
      .once()

    whenReady(testService.getGitHubRepo(userName).value) { result =>
      result shouldBe Left(apiNotFound)
    }
  }
  }

}

