package controllers

import baseSpec.BaseSpecWithApplication
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())

  "Something" should {
    "Do Something" when {
      "Here" in {

      }
    }
  }
}
