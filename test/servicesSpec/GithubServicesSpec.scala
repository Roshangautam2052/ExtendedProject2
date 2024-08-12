package servicesSpec

import cats.data.EitherT
import connector.GitHubConnector
import models.{APIError, DataModel}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{JsNull, JsValue, Json, OFormat}
import services.GitHubServices

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.tools.nsc.interactive.Response

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new GitHubServices(mockConnector)
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

  "getGitHubUser" should {
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
        println(result)
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
}
