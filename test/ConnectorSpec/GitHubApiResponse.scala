package ConnectorSpec

import play.api.libs.json.{Json, OFormat}


case class GitHubApiResponse(
                              message: String,
                              success: Boolean
                            )

object GitHubApiResponse {
  implicit val format: OFormat[GitHubApiResponse] = Json.format[GitHubApiResponse]
}