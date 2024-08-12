package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.GitHubConnector
import models.{APIError, DataModel}
import play.api.libs.json.{JsObject, JsValue, Reads}

import java.time.ZonedDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector:GitHubConnector, repositoryServices: RepositoryServices){

  def getGitHubUser(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, DataModel] = {
   val url = s"https://api.github.com/users/$userName"


    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap { json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "User not found in Github"))

        case Some(item) =>
          val userName: String = (item \ "login").as[String]
          val dateAccount = ZonedDateTime.parse((item \ "created_at").as[String]).toLocalDate
          val location = (item \ "location").asOpt[String].getOrElse("")
          val numberOfFollowers = (item\ "followers").asOpt[Int].getOrElse(0)
          val numberFollowing = (item \ "following").asOpt[Int].getOrElse(0)
          val githubAccount = true

          Right(DataModel(userName = userName, dateAccount = dateAccount, location = location, numberOfFollowers = numberOfFollowers, numberFollowing = numberFollowing, gitHubAccount = githubAccount))

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }
  }
}
