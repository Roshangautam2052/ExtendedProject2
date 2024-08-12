package services

import cats.data.EitherT
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

}
