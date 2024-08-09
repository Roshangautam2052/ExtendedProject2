package services

import cats.data.EitherT
import com.google.inject.Singleton
import connector.connectors.GitHubConnector
import models.{APIError, DataModel}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubServices @Inject()(connector:GitHubConnector){
  def getGitHubUser(userName: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, DataModel] =
    connector.get[DataModel](s"https://api.github.com/users/$userName")

}
