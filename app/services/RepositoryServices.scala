package services


import cats.data.EitherT
import com.mongodb.client.result.DeleteResult
import models.{APIError, DataModel}
import org.mongodb.scala.result
import repository.{DataRepository, DataRepositoryTrait, LoginRepositoryTrait}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class RepositoryServices @Inject()(dataRepository: DataRepositoryTrait, loginRepo: LoginRepositoryTrait)(implicit ec: ExecutionContext) {

  def createUser(user: DataModel): Future[Either[APIError, DataModel]] = {
    dataRepository.createUser(user).map {
      case Right(createdUser) => Right(createdUser)
      case Left(error) => Left(error)
    }
  }

  def LogInUser(user: DataModel): Future[Either[APIError, DataModel]] = {
        loginRepo.logIn(user).map {
          case Right(user) => Right(user)
          case Left(loginError) => Left(loginError)
        }
  }



  def deleteDatabaseUser(userName: String): Future[Either[APIError, DeleteResult]] = {
    dataRepository.deleteUser(userName).map {
      case Right(deleteResult) if deleteResult.getDeletedCount > 0 => Right(deleteResult)
      case Right(deleteResult) if deleteResult.getDeletedCount == 0 => Left(APIError.NotModified(304, s"${userName} not found, cannot delete"))
      case Left(error) => Left(APIError.DatabaseError(error.httpResponseStatus, error.reason))
    }
  }


  def readUser(userName:String):Future[Either[APIError, DataModel]] ={
    dataRepository.findUserByName(userName).map{
      case Right(user) => Right(user)
      case Left(APIError.NotFoundError(code, message)) => Left(APIError.NotFoundError(code, message))
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
      // TODO - Added error case for database error
    }
  }


  def updateUser(userName: String, updatedUser: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    dataRepository.updateUser(userName, updatedUser).map {
      case Right(result) if(result.getModifiedCount == 1) =>  Right(result)
      case Right(result) if (result.getModifiedCount == 0) => Left(APIError.NotFoundError(404, s"User not found to update"))
      case Left(error) => Left(APIError.DatabaseError(error.httpResponseStatus, error.upstreamMessage))
    }
  }
  //TODO - SCG - Updated to factor in non acknowledged updates (this was handled i was being silly)
  // changed it to be username as the result is not the user




}
