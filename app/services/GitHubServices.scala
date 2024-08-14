package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.GitHubConnector
import models.{APIError, DataModel, PublicRepoDetails}
import play.api.libs.json.{JsArray, JsObject, JsValue, Reads}

import java.time.ZonedDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector:GitHubConnector)(repositoryServices: RepositoryServices){

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
          val user = DataModel(userName = userName, dateAccount = dateAccount, location = location,
            numberOfFollowers = numberOfFollowers, numberFollowing = numberFollowing, gitHubAccount = githubAccount)
          Right(user)

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }
  }

  def getGitHubRepo(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[PublicRepoDetails]] = {
    val url = s"https://api.github.com/users/$userName/repos"

    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {
      case arr: JsArray =>

        if (arr.value.isEmpty) {
          Left(APIError.NotFoundError(404, s"No repositories found for user $userName"))
        } else {

          val repos = arr.value.map { item =>
            val name = (item \ "name").as[String]
            val language = (item \ "language").asOpt[String]
            val pushedAt = (item \ "pushed_at").as[String]

            PublicRepoDetails(name, language, pushedAt)
          }.toSeq

          Right(repos)
        }

      case obj: JsObject =>
        if ((obj \ "status").asOpt[String].contains("404")) {
          Left(APIError.NotFoundError(404, "User not found in Github"))
        } else {
          Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
        }

      case _ =>
        Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
    }
  }
}
