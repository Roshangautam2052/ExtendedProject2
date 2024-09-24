# Log In Function

### Current thoughts: 

Currently, the plan is to have three Database Collections.
1. User's
   - Store created users that individuals can make that are not on GitHub, 
   - When signing in and the User is on GitHub a copy of the GitHub data is stored in this DataSet (allows for manipulation of the data in our app)
   (These's can be set with passwords as a part of the DataModel to mimic a password but it will not be envrypted.)
   (GitHub Apps does allow a redirect to GitHub for user's to actually sign in as a future development want)

2. Logged In
    - This collection is for the actively logged in individual. 
    - Creating an individual within this collection is the logged in user. 
    - Logging out deletes the person from this collection 
3. Saved
   - This is a collection for users that you are following? 

##### Logged In Collection: 

The logged in collection will allow a user to "create" a user in this collection when they log in. 
This data can be pulled from the other database collection or GitHub.
Their log in session will be persistent as the data will continue to exist in the database instance. 
To get the logged-in user or to check if there is a logged-in user then we can check the collection whether it exists and is non-empty. 
Logging out will be deleting this account from the database.
When a user is logged in they can update their information such as screen name etc. (this will trigger an update to both data sets)

update the DataModel to use a logged in User dataModel



*Future GitHub Auth* 
Log In with GitHub:  To achieve your goal of authenticating a user with their GitHub credentials, redirecting them to a local page, and accessing their data in a Scala Play Framework application, you can use GitHub's OAuth 2.0 flow. Here’s a step-by-step explanation of how you can implement this in Scala Play:
1. Register a GitHub App
   First, you need to create a GitHub App to obtain the Client ID and Client Secret:
1. Go to GitHub Developer Settings.
2. Click "New GitHub App."
3. Fill in details such as the callback URL (this should point to a route in your Play app, e.g., http://localhost:9000/auth/github/callback).
4. Set the necessary permissions for your app.
5. Once registered, GitHub will provide you with a Client ID and Client Secret.
2. Set Up Routes in Play Framework
   In your conf/routes file, define the routes to start the authentication process and handle the GitHub callback:
   conf
   Copy code
# Routes for GitHub OAuth
GET   /auth/github             controllers.AuthController.githubLogin
GET   /auth/github/callback    controllers.AuthController.githubCallback
3. Redirect to GitHub for Authentication
   In the AuthController, create the action that redirects the user to GitHub for authentication. This involves redirecting to GitHub's OAuth URL with your Client ID and the desired scopes (such as read:user to get basic user information).
   scala
   Copy code
   package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.Future

@Singleton
class AuthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

private val clientId = "YOUR_CLIENT_ID"
private val clientSecret = "YOUR_CLIENT_SECRET"
private val redirectUri = "http://localhost:9000/auth/github/callback"
private val githubOAuthUrl = "https://github.com/login/oauth/authorize"

def githubLogin: Action[AnyContent] = Action {
val githubUrl = s"$githubOAuthUrl?client_id=$clientId&redirect_uri=$redirectUri&scope=read:user"
Redirect(githubUrl)
}
}
When the user visits /auth/github, they will be redirected to GitHub for login.
4. Handle the GitHub OAuth Callback
   After the user successfully logs in and authorizes your app, GitHub will redirect back to your redirect_uri with a code parameter. This code is used to exchange for an access token.
   In the AuthController, handle the GitHub callback and make a POST request to GitHub to exchange the code for an access token. Then, use the token to retrieve user information.
   scala