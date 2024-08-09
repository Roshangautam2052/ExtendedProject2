package services

import repository.DataRepository

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class RepositoryServices @Inject() (dataRepository: DataRepository)(implicit ec: ExecutionContext) {

}
