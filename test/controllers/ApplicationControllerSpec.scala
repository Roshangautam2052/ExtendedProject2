package controllers

import baseSpec.BaseSpecWithApplication
import models.DataModel.userForm
import models.{DataModel, UserSearchParameter}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.data.Form
import play.api.http.Status
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, NOT_FOUND, NOT_MODIFIED, OK}
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.mvc.Results.{Accepted, NotFound}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, await, contentAsJson, defaultAwaitTimeout, status}

import java.time.LocalDate
import scala.concurrent.{Await, Future}

class ApplicationControllerSpec extends BaseSpecWithApplication {

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())

  val TestApplicationController = new ApplicationController(controllerComponents = component,
    gitService = gitService, repoService = repoService)

  val testDataNotOnGitHub: DataModel = DataModel(
    userName = "SpencerCGriffiths{}{}{}", // this would be invalid so not on github
    dateAccount = LocalDate.parse("2023-04-07"),
    location = "",
    numberOfFollowers = 2,
    numberFollowing = 2,
    gitHubAccount = true
  )
val testDataNotOnGitHubUpdate: DataModel = DataModel(
    userName = "SpencerCGriffiths{}{}{}Update", // this would be invalid so not on github
    dateAccount = LocalDate.parse("2023-04-07"),
    location = "",
    numberOfFollowers = 2,
    numberFollowing = 2,
    gitHubAccount = true
  )

  val testDataGitValid: DataModel = DataModel(
    userName = "SpencerCGriffiths",
    dateAccount = LocalDate.parse("2023-04-07"),
    location = "",
    numberOfFollowers = 2,
    numberFollowing = 2,
    gitHubAccount = true
  )


  val testUserSearchForm: UserSearchParameter = UserSearchParameter(
    userName = "GarysRobot"
  )

  private val invalidTestData: String = "invalid Json DataModel"

  val invalidUser:String = "hell124124"


  "ApplicationController.getGitHubUser" should {

    "return a user" when {

      "the user exits in GitHub" in {
        beforeEach()
        val request: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/githubDemo/${testDataGitValid.userName}")
        val result = TestApplicationController.getGitHubUser(testDataGitValid.userName)(request)
        val content = contentAsJson(result)
        println(content)
        status(result) shouldBe OK
        content shouldBe (Json.toJson(testDataGitValid))
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

  "ApplicationController.readDataBaseUser" should {

    "return a user from the database" when {
      "given a valid username and user exists" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED

        val result = TestApplicationController.readDataBaseUser(testDataNotOnGitHub.userName)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result).as[DataModel] shouldBe testDataNotOnGitHub
        afterEach()
      }
    }

    "return a 404 error from database" when {
      "when the username does not exist in the database" in {
        beforeEach()
        val result = TestApplicationController.readDataBaseUser(testDataNotOnGitHub.userName)(FakeRequest())

        status(result) shouldBe NOT_FOUND
        afterEach()
      }
    }
  }

  "ApplicationController.createDataBaseUser" should {

    "return a Created 201 status and the created user" when {
      "a valid Json is sent for creation" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED
        contentAsJson(createResult).as[DataModel] shouldBe testDataNotOnGitHub

        afterEach()
      }
    }

    "return a Bad Request 400 Status and error message" when {
      "invalid Json that cannot be converted to a DataModel is sent" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(invalidTestData))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.BAD_REQUEST
        contentAsJson(createResult) shouldBe Json.toJson(s"Invalid Body: \"invalid Json DataModel\"")

        afterEach()

      }
    }
  }

  "ApplicationController.readDatabaseOrAddFromGithub" should {

    "return a view with the user and 200 Ok" when {

      "the user exists in the database" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED

        val readRequest: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/api/read?userName=${testDataNotOnGitHub.userName}")
        val result = TestApplicationController.readDatabaseOrAddFromGithub()(readRequest)

        status(result) shouldBe OK
        whenReady(result) { result =>
          val bodyFuture = result.body.consumeData.map(_.utf8String)
          val responseBody = Await.result(bodyFuture, 10.seconds)

          responseBody should include (s"${testDataNotOnGitHub.userName}")
        }
        afterEach()
      }

      "the user does not exist in the database but does in github" in {
        beforeEach()

        val readRequest: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/api/read?userName=${testDataGitValid.userName}")
        val result = TestApplicationController.readDatabaseOrAddFromGithub()(readRequest)

        status(result) shouldBe OK
        whenReady(result) { result =>
          val bodyFuture = result.body.consumeData.map(_.utf8String)
          val responseBody = Await.result(bodyFuture, 10.seconds)

          responseBody should include (s"${testDataGitValid.userName}")
        }
        afterEach()
      }
    }

    "returns the appropriate error [###]" when {
      // SCG - Left in readUser - Have to mock to test the 500 database error for MongoDB
      // SCG - Left in getGitHubUser - Have to mock to test the 500 database error for Git i.e. returning non valid object
      // SCG - Left in createUser - Have to mock response from GitHub for it not to be valid dataModel to store in database
      "[400- BadRequest] the userName was not provided and search processed" in {
        beforeEach()
        val requestWithErrors: FakeRequest[AnyContentAsEmpty.type] = buildGet("/api/read?userName=")
        val result = TestApplicationController.readDatabaseOrAddFromGithub()(requestWithErrors)

        status(result) shouldBe BAD_REQUEST
        afterEach()
      }

      "[404- NotFound] the userName was provided and search processed but no user found" in {
        beforeEach()
        val notAGitHubUserName = "ThisCannotBeAGitHubUserThatWouldBeCrazy"
        val requestWithErrors: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/api/read?userName=$notAGitHubUserName")
        val result = TestApplicationController.readDatabaseOrAddFromGithub()(requestWithErrors)

        status(result) shouldBe NOT_FOUND
        afterEach()
      }
    }
  }

  "ApplicationController.deleteDatabaseUser" should {
    "return an Accepted Response" when {
      "The database user existed and has been deleted" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED

        val requestDelete: FakeRequest[AnyContentAsEmpty.type] = buildDelete(s"/api/delete/${testDataNotOnGitHub.userName}")
        val result = TestApplicationController.deleteDatabaseUser(testDataNotOnGitHub.userName)(requestDelete)

        status(result) shouldBe ACCEPTED
        afterEach()
      }
    }
    "return a error[###] status" when {
      // SCG - Left error - Have to mock to test the 500 database error for MongoDB
      "[304- Not modified]the user does not exist to delete" in {
        beforeEach()
        val requestDelete: FakeRequest[AnyContentAsEmpty.type] = buildDelete(s"/api/delete/${testDataNotOnGitHub.userName}")
        val result = TestApplicationController.deleteDatabaseUser(testDataNotOnGitHub.userName)(requestDelete)

        status(result) shouldBe NOT_MODIFIED
        afterEach()
      }
    }
  }

  "ApplicationController.updateDatabaseUser" should {
    "return a Accepted 202" when {
      "The database user exists and has been appropriately updated" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED
        val requestUpdate: FakeRequest[JsValue] = buildPatch(s"/api/updateUser/${testDataNotOnGitHub.userName}").withBody[JsValue](Json.toJson(testDataNotOnGitHubUpdate))
        val result = TestApplicationController.updateDatabaseUser(testDataNotOnGitHub.userName)(requestUpdate)
        println(result)
        status(result) shouldBe ACCEPTED
        afterEach()
      }
    }
    "return a error[###] status" when {
      // SCG - Left error - Have to mock to test the 500 database error for MongoDB
      "[404- Not found]the user does not exist to update" in {
        beforeEach()
        val requestUpdate: FakeRequest[JsValue] = buildPatch(s"/api/updateUser/${testDataNotOnGitHub.userName}").withBody[JsValue](Json.toJson(testDataNotOnGitHubUpdate))
        val result = TestApplicationController.updateDatabaseUser(testDataNotOnGitHub.userName)(requestUpdate)
        println(result)
        status(result) shouldBe NOT_FOUND
        afterEach()
      }

      "[400- Bad Request]the User exists but the request sent is not a valid DataModel for update" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testDataNotOnGitHub))
        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)

        status(createResult) shouldBe Status.CREATED
        val requestUpdate: FakeRequest[JsValue] = buildPatch(s"/api/updateUser/${testDataNotOnGitHub.userName}").withBody[JsValue](Json.toJson(invalidTestData))
        val result = TestApplicationController.updateDatabaseUser(testDataNotOnGitHub.userName)(requestUpdate)
        println(result)
        status(result) shouldBe BAD_REQUEST
        afterEach()
      }
    }
  }

  "ApplicationController.createDatabaseUserForm" should {
    "return a Accepted 202" when {
      "The user request is a completed valid model to create" in {
        beforeEach()

        val userFormTest = Map(
          "userName" -> "Hello",
          "dateAccount" -> "2023-04-07",
          "location" -> "location",
          "numberOfFollowers" -> "5",
          "numberFollowing" -> "10",
          "gitHubAccount" -> "true"
        )

        val request = FakeRequest(POST, "/addUser/form").withFormUrlEncodedBody(userFormTest.toSeq: _*)

        val createResult = TestApplicationController.createDatabaseUserForm()(request)

        status(createResult) shouldBe Status.CREATED
        afterEach()
      }
    }

    "returns the appropriate error [###]" when {
      "[400- BadRequest] the dataModel entered for a new user was sent but not correct" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/addUser/form").withBody[JsValue](Json.toJson(""))
        val createResult = TestApplicationController.createDatabaseUserForm()(request)

        status(createResult) shouldBe BAD_REQUEST
        afterEach()
//
      }
    }
  }
}
