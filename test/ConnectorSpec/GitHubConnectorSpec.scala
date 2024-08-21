package ConnectorSpec

import baseSpec.BaseSpecWithApplication
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, status, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import connector.GitHubConnector
import models.{APIError, CreateFileModel, DeleteModel, UpdateFileModel}
import org.scalamock.clazz.MockImpl.mock
import org.scalamock.scalatest.MockFactory
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.{Await, Future}

class GitHubConnectorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with BaseSpecWithApplication with MockFactory {


  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080))
  // Starts a WireMock Server that will run locally on port 8080.
  // This will mean request can be made to port 8080 and will be intercepted

  override def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor("localhost", 8080)
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    ws.close()
    // Here we use WS Which is imported with BaseSpecWithApplication
  }

  val connector = new GitHubConnector(ws)

  "GitHubConnector.get when retrieving a user (getGitHubUser)" should {

    "return a Json Response [•••]" when {

      "[UserData] called with a existing userName to the API" in {
        // Set up WireMock to return a 200 response with specific body
        val userName = "testuser"
        val jsonResponse = Json.obj(
          "login" -> userName,
          "created_at" -> "2024-08-16T09:10:38Z",
          "location" -> "Somewhere",
          "followers" -> 42,
          "following" -> 10
        )

        stubFor(WireMock.get(urlEqualTo(s"/users/$userName"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$userName").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 Not Found response when the userName is not in GitHub" in {
        val notAName = "testuser"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )


        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/users/$notAName"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$notAName").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }
  }


  "GitHubConnector.get when retrieving user repositories (getGitHubRepo)" should {

    "return a Json Response [•••] of user Repositories" when {

      "[RepoData] called with an existing userName to the API" in {
        // Set up WireMock to return a 200 response with specific body
        val userName = "testuser"
        val jsonResponse = Json.arr(
          Json.obj(
            "name" -> "Repo1",
            "owner" -> Json.obj("login" -> userName),
            "language" -> "Scala",
            "pushed_at" -> "2024-08-16T09:10:38Z"
          ),
          Json.obj(
            "name" -> "Repo2",
            "owner" -> Json.obj("login" -> userName),
            "language" -> "Java",
            "pushed_at" -> "2024-07-16T09:10:38Z"
          )
        )

        stubFor(WireMock.get(urlEqualTo(s"/users/$userName/repos"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$userName/repos").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return an empty array when the userName has no repositories in GitHub" in {
        val userName = "testuser"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.arr(
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/users/$userName/repos"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$userName/repos").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 Not Found response when the userName is not in GitHub to retrieve the Repos" in {
        val notAName = "testuser"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/users/$notAName/repos"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$notAName/repos").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }


  }

  "GitHubConnector.get when retrieving directories and files (getGitDirsAndFiles)" should {

    "return a Json Response [•••] of user Dirs and Files" when {

      "[Dir/File] called with an existing userName and repoName to the API and there are dirs and files" in {
        // Set up WireMock to return a 200 response with specific body
        val userName = "testUser"
        val repoName = "testRepo"
        val jsonResponse = Json.arr(
          Json.obj(
            "name" -> "File1",
            "type" -> "file",
            "path" -> "src/main/File1.scala",
            "sha" -> "abc123"
          ),
          Json.obj(
            "name" -> "File2",
            "type" -> "file",
            "path" -> "src/main/File2.scala",
            "sha" -> "def456"
          ),
          Json.obj(
            "name" -> "Directory1",
            "type" -> "dir",
            "path" -> "src/main/Directory1",
            "sha" -> "ghi789"
          )
        )

        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 repository is empty response when the userName's repository is empty in GitHub" in {
        val userName = "testUser"
        val repoName = "testRepo"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "This repository is empty",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 when the userName is incorrect but the repoName is correct" in {
        val notAUser = "notATestUser"
        val repoName = "testRepo"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$notAUser/$repoName/contents"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$notAUser/$repoName/contents").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
      "return a 404 when the repoName is incorrect but the userName is correct" in {
        val userName = "testUser"
        val repoName = "testRepo"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }
  }

  "GitHubConnector.get when retrieving directories and files inside a directory (openGitDir)" should {

    "return a Json Response [•••] of user Dirs and Files" when {

      "[Dir/File] called with an existing userName and repoName and path to the API and there are dirs and files" in {
        // Set up WireMock to return a 200 response with specific body
        val userName = "testUser"
        val repoName = "testRepo"
        val dirPath = "somePath"
        val jsonResponse = Json.arr(
          Json.obj(
            "name" -> "File1",
            "type" -> "file",
            "path" -> "src/main/File1.scala",
            "sha" -> "abc123"
          ),
          Json.obj(
            "name" -> "File2",
            "type" -> "file",
            "path" -> "src/main/File2.scala",
            "sha" -> "def456"
          ),
          Json.obj(
            "name" -> "Directory1",
            "type" -> "dir",
            "path" -> "src/main/Directory1",
            "sha" -> "ghi789"
          )
        )

        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents/$dirPath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$dirPath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 not found if the folder is empty (does not exist if empty) when the userName & repository are correct" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val dirPath = "somePath"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents/$dirPath"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$dirPath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "return a 404 when the userName is incorrect but the repoName is correct and a correct path provided" in {
        val notAUser = "notATestUser"
        val repoName = "testRepo"
        val dirPath = "somePath"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$notAUser/$repoName/contents/$dirPath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$notAUser/$repoName/contents/$dirPath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
      "return a 404 when the repoName is incorrect but the userName is correct and a correct path provided" in {
        val userName = "testUser"
        val notARepoName = "testRepoWrong"
        val dirPath = "somePath"
        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$notARepoName/contents/$dirPath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$notARepoName/contents/$dirPath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }
  }

  "GitHubConnector.get when retrieving file content (getGitRepoFileContent)" should {

    "return a Json Response [•••] of file content" when {

      "called with an existing userName and repoName and path to the API and file has content" in {
        // Set up WireMock to return a 200 response with specific body
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val jsonResponse = Json.obj(
          "path" -> ".gitignore",
          "sha" -> "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c",
          "content" -> "bm9kZV9tb2R1bGVzCi5lbnYuKg==\n",
        )


        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)

      }

      "called with an existing userName and repoName and path to the API and file has no content" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val jsonResponse = Json.obj(
          "path" -> ".gitignore",
          "sha" -> "Cg==\n",
          "content" -> "bm9kZV9tb2R1bGVzCi5lbnYuKg==\n",
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "called with a non existent userName, existing repoName and existing file path to the API " in {
        val notAUser = "notATestUser"
        val repoName = "testRepo"
        val filePath = "somePath"

        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$notAUser/$repoName/contents/$filePath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$notAUser/$repoName/contents/$filePath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
      "called with a  existing userName, non existent repoName and existing file path to the API " in {
        val userName = "testUser"
        val notARepoName = "testRepoWrong"
        val filePath = "somePath"
        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$notARepoName/contents/$filePath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$notARepoName/contents/$filePath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
      "called with a existing userName, existing repoName and non existent file path to the API " in {
        val userName = "testUser"
        val repoName = "testRepo"
        val fileWrongPath = "somePath"
        // Create a JSON response for "Not Found"
        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest",
          "status" -> "404"
        )

        // Stub the WireMock to return this JSON when the userName is not found
        stubFor(WireMock.get(urlEqualTo(s"/repos/$userName/$repoName/contents/$fileWrongPath"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.get[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$fileWrongPath").value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }
  }

  "GitHubConnector.delete when retrieving file content (deleteDirectoryOrFile)" should {
    /**
     * Non matching Sha:
     * {
     * "message": "FolderFileTest.md does not match 5bcfef062cc01f52b25baa610943480708e0764",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file",
     * "status": "409"
     *
     * Non matching file name, repo name, user name, to sha:
     * {
     * "message": "Not Found",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file",
     * "status": "404"
     * }
     * non message in body:
     * {
     * "message": "Invalid request.\n\n\"message\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file",
     * "status": "422"
     * }
     *
     * no sha in body:
     * {
     * "message": "Invalid request.\n\n\"sha\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file",
     * "status": "422"
     * }
     */
    "return a Json Response" when {

      "(all correct)- called with: userName, repoName, path, delete request" in {
        // Set up WireMock to return a 200 response indicating successful deletion
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "status" -> "success"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid Path)- called with: userName, repoName, invalid path, delete request" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val invalidFilePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$repoName/contents/$invalidFilePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$invalidFilePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid Repo)- called with: userName, invalid repoName, path, delete request" in {
        val userName = "testUser"
        val InvalidRepoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$InvalidRepoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$InvalidRepoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid UserName)- called with: invalid userName, repoName, path, delete request" in {
        val InvalidUserName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$InvalidUserName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$InvalidUserName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid Sha for the file path)- called with: userName, repoName, path, delete request(invalid sha)" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "NotTheRightSha")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid request Body- no commit message)- called with: userName, repoName, path, delete request(missing message)" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"sha\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "422"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid request Body- no sha)- called with: userName, repoName, path, delete request(missing sha)" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = DeleteModel("Deleting a file", "cfa87189d46de87b0aa55c31164dd9a4b7e8fe9c")

        val requestBody = Json.obj(
          "message" -> formData.message,
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"sha\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "422"
        )

        stubFor(WireMock.delete(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.delete[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }
    }
  }

  "GitHubConnector.create when retrieving file content (createFile)" should {
    /**
     * // file name and the formDataFileName always the same
     * fileName can be missing from body
     *
     * Non matching repo name, user name:
     * {
     * "message": "Not Found",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#delete-a-file",
     * "status": "404"
     * }
     * non message in body:
     * {
     * "message": "Invalid request.\n\n\"message\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "422"
     * }
     * non content in body:
     * {
     * "message": "Invalid request.\n\n\"content\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "422"
     * }
     *
     * no fileName in body:
     * {
     * "message": "Created"
     * }
     */
    "return a Json Response" when {

      "(all correct)- called with: userName, repoName, path, create Request Body" in {
        // Set up WireMock to return a 200 response indicating successful deletion
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "fileName" -> formData.fileName
        )

        val jsonResponse = Json.obj(
          "message" -> "Create"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(all correct)- called with: userName, repoName, path, create Request Body missing FileName" in {
        // Set up WireMock to return a 200 response indicating successful deletion
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
        )

        val jsonResponse = Json.obj(
          "message" -> "Create"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(invalid UserName)- called with: invalid userName, repoName, path, create Request Body" in {
        val userNameInvalid = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "fileName" -> formData.fileName
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userNameInvalid/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userNameInvalid/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(invalid repoName)- called with: userName, invalid repoName, path, create Request Body" in {
        val userName = "testUser"
        val repoNameInvalid = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "fileName" -> formData.fileName
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#delete-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoNameInvalid/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoNameInvalid/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(missing message)- called with: userName, repoName, path, create Request Body - missing message" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "content" -> formData.content,
          "fileName" -> formData.fileName
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"message\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
          "status" -> "422"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(missing content)- called with: userName, repoName, path, create Request Body - missing content" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = CreateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "CreateFile.md")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "fileName" -> formData.fileName
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"content\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
          "status" -> "422"
        )
        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }


    }
  }

  "GitHubConnector.create when retrieving file content (editContent)" should {
    /** Possible options/
     * Different file name to current file name creates - Do not test
     * Trying to name a file that already exists - FileName exists - Sha from different file
     * {
     * "message": "NewFile124 does not match b45ef6fec89518d314f546fd6c3025367b721684",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "409"
     * }
     * Invalid UserName
     * Invalid RepoName
     * Missing message:
     * {
     * "message": "Invalid request.\n\n\"message\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "422"
     * }
     * Missing content:
     * {
     * "message": "Invalid request.\n\n\"content\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "422"
     * }
     * Missing Sha:
     * {
     * "message": "Invalid request.\n\n\"sha\" wasn't supplied.",
     * "documentation_url": "https://docs.github.com/rest/repos/contents#create-or-update-file-contents",
     * "status": "422"
     * }
     *
     */
    "return a Json Response" when {

      "(all correct)- called with: userName, repoName, path, update Request Body" in {
        // Set up WireMock to return a 200 response indicating successful deletion
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = UpdateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "b45ef6fec89518d314f546fd6c3025367b721684", "fileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Create"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        // Call your connector method here
        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        // Assert the response is as expected
        result shouldBe Right(jsonResponse)
      }

      "(differentFileName)- called with: userName, repoName, different path, update Request Body (creates a new file)" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "differentPath"
        val formData = UpdateFileModel("Creating new file", "SGVsbG8sIFdvcmxkIQ==", "b45ef6fec89518d314f546fd6c3025367b721684", "newFileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "File created"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(201) // Assuming 201 for creation of new file
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(different File Name that exists)- called with: userName, repoName, different path that exists(non matching sha to path), update Request Body" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "differentPath"
        val formData = UpdateFileModel("Updating existing file", "SGVsbG8sIFdvcmxkIQ==", "mismatchedSha", "existingFileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "NewFile124 does not match b45ef6fec89518d314f546fd6c3025367b721684",
          "documentation_url" -> "\"https://docs.github.com/rest/repos/contents#update-a-file\",",
          "status" -> "409",
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(409)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(invalid userName)- called with: invalid userName, repoName, path, update Request Body" in {
        val userName = "invalidUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = UpdateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "b45ef6fec89518d314f546fd6c3025367b721684", "fileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#update-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(invalid RepoName)- called with: userName, invalid repoName, path, update Request Body" in {
        val userName = "testUser"
        val repoName = "invalidRepo"
        val filePath = "somePath"
        val formData = UpdateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "b45ef6fec89518d314f546fd6c3025367b721684", "fileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Not Found",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#update-a-file",
          "status" -> "404"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(missing message in body)- called with: userName, repoName, path, update Request Body" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = UpdateFileModel("", "SGVsbG8sIFdvcmxkIQ==", "b45ef6fec89518d314f546fd6c3025367b721684", "fileName")

        val requestBody = Json.obj(
          "content" -> formData.content,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"message\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#update-a-file",
          "status" -> "422"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(missing content in body)- called with: userName, repoName, path, update Request Body" in {
        val userName = "testUser"
        val repoName = "testRepo"
        val filePath = "somePath"
        val formData = UpdateFileModel("Creating", "", "b45ef6fec89518d314f546fd6c3025367b721684", "fileName")

        val requestBody = Json.obj(
          "message" -> formData.message,
          "sha" -> formData.sha
        )

        val jsonResponse = Json.obj(
          "message" -> "Invalid request.\n\n\"content\" wasn't supplied.",
          "documentation_url" -> "https://docs.github.com/rest/repos/contents#update-a-file",
          "status" -> "422"
        )

        stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
          .withRequestBody(equalToJson(requestBody.toString()))
          .willReturn(aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse.toString())
          ))

        val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

        result shouldBe Right(jsonResponse)
      }
      "(missing sha in body)- called with: userName, repoName, path, update Request Body" in {
          val userName = "testUser"
          val repoName = "testRepo"
          val filePath = "somePath"
          val formData = UpdateFileModel("Creating", "SGVsbG8sIFdvcmxkIQ==", "", "fileName")

          val requestBody = Json.obj(
            "message" -> formData.message,
            "content" -> formData.content
          )

          val jsonResponse = Json.obj(
            "message" -> "Invalid request.\n\n\"sha\" wasn't supplied.",
            "documentation_url" -> "https://docs.github.com/rest/repos/contents#update-a-file",
            "status" -> "422"
          )

          stubFor(WireMock.put(urlEqualTo(s"/repos/$userName/$repoName/contents/$filePath"))
            .withRequestBody(equalToJson(requestBody.toString()))
            .willReturn(aResponse()
              .withStatus(422)
              .withHeader("Content-Type", "application/json")
              .withBody(jsonResponse.toString())
            ))

          val result = Await.result(connector.create[JsValue](s"http://localhost:8080/repos/$userName/$repoName/contents/$filePath", requestBody).value, 5.seconds)

          result shouldBe Right(jsonResponse)
      }
    }
  }
}
/**
Potential error case
//      "GitHub API returns an error from the request" in {
//        val userName = "testuser"
//
//        stubFor(WireMock.get(urlEqualTo(s"/users/$userName"))
//          .willReturn(aResponse()
//            .withFault(Fault.EMPTY_RESPONSE)
//          ))
//        println(userName)
//        //  Call your connector method here
//        try {
//          val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$userName").value, 5.seconds).
//          println(result)
//          result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
//        } catch {
//          case ex: Exception =>
//            println(s"Caught exception: ${ex.getMessage}")
//            fail("The test failed due to an unexpected exception.")
//        }
//      }

 "Return a 500 Bad Api Response " when {

 "GitHub API returns an error from the request" in {
 val userName = "testuser"
 val errorStatusCodes = List(400, 401, 403, 404, 500, 502, 503)

 errorStatusCodes.foreach { statusCode =>
 stubFor(WireMock.get(urlEqualTo(s"/users/$userName/repos"))
 .willReturn(aResponse()
 .withStatus(statusCode)
 .withHeader("Content-Type", "application/json")
 .withBody(s"""{"message": "Error with status $statusCode"}""")
 ))

 // Call your connector method here
 val result = Await.result(connector.get[JsValue](s"http://localhost:8080/users/$userName/repos").value, 5.seconds)

 // Assert that the result is a Left with the appropriate APIError
 result shouldBe Left(APIError.BadAPIResponse(statusCode, "Could not connect"))
 }
 }
 }
*/