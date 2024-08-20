//package controllers
//
//import baseSpec.BaseSpecWithApplication
//import models.DataModel
//import play.api.http.Status
//import play.api.http.Status.{NOT_FOUND, OK}
//import play.api.libs.json.{JsSuccess, JsValue, Json}
//import play.api.mvc.{AnyContentAsEmpty, Result}
//import play.api.mvc.Results.NotFound
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status}
//
//import java.time.LocalDate
//import scala.concurrent.Future
//
//class ApplicationControllerSpec extends BaseSpecWithApplication {
//
//  override def beforeEach(): Unit = await(repository.deleteAll())
//  override def afterEach(): Unit = await(repository.deleteAll())
//
//  val TestApplicationController = new ApplicationController(controllerComponents = component,
//    gitService = gitService, repoService = repoService)
//
//  val testData: DataModel = DataModel(
//    userName = "SpencerCGriffiths",
//    dateAccount = LocalDate.parse("2023-04-07"),
//    location = "",
//    numberOfFollowers = 2,
//    numberFollowing = 2,
//    gitHubAccount = true
//  )
//
//  private val invalidTestData: String = "invalid Json DataModel"
//
//  val invalidUser:String = "hell124124"
//
//
//  "ApplicationController.getGitHubUser" should {
//
//    "return a user" when {
//
//      "the user exits in GitHub" in {
//        beforeEach()
//        val request: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/githubDemo/${testData.userName}")
//        val result = TestApplicationController.getGitHubUser(testData.userName)(request)
//        val content = contentAsJson(result)
//        println(content)
//        status(result) shouldBe OK
//        content shouldBe (Json.toJson(testData))
//        afterEach()
//      }
//    }
//
//    "return a 404 error " when {
//      "the user does not exits in GitHub" in {
//        beforeEach()
//        val request:FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/githubDemo/$invalidUser")
//        val result = TestApplicationController.getGitHubUser(invalidUser)(request)
//        status(result) shouldBe NOT_FOUND
//
//      }
//    }
//  }
//
//  "ApplicationController.readDataBaseUser" should {
//
//    "return a user from the database" when {
//      "given a valid username and user exists" in {
//        beforeEach()
//        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testData))
//        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)
//
//        status(createResult) shouldBe Status.CREATED
//
//        val result = TestApplicationController.readDataBaseUser(testData.userName)(FakeRequest())
//
//        status(result) shouldBe OK
//        contentAsJson(result).as[DataModel] shouldBe testData
//        afterEach()
//      }
//    }
//
//    "return a 404 error from database" when {
//      "when the username does not exist in the database" in {
//        beforeEach()
//        val result = TestApplicationController.readDataBaseUser(testData.userName)(FakeRequest())
//
//        status(result) shouldBe NOT_FOUND
//        afterEach()
//      }
//    }
//  }
//
//  "ApplicationController.createDataBaseUser" should {
//
//    "return a Created 201 status and the created user" when {
//      "a valid Json is sent for creation" in {
//        beforeEach()
//        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testData))
//        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)
//
//        status(createResult) shouldBe Status.CREATED
//        contentAsJson(createResult).as[DataModel] shouldBe testData
//
//        afterEach()
//      }
//    }
//
//    "return a Bad Request 400 Status and error message" when {
//      "invalid Json that cannot be converted to a DataModel is sent" in {
//        beforeEach()
//        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(invalidTestData))
//        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)
//
//        status(createResult) shouldBe Status.BAD_REQUEST
//        contentAsJson(createResult) shouldBe Json.toJson(s"Invalid Body: \"invalid Json DataModel\"")
//
//        afterEach()
//
//      }
//    }
//  }
//
//  "ApplicationController.readDatabaseOrAddFromGithub" should {
//    "read the database and return 200 Ok and the user" when {
//      "the user exists in the database" in {
//        beforeEach()
//        val request: FakeRequest[JsValue] = buildPost("/api/create/").withBody[JsValue](Json.toJson(testData))
//        val createResult: Future[Result] = TestApplicationController.createDatabaseUser()(request)
//
//        status(createResult) shouldBe Status.CREATED
//
//        val readRequest: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/api/${testData.userName}")
//        val result = TestApplicationController.readDatabaseOrAddFromGithub(testData.userName)(readRequest)
//
//        status(result) shouldBe OK
//        contentAsJson(result).as[DataModel] shouldBe testData
//        afterEach()
//      }
//    }
//    "read the user from github and return 200 Ok and the user" when {
//      "the user does not exist in the database but does in github" in {
//        beforeEach()
//
//        val request: FakeRequest[AnyContentAsEmpty.type] = buildGet(s"/api/${testData.userName}")
//        val result = TestApplicationController.readDatabaseOrAddFromGithub(testData.userName)(request)
//        val content = contentAsJson(result)
////        println(content)
//        status(result) shouldBe OK
////        content shouldBe (Json.toJson(testData))
//        afterEach()
//
//      }
//    }
//  }
//
//  "ApplicationController" should {
//    "a" when {
//      "b" in {
//        beforeEach()
//        afterEach()
//      }
//    }
//    "c" when {
//      "d" in {
//        beforeEach()
//        afterEach()
//
//      }
//    }
//  }
//}
