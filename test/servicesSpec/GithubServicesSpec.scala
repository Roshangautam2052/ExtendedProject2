package servicesSpec

import connector.connectors.GitHubConnector
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import services.GitHubServices

import scala.concurrent.ExecutionContext

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new GitHubServices(mockConnector)

  val testUser: JsValue = Json.obj (
   "login" -> "SpencerCGriffiths"
  )

  "" should {
    "" when {
      "" in {
    testService.getGitHubUser("SpencerCGriffiths").value map {
      case Right(data) => println("Here",data.userName)

        data.userName shouldBe "SpencerCGriffiths"
        }
      }

    }
  }
}