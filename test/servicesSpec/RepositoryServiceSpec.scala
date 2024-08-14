package servicesSpec


import baseSpec.BaseSpec
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import models.{APIError, DataModel}
import org.mongodb.scala.bson.{BsonString, BsonValue}
import org.mongodb.scala.result.UpdateResult
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results.Status
import repository.DataRepositoryTrait
import services.RepositoryServices


import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}


class RepositoryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {


  val mockRepository: DataRepositoryTrait = mock[DataRepositoryTrait]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new RepositoryServices(mockRepository)




  "RepositoryService.CreateUser()" should {
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
          .once()


        whenReady(testService.createUser(testDataModel)) { result =>


          result shouldBe Right(testDataModel)
        }
      }
    }


    "return a Left(APIError)" when {
      "The database was reached but the create was not made returning not acknowledged" in {
        (mockRepository.createUser _)
          .expects(testDataModel)
          .returning(Future.successful(Left(APIError.BadAPIResponse(500, s"Couldn't add $testDataModel to the database"))))


        whenReady(testService.createUser(testDataModel)) { result =>
          result shouldBe Left(APIError.BadAPIResponse(500, s"Couldn't add $testDataModel to the database"))
        }


      }


      "An error occurred whilst reaching the database" in {
        (mockRepository.createUser _)
          .expects(testDataModel)
          .returning(Future.successful(Left(APIError.DatabaseError(500, s"Failed to insert"))))
          .once()


        whenReady(testService.createUser(testDataModel)) { result =>
          result shouldBe Left(APIError.DatabaseError(500, s"Failed to insert"))
        }


      }
    }
  }


  "RepositoryService.deleteDatabaseUser()" should {
    val testDataModel = new DataModel(
      userName = "test",
      dateAccount = LocalDate.parse("2004-04-01"),
      location = "Here",
      numberOfFollowers = 10,
      numberFollowing = 10,
      gitHubAccount = false
    )


    "return a Right(deleteResult)" when {


      val mockSuccessDeleteResult: DeleteResult = DeleteResult.acknowledged(1)
      "the database successfully deleted a user" in {


        (mockRepository.deleteUser _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Right(mockSuccessDeleteResult)))
          .once()


        whenReady(testService.deleteDatabaseUser(testDataModel.userName)) { result =>


          result shouldBe Right(mockSuccessDeleteResult)
        }
      }
    }


    "return a Left(APIError)" when {
      val mockFailDeleteResult: DeleteResult = DeleteResult.acknowledged(0)
      "The database was reached but the delete was not acknowledged (entry doesn't exist)" in {
        (mockRepository.deleteUser _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Right(mockFailDeleteResult)))
          .once()


        whenReady(testService.deleteDatabaseUser(testDataModel.userName)) { result =>
          result shouldBe Left(APIError.NotModified(304, s"${testDataModel.userName} not found, cannot delete"))
        }


      }
      "An error was thrown whilst reaching the database" in {
        (mockRepository.deleteUser _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Left(APIError.DatabaseError(500, "An unexpected error happened: "))))
          .once()


        whenReady(testService.deleteDatabaseUser(testDataModel.userName)) { result =>


          result shouldBe Left(APIError.DatabaseError(500, "Bad response from upstream; got status: 500, and got reason An unexpected error happened: "))
        }
      }
    }
  }


  "RepositoryService.readUser()" should {
    val testDataModel = new DataModel(
      userName = "test",
      dateAccount = LocalDate.parse("2004-04-01"),
      location = "Here",
      numberOfFollowers = 10,
      numberFollowing = 10,
      gitHubAccount = false
    )


    "return a Right(user)" when {


      val mockSuccessDeleteResult: DeleteResult = DeleteResult.acknowledged(1)
      "the database successfully found a user" in {


        (mockRepository.findUserByName _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Right(testDataModel)))
          .once()


        whenReady(testService.readUser(testDataModel.userName)) { result =>


          result shouldBe Right(testDataModel)
        }
      }
    }


    "return a Left(APIError)" when {


      "The database was reached but the user does not exist" in {
        (mockRepository.findUserByName _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Left(APIError.NotFoundError(404, s"The ${testDataModel.userName} is not found in the database."))))
          .once()


        whenReady(testService.readUser(testDataModel.userName)) { result =>
          result shouldBe Left(APIError.NotFoundError(404, s"The ${testDataModel.userName} is not found in the database."))
        }


      }
      "An error was created whilst reaching the database" in {
        (mockRepository.findUserByName _)
          .expects(testDataModel.userName)
          .returning(Future.successful(Left(APIError.DatabaseError(500, "An unexpected error happened: "))))
          .once()


        whenReady(testService.readUser(testDataModel.userName)) { result =>


          result shouldBe Left(APIError.DatabaseError(500, "An unexpected error happened: "))
        }
      }
    }
  }


  "RepositoryService.updateUser()" should {
    val testDataModel = new DataModel(
      userName = "test",
      dateAccount = LocalDate.parse("2004-04-01"),
      location = "Here",
      numberOfFollowers = 10,
      numberFollowing = 10,
      gitHubAccount = false
    )


    "return a Right(updateResult)" when {
      val matchedCount = 1L        // Number of documents matched by the update query
      val modifiedCount = 1L       // Number of documents actually modified
      val upsertedId: BsonValue = BsonString("a")  // No upserted ID, as it's an optional parameter
      val mockSuccessUpdateResult: UpdateResult = UpdateResult.acknowledged(matchedCount, modifiedCount, upsertedId)


      "the database successfully found and update a user" in {


        (mockRepository.updateUser _)
          .expects(testDataModel.userName, testDataModel)
          .returning(Future.successful(Right(mockSuccessUpdateResult)))
          .once()


        whenReady(testService.updateUser(testDataModel.userName, testDataModel)) { result =>


          result shouldBe Right(mockSuccessUpdateResult)
        }
      }
    }


    "return a Left(APIError)" when {


      val mockFailUpdateResult: UpdateResult = UpdateResult.unacknowledged()
      "The database was reached but the user does not exist to update" in {
        (mockRepository.updateUser _)
          .expects(testDataModel.userName, testDataModel)
          .returning(Future.successful(Right(mockFailUpdateResult)))
          .once()


        whenReady(testService.updateUser(testDataModel.userName, testDataModel)) { result =>
          result shouldBe Left(APIError.NotFoundError(404, s"${testDataModel.userName} not found to update"))
        }


      }
      "An error was created whilst reaching the database" in {
        (mockRepository.updateUser _)
          .expects(testDataModel.userName, testDataModel)
          .returning(Future.successful(Left(APIError.BadAPIResponse(500, "An error occurred:"))))
          .once()


        whenReady(testService.updateUser(testDataModel.userName, testDataModel)) { result =>


          result shouldBe Left(APIError.DatabaseError(500, "An error occurred:"))
        }
      }
      // TODO - We could add a mock error val i.e. val error: String = "some message" to test the S interpolated strings
    }
  }
}
