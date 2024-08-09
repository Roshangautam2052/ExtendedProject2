package servicesSpec

import connector.connectors.GitHubConnector
import org.scalamock.scalatest.MockFactory

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.GitHubServices

import scala.concurrent.ExecutionContext

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new GitHubServices(mockConnector)

  "" should {
    "" when {
      "return a GithubUser mapped to DataModel" in {
        testService.getGitHubUser("SpencerCGriffiths")
      }

    }
  }
}