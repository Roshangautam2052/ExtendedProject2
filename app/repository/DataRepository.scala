package repository

import cats.data.{EitherT, NonEmptySet}
import com.google.inject.ImplementedBy
import models._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model.{Filters, IndexModel, Indexes, ReplaceOptions}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[DataRepository])
trait DataRepositoryTrait {
  def createUser(user:DataModel): Future[Either[APIError, DataModel]]
  def deleteUser(userName: String): Future[Either[APIError, DeleteResult]]
  def findUserByName(userName:String): Future[Either[APIError, DataModel]]
  def updateUser(userName: String, updatedUser: DataModel): Future[Either[APIError.BadAPIResponse, UpdateResult]]
  def deleteAll(): Future[Unit]
}

@Singleton
class DataRepository @Inject()(
                                mongoComponent: MongoComponent
                              )(implicit ec: ExecutionContext) extends PlayMongoRepository[DataModel](
  collectionName = "dataModels",
  mongoComponent = mongoComponent,
  domainFormat = DataModel.formats,
  indexes = Seq(IndexModel(
    Indexes.ascending("_id")
  )),
  replaceIndexes = false

) with DataRepositoryTrait {

  override def createUser(user:DataModel):Future[Either[ APIError, DataModel]] ={
    val mappedUser= collection.insertOne(user).toFuture()
      mappedUser.map { result =>
        if(result.wasAcknowledged()) Right(user)
        else Left(APIError.BadAPIResponse(500, s"Couldn't add $user to the database"))
      }
        .recover {
          case exception: Throwable => Left(APIError.DatabaseError(500, s"Failed to insert user due to ${exception.getMessage}"))
        }
    }

  private def byName(userName: String): Bson = {
    Filters.and(
      Filters.equal("userName", userName)
    )
  }

  override def deleteUser(userName: String): Future[Either[APIError, DeleteResult]] = {
    collection.deleteOne(filter = byName(userName)).toFuture().map {
      deleteResult =>
        Right(deleteResult)
    }
      .recover{
        case NonFatal(e) => Left(APIError.DatabaseError(500, s"An unexpected error happened: ${e.getMessage}"))
      }
  }

  override def findUserByName(userName:String): Future[Either[APIError, DataModel]] = {
    collection.find(byName(userName)).toFuture().map { result =>
     result.headOption match {
       case Some(user) => Right(user)
       case None => Left(APIError.NotFoundError(404, s"The $userName is not found in the database."))
     }
    }
  }

  override def updateUser(userName: String, updatedUser: DataModel): Future[Either[APIError.BadAPIResponse, UpdateResult]] =
    collection.replaceOne(
      filter = byName(userName),
      replacement = updatedUser,
      options = new ReplaceOptions().upsert(false)
    ).toFuture().map(Right(_)).recover {
      case ex: Exception => Left(APIError.BadAPIResponse(500, s"An error occurred: ${ex.getMessage}"))
    }


  override def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ())

}
