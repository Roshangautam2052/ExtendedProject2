package servicesSpec

import cats.data.EitherT
import connector.connectors.GitHubConnector
import models. DataModel
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json, OFormat}
import services.GitHubServices

import scala.concurrent.ExecutionContext

class GithubServicesSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {

  val mockConnector: GitHubConnector = mock[GitHubConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new GitHubServices(mockConnector)
  val testData: JsValue = Json.parse(
    """
      |{
      |  "login": "SpencerCGriffiths",
      |  "id": 130156132,
      |  "node_id": "U_kgDOB8IGZA",
      |  "avatar_url": "https://avatars.githubusercontent.com/u/130156132?v=4",
      |  "gravatar_id": "",
      |  "url": "https://api.github.com/users/SpencerCGriffiths",
      |  "html_url": "https://github.com/SpencerCGriffiths",
      |  "followers_url": "https://api.github.com/users/SpencerCGriffiths/followers",
      |  "following_url": "https://api.github.com/users/SpencerCGriffiths/following{/other_user}",
      |  "gists_url": "https://api.github.com/users/SpencerCGriffiths/gists{/gist_id}",
      |  "starred_url": "https://api.github.com/users/SpencerCGriffiths/starred{/owner}{/repo}",
      |  "subscriptions_url": "https://api.github.com/users/SpencerCGriffiths/subscriptions",
      |  "organizations_url": "https://api.github.com/users/SpencerCGriffiths/orgs",
      |  "repos_url": "https://api.github.com/users/SpencerCGriffiths/repos",
      |  "events_url": "https://api.github.com/users/SpencerCGriffiths/events{/privacy}",
      |  "received_events_url": "https://api.github.com/users/SpencerCGriffiths/received_events",
      |  "type": "User",
      |  "site_admin": false,
      |  "name": "Spencer Clarke-Griffiths",
      |  "company": null,
      |  "blog": "",
      |  "location": null,
      |  "email": null,
      |  "hireable": null,
      |  "bio": null,
      |  "twitter_username": null,
      |  "public_repos": 14,
      |  "public_gists": 0,
      |  "followers": 2,
      |  "following": 2,
      |  "created_at": "2023-04-07T12:50:03Z",
      |  "updated_at": "2024-07-02T09:55:22Z"
      |}
""".stripMargin
  )

  "getGoogleBook" should {
    val url: String = "testUrl"
    "return a book" in {
      (mockConnector.get[DataModel](_: String)(_: OFormat[DataModel], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT(testData))
        .once()

      whenReady(testService.getGitHubUser(userName = "SpencerCGriffiths").value) {
        result =>
          result shouldBe EitherT.rightT(DataModel)
      }
    }
  }
}
  /*
  userName: String,
                    dateAccount: LocalDate,
                    location : String,
                    numberOfFollowers: Int,
                    numberFollowing: Int,
                    gitHubAccount: Boolean
   */