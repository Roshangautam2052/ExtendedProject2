package servicesSpec

import baseSpec.BaseSpec
import models.DataModel
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import repository.DataRepositoryTrait
import services.RepositoryServices

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  val mockRepository: DataRepositoryTrait = mock[DataRepositoryTrait]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new RepositoryServices(mockRepository)



  "RespositoryService.CreateUser()" should {
    val testDataModel = new DataModel(
      userName = "test",
      dateAccount = LocalDate.parse("2004-04-01"),
      location = "Here",
      numberOfFollowers = 10,
      numberFollowing = 10,
      gitHubAccount = false
    )
    "return a Right(createdUser)" when {

      "the database successfully created a user" in {

        (mockRepository.createUser _)
          .expects(testDataModel)
          .returning(Future.successful(Right(testDataModel)))

        whenReady(testService.createUser(testDataModel)){ result =>

          println(result)
          result shouldBe Right(testDataModel)

        }
      }
    }
  }
}
