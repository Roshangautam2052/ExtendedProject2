package controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, CREATED, OK}
import play.api.mvc.ControllerHelpers.Continue.withHeaders
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{DELETE, GET, contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import services.GitHubServices

import scala.concurrent.{ExecutionContext, Future}

class GitHubRepoControllerSpec extends AnyWordSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite with Matchers {
  val mockGitHubServices: GitHubServices = mock[GitHubServices]
  val stubbedControllerComponent: ControllerComponents = stubControllerComponents()
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testGitHubRepoController: GitHubRepoController = new GitHubRepoController(stubbedControllerComponent, mockGitHubServices)

  "GitHubRepoController.displayDeleteForm" should {
      "GET request is sent in the URI GitHub/deleteRepos/showForm/$userName/$repoName/$sha/$path/$fileName" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val sha = "testSha"
        val path = "testPath"
        val fileName = "testFileName"
        val validDeleteForm = Map("message" -> "", "sha" -> "nonEmptySha")
        val request = FakeRequest(GET, s"/GitHub/deleteRepos/showForm/$userName/$repoName/$sha/$path/$fileName ")
          .withFormUrlEncodedBody(validDeleteForm.toSeq: _*)
        val result: Future[Result] = testGitHubRepoController.displayDeleteForm(userName, repoName, sha, path, fileName)(request)
        val content = contentAsString(result)
        status(result) shouldBe OK
        content should include(s"<form action =/GitHub/repos/:$userName/$repoName/contents/$path/$fileName")
        content should include(s"Name: $userName")
        content should include(s"sha: $sha")
        content should include(s"Repository: $repoName")
        content should include(s"Path: $path")
        content should include(s"File Name: $fileName")
      }
    }
  "GitHubRepoController.deleteDirectoryOrFile" should {
      " return 400 there is error in the Form " in {
        val invalidFormData = Map("message" -> "", "sha" -> "nonEmptySha")
        val userName = "testUser"
        val repoName = "testRepo"
        val path = "testPath"
        val fileName = "testFileName"
        val request = FakeRequest(DELETE, s"/GitHub/repos/$userName/$repoName/$path/$fileName")
          .withFormUrlEncodedBody(invalidFormData.toSeq: _*)
        val result = testGitHubRepoController.deleteDirectoryOrFile(userName, repoName, path, fileName)(request)
        status(result) shouldBe  BAD_REQUEST
        contentAsString(result) should include("Data not avail:")

      }
    " return 200 there is no error in the Form " in {
      val validDeleteForm = Map("message" -> "Hello", "sha" -> "nonEmptySha")
      val userName = "testUser"
      val repoName = "testRepo"
      val path = "testPath"
      val fileName = "testFileName"
      val request = FakeRequest(DELETE, s"/GitHub/repos/$userName/$repoName/$path/$fileName")
        .withFormUrlEncodedBody(validDeleteForm.toSeq: _*)
      val result = testGitHubRepoController.deleteDirectoryOrFile(userName, repoName, path, fileName)(request)
      status(result) shouldBe CREATED
    }
    }
}
