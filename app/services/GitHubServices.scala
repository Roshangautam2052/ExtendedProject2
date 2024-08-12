package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.connectors.GitHubConnector
import models.{APIError, DataModel}
import org.bson.json.JsonObject
import play.api.libs.json.{JsArray, JsObject, JsValue, Reads}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector:GitHubConnector)(repositoryServices: RepositoryServices){

  def getGitHubUser(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, DataModel] = {
   val url = s"https://api.github.com/users/$userName"
    connector.get[JsValue](url)(Reads.JsValueReads, ex).subflatMap { json =>
      json.asOpt[JsObject] match {
        case Some(item) =>
          val userName: String = (item \ "login").as[String]
          val dateAccount = ZonedDateTime.parse((item \ "created_at").as[String]).toLocalDate
          val location = (item \ "location").asOpt[String].getOrElse("")
          val numberOfFollowers = (item\ "followers").asOpt[Int].getOrElse(0)
          val numberFollowing = (item \ "following").asOpt[Int].getOrElse(0)
          val githubAccount = true

          Right(repositoryServices.createUser(Right(DataModel(userName = userName, dateAccount = dateAccount,
            location = location, numberOfFollowers = numberOfFollowers,
            numberFollowing = numberFollowing, gitHubAccount = githubAccount))))
        case None =>
          Left(APIError.NotFoundError(404, "User not found in database"))
      }
    }
  }
}
