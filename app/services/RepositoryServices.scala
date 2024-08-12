package services

import cats.data.EitherT
import com.mongodb.client.result.DeleteResult
import models.{APIError, DataModel}
import repository.DataRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepositoryServices @Inject() (dataRepository: DataRepository)(implicit ec: ExecutionContext) {

  def createUser(user:DataModel): Future[Either[ APIError, DataModel]]={
    dataRepository.createUser(user).map {
      case Right(createdUser) => Right(createdUser)
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
      case Left(APIError.BadAPIResponse(code, message)) => Left(APIError.BadAPIResponse(code, message))
    }
  }

  def deleteDatabaseUser(userName: String): Future[Either[APIError, DeleteResult]] = {
    dataRepository.deleteUser(userName).map {
      case Right(deleteResult) if deleteResult.getDeletedCount > 0 => Right(deleteResult)
      case Right(deleteResult) if deleteResult.getDeletedCount == 0 => Left(APIError.NotModified(304, s"${userName} not found, cannot delete"))
      case Left(error) => Left(APIError.DatabaseError(error.httpResponseStatus, error.reason))
    }
  }

}
