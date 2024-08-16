package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.GitHubConnector
import models.{APIError, DataModel, DeleteModel, FileContent, PublicRepoDetails, TopLevelModel}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json, Reads}

import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.Inject
import javax.print.attribute.standard.RequestingUserName
import scala.Right
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector: GitHubConnector)(repositoryServices: RepositoryServices) {

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
          val numberOfFollowers = (item \ "followers").asOpt[Int].getOrElse(0)
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
            val userName = (item \"owner" \ "login").as[String]
            val name = (item \ "name").as[String]
            val language = (item \ "language").asOpt[String]
            val pushedAt = (item \ "pushed_at").as[String]

            PublicRepoDetails(userName, name, language, pushedAt)
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

  def getGitDirsAndFiles(userName: String, repoName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[TopLevelModel]] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents"


    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {
      case arr: JsArray =>

        if (arr.value.isEmpty) {
          val empty: Seq[TopLevelModel] = Seq()
          Right(empty)
        } else {

          val contents = arr.value.map { item =>
            val name = (item \ "name").as[String]
            val format = (item \ "type").as[String]
            val path = (item \ "path").as[String]
            val sha = (item \ "sha"). as[String]

          TopLevelModel(name,sha, format, path)
        }.toSeq
        Right(contents)
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


  def openGitDir(userName: String, repoName: String, path: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[TopLevelModel]] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"

    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {
      case arr: JsArray =>

        if (arr.value.isEmpty) {
          Left(APIError.NotFoundError(404, s"This $repoName is empty"))
        } else {

          val contents = arr.value.map { item =>
            val name = (item \ "name").as[String]
            val format = (item \ "type").as[String]
            val path = (item \ "path").as[String]
            val sha = (item \ "sha"). as[String]

          TopLevelModel(name,sha, format, path)
        }.toSeq
        Right(contents)
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

  def getGitRepoFileContent(userName: String, repoName: String, path:String)(implicit ex: ExecutionContext): EitherT[Future, APIError, String] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "User not found in Github"))

        case Some(item) =>
          val file: String = (item \ "content").as[String]
          val clean64 = file.replaceAll("\\s","")
        val decodedFile = Base64.getDecoder.decode(clean64)
          val textDecoded = new String(decodedFile, "UTF-8")
          Right(textDecoded)

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }


  }

  def deleteDirectoryOrFile(userName: String, repo: String, path: String, formData: DeleteModel)(implicit ex: ExecutionContext): EitherT[Future, APIError, String] ={
    val url = s"https://api.github.com/repos/$userName/$repo/contents/$path"

    val body = Json.obj(
      "message" -> formData.message,
      "sha" -> formData.sha
    )

    connector.delete[JsValue](url, body)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "User not found in Github"))

        case Some(item) =>
          Right(item.toString())

          /** "{\"content\":null,\"commit\":{\"sha\":\"1841ae232f55cf092b06a18cb404e40aa26f0152\",\"node_id\":\"C_kwDOMkQgjtoAKDE4NDFhZTIzMmY1NWNmMDkyYjA2YTE4Y2I0MDRlNDBhYTI2ZjAxNTI\",\"url\":\"https://api.github.com/repos/GarysRobot/TestRepo/git/commits/1841ae232f55cf092b06a18cb404e40aa26f0152\",\"html_url\":\"https://github.com/GarysRobot/TestRepo/commit/1841ae232f55cf092b06a18cb404e40aa26f0152\",\"author\":{\"name\":\"GarysRobot\",\"email\":\"spennder@gmail.com\",\"date\":\"2024-08-16T13:25:38Z\"},\"committer\":{\"name\":\"GarysRobot\",\"email\":\"spennder@gmail.com\",\"date\":\"2024-08-16T13:25:38Z\"},\"tree\":{\"sha\":\"4b825dc642cb6eb9a060e54bf8d69288fbee4904\",\"url\":\"https://api.github.com/repos/GarysRobot/TestRepo/git/trees/4b825dc642cb6eb9a060e54bf8d69288fbee4904\"},\"message\":\"Delete\",\"parents\":[{\"sha\":\"fbbb6d162baebcea95dfb2cec52d74efe2b8cf6c\",\"url\":\"https://api.github.com/repos/GarysRobot/TestRepo/git/commits/fbbb6d162baebcea95dfb2cec52d74efe2b8cf6c\",\"html_url\":\"https://github.com/GarysRobot/TestRepo/commit/fbbb6d162baebcea95dfb2cec52d74efe2b8cf6c\"}],\"verification\":{\"verified\":false,\"reason\":\"unsigned\",\"signature\":null,\"payload\":null}}}" */
        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }
  }



}
