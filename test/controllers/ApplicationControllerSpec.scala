package controllers

import baseSpec.BaseSpecWithApplication
import models.DataModel
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.NotFound
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status}

import java.time.LocalDate
import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())

  val TestApplicationController = new ApplicationController(controllerComponents = component,
    gitService = gitService, repoService = repoService)

  val testData: DataModel = DataModel(
    userName = "SpencerCGriffiths",
    dateAccount = LocalDate.parse("2023-04-07"),
    location = "",
    numberOfFollowers = 2,
    numberFollowing = 2,
    gitHubAccount = true
  )
  val invalidUser:String = "hell124124"


  "ApplicationController.getGitHubUser" should {
    "return a user" when {
      "the user exits in GitHub" in {
        beforeEach()
        val request: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/githubDemo/${testData.userName}")
        val result = TestApplicationController.getGitHubUser(testData.userName)(request)
        val content = contentAsJson(result)
        println(content)
        status(result) shouldBe OK
        content shouldBe (Json.toJson(testData))
        afterEach()
      }
    }
    "return a 404 error " when {
      "the user does not exits in GitHub" in {
        beforeEach()
        val request:FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/githubDemo/$invalidUser")
        val result = TestApplicationController.getGitHubUser(invalidUser)(request)
        status(result) shouldBe NOT_FOUND

      }
    }
  }
}
