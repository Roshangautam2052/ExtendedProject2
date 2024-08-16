package connector


import AuthToken.AuthToken.authToken
import cats.data.EitherT
import models.APIError
import play.api.libs.json.{JsObject, JsValue, Reads}
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

  def delete[Response](url: String, body: JsValue)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
      .withHttpHeaders(
      "Content-Type" -> "application/json", // Set the content type to JSON
      "Authorization" -> s"Bearer $authToken" // Add the Authorization header with the token
    )

    // Content type is set to Json
    val response = request
    .withMethod("POST")
    .withBody(body)
    .execute()

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

  def create[Response](url: String, body: JsValue)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
      .withHttpHeaders(
        "Content-Type" -> "application/json",  // Set the content type to JSON
        "Authorization" -> s"Bearer $authToken" // Add the Authorization header with the token
      )

    // Send a POST request with the JSON body
    val response = request
      .post(body)

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