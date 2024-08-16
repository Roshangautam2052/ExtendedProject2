package connector


import cats.data.EitherT
import models.APIError
import play.api.libs.json.Reads
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GitHubConnector @Inject()(ws: WSClient) {

  def get[Response](url: String)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
    val response = request.get()
    EitherT{
      response.map {
          result =>
            Right(result.json.as[Response])
        }
        .recover {
          case _ : WSResponse =>
            Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }

  }

  def delete[Response](url: String)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
    val response = request.get()
    EitherT{
      response.map {
          result =>
            Right(result.json.as[Response])
        }
        .recover {
          case _ : WSResponse =>
            Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }

  }
}