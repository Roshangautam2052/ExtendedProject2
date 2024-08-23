package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.GitHubConnector
import models._
import play.api.libs.json.JsNull.asOpt
import play.api.libs.json._

import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector: GitHubConnector) extends GitHubServiceTrait {

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
            val userName = (item \ "owner" \ "login").as[String]
            val name = (item \ "name").as[String]
            val language = (item \ "language").asOpt[String]
            val pushedAt = (item \ "pushed_at").as[String]

            PublicRepoDetails(userName, name, language, pushedAt)
          }.toSeq
          Right(repos)
        }

      case obj: JsObject =>
        if ((obj \ "status").asOpt[String].contains("404")) {
          Left(APIError.NotFoundError(404, "Repository not found in Github"))
        } else {
          Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
        }

      case _ =>
        Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
    }
  }

  def getGitDirsAndFiles(userName: String, repoName: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[FilesAndDirsModel]] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents"


    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {
      case arr: JsArray =>

        if (arr.value.isEmpty) {
          val empty: Seq[FilesAndDirsModel] = Seq()
          Right(empty)
        } else {

          val contents = arr.value.map { item =>
            val name = (item \ "name").as[String]
            val format = (item \ "type").as[String]
            val path = (item \ "path").as[String]
            val sha = (item \ "sha").as[String]

            FilesAndDirsModel(name, sha, format, path)
          }.toSeq
          Right(contents)
        }


      case obj: JsObject =>

        if ((obj \ "status").asOpt[String].contains("404") && (obj \ "message").asOpt[String].contains("This repository is empty")) {
          Right(Seq())
        } else if ((obj \ "status").asOpt[String].contains("404")) {
          Left(APIError.NotFoundError(404, "User or Repository Not found"))
        }
        else {
          Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
        }

      case _ =>
        Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
    }
  }

  def openGitDir(userName: String, repoName: String, path: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, Seq[FilesAndDirsModel]] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"

    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap {
      case arr: JsArray =>

        if (arr.value.isEmpty) {
          val empty: Seq[FilesAndDirsModel] = Seq()
          Right(empty)
        } else {
          val contents = arr.value.map { item =>
            val name = (item \ "name").as[String]
            val format = (item \ "type").as[String]
            val path = (item \ "path").as[String]
            val sha = (item \ "sha").as[String]

            FilesAndDirsModel(name, sha, format, path)
          }.toSeq
          Right(contents)
        }


      case obj: JsObject =>
        if ((obj \ "status").asOpt[String].contains("404")) {
          Left(APIError.NotFoundError(404, "Directory not found in Github"))
        } else {
          Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
        }

      case _ =>
        Left(APIError.BadAPIResponse(500, "Unexpected JSON format"))
    }
  }

  def getGitRepoFileContent(userName: String, repoName: String, path: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, FileContent] = {
    val url = s"https://api.github.com/repos/$userName/$repoName/contents/$path"
    connector.get[JsValue](url)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap { json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "Directory not found in Github"))

        case Some(item) =>
          val file: String = (item \ "content").as[String]
          val clean64 = file.replaceAll("\\s", "")
          val path = (item \ "path").as[String]
          val sha = (item \ "sha").as[String]
          val decodedFile = Base64.getDecoder.decode(clean64)
          val textDecoded = new String(decodedFile, "UTF-8")
          Right(FileContent(textDecoded, sha, path))

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }


  }

  def deleteDirectoryOrFile(userName: String, repo: String, path: String, formData: DeleteModel)(implicit ex: ExecutionContext): EitherT[Future, APIError, String] = {
    val url = s"https://api.github.com/repos/$userName/$repo/contents/$path"

    val body = Json.obj(
      "message" -> formData.message,
      "sha" -> formData.sha
    )

    connector.delete[JsValue](url, body)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap { json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>

          Left(APIError.NotFoundError(404, "File, user or repo does not exist to delete"))

        case Some(item) if (item \ "status").asOpt[String].contains("409") =>
          val message = (item \ "message").as[String]
          Left(APIError.NotModified(409, message))

        case Some(item) if (item \ "status").asOpt[String].contains("422") =>
          val message = (item \ "message").as[String]
          Left(APIError.NotModified(422, message))


        case Some(item) =>
          // could check for message = "Delete" for validation
          Right(item.toString())

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }
  }

  def createFile(userName: String, repo: String, fileName: String, formData: CreateFileModel, path: Option[String])(implicit ex: ExecutionContext): EitherT[Future, APIError, String] = {
    val url = path match {
      case Some(path) => s"https://api.github.com/repos/$userName/$repo/contents/$path/$fileName"
      case None => s"https://api.github.com/repos/$userName/$repo/contents/$fileName"
    }

    val encodedFormContent = Base64.getEncoder.encodeToString(formData.content.getBytes)
    val body = Json.obj(
      "message" -> formData.message,
      "content" -> encodedFormContent,
      "fileName" -> formData.fileName
    )

    connector.create[JsValue](url, body)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap { json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "Directory not found in Github"))

        case Some(item) =>
          // could check message = create for validation
          Right(item.toString())

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with Github Response Data"))
      }
    }
  }

  def editContent(userName: String, repoName: String, path: String, formData: UpdateFileModel)(implicit ex: ExecutionContext): EitherT[Future, APIError, String] = {


    val url = s"https://api.github.com/repos/$userName/$repoName/contents/${formData.path}"

    val encodedFormContent = Base64.getEncoder.encodeToString(formData.content.getBytes)

    val body = Json.obj(
      "message" -> formData.message,
      "content" -> encodedFormContent,
      "sha" -> formData.sha
    )

    connector.create[JsValue](url, body)(Reads.JsValueReads, ex).leftMap {
      case APIError.BadAPIResponse(code, msg) => APIError.BadAPIResponse(code, msg)
    }.subflatMap { json =>
      json.asOpt[JsObject] match {

        case Some(item) if (item \ "status").asOpt[String].contains("404") =>
          Left(APIError.NotFoundError(404, "File, user or repo does not exist to delete"))

        case Some(item) if (item \ "status").asOpt[String].contains("409") =>
          val message = (item \ "message").as[String]
          Left(APIError.NotModified(409, message))

        case Some(item) if (item \ "status").asOpt[String].contains("422") =>
          val message = (item \ "message").as[String]
          Left(APIError.NotModified(422, message))


        case Some(item) =>
          val result: EitherT[Future, APIError, String] = if (path != formData.path) {
            val deleteModel = DeleteModel(message = s"Delete Duplication ${formData.message}", sha = formData.sha)

            val response = deleteDirectoryOrFile(userName, repoName, path, deleteModel).value.flatMap {
              case Right(_) =>
                Future.successful(Right(item.toString()))
              case Left(error) =>
                Future.successful(Left(APIError.BadAPIResponse(500, "File Duplicated, error with duplicate file update")))
            }

            EitherT(response)
          } else {
            EitherT.rightT(item.toString()) // Paths are equal, return the item without attempting to delete
          }

          Await.result(result.value, 10.seconds)

        case None =>
          Left(APIError.BadAPIResponse(500, "Error with GitHub Response Data"))

      }
    }
  }
}
