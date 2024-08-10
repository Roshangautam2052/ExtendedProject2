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
class GitHubServices @Inject()(connector:GitHubConnector){

  def getGitHubUser(userName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, DataModel] = {
   val url = s"https://api.github.com/users/$userName"

    connector.get[JsValue](url)(Reads.JsValueReads, ex).subflatMap { json =>
      println("Here")

      json.asOpt[JsObject] match {
        case Some(item) =>
          val userName: String = (item \ "login").as[String]
          val dateAccount = ZonedDateTime.parse((item \ "created_at").as[String]).toLocalDate
          val location = (item \ "location").asOpt[String].getOrElse("")
          val numberOfFollowers = (item\ "followers").asOpt[Int].getOrElse(0)
          val numberFollowing = (item \ "following").asOpt[Int].getOrElse(0)
          val githubAccount = true

          Right(DataModel(userName = userName, dateAccount = dateAccount, location = location, numberOfFollowers = numberOfFollowers, numberFollowing = numberFollowing, gitHubAccount = githubAccount))
        case None =>
          Left(APIError.NotFoundError(404, "User not found in database"))
      }
    }
  }
}
//{
//  "login": "SpencerCGriffiths",
//  "id": 130156132,
//  "node_id": "U_kgDOB8IGZA",
//  "avatar_url": "https://avatars.githubusercontent.com/u/130156132?v=4",
//  "gravatar_id": "",
//  "url": "https://api.github.com/users/SpencerCGriffiths",
//  "html_url": "https://github.com/SpencerCGriffiths",
//  "followers_url": "https://api.github.com/users/SpencerCGriffiths/followers",
//  "following_url": "https://api.github.com/users/SpencerCGriffiths/following{/other_user}",
//  "gists_url": "https://api.github.com/users/SpencerCGriffiths/gists{/gist_id}",
//  "starred_url": "https://api.github.com/users/SpencerCGriffiths/starred{/owner}{/repo}",
//  "subscriptions_url": "https://api.github.com/users/SpencerCGriffiths/subscriptions",
//  "organizations_url": "https://api.github.com/users/SpencerCGriffiths/orgs",
//  "repos_url": "https://api.github.com/users/SpencerCGriffiths/repos",
//  "events_url": "https://api.github.com/users/SpencerCGriffiths/events{/privacy}",
//  "received_events_url": "https://api.github.com/users/SpencerCGriffiths/received_events",
//  "type": "User",
//  "site_admin": false,
//  "name": "Spencer Clarke-Griffiths ",
//  "company": null,
//  "blog": "",
//  "location": null,
//  "email": null,
//  "hireable": null,
//  "bio": null,
//  "twitter_username": null,
//  "public_repos": 14,
//  "public_gists": 0,
//  "followers": 2,
//  "following": 2,
//  "created_at": "2023-04-07T12:50:03Z",
//  "updated_at": "2024-07-02T09:55:22Z"
//}