package repository

import cats.data.EitherT
import models._
import org.mongodb.scala.model.{IndexModel, Indexes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

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

) {

  def createUser(user:DataModel):Future[Either[ APIError, DataModel]] ={
    val mappedUser= collection.insertOne(user).toFuture()
      mappedUser.map { result =>
        if(result.wasAcknowledged()) Right(user)
        else Left(APIError.BadAPIResponse(500, s"Couldn't add $user to the database"))
      }
        .recover {
          case exception: Throwable => Left(APIError.DatabaseError(500, s"Failed to insert book due to ${exception.getMessage}"))
        }
    }
}
