package connector

package connectors


import cats.data.EitherT
import models.APIError

import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess, OFormat, Reads}
import play.api.libs.ws.{WSClient, WSResponse}

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
}