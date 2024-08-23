
# Gary's GitHub

This project is an implementation of GitHub. The project is an API with front end developed using Scala and the Play framework. Within this project you can perform operations on Users and Repositories.  

## Technologies & Dependencies
- Languages
  - Scala 2.13.8
  - HTML
  - CSS
- Testing 
  - ScalaTest 
  - ScalaMock 
  - ScalaTestPlus
- HTTP Handling
  - Play WS
- Plugins/Dependencies
    - Play scala framework
    - HMRC library - Mongo Play connector
    - Guice - Dependency injection
    - Cats Core



## The current project scope

###### Users:
Create, update and delete dummy GitHub users within a MongoDB Database. 

Search for a user will first search your database then GitHub's own database. If they exist in GitHub a mirrored copy will be made in your MongoDB Database. 

###### Repositories 
Display public repositories for any user that exists on GitHub (if they are from your database this will be empty). 

Read, Create, Update and Delete Files and Directories in Repositories that belong to you as a logged-in User. (see running the project)


## Running the project

To run this project you will need to do a number of steps: 
1. Set up and run Docker for the MongoDB database
2. Retrieve and set a GitHub Authentication Token
3. Optionally use Studio3T to see the Database


### Setting up Docker
  1. Install Docker

- Docker for Mac: Follow the installation guide [here](https://docs.docker.com/desktop/install/mac-install/).
- Docker for Windows: Follow the installation guide [here](https://docs.docker.com/desktop/install/windows-install/).
- Docker for Linux: Follow the installation guide [here](https://docs.docker.com/desktop/install/linux-install/).

  
  2. Verify Docker Installation
  
After installation verify docker is installed correctly by opening a terminal (or Command Prompt on windows) and running

```Bash
docker --version
```
*You should see Docker's version number if the installation was successful*

  3. Pull and Run MongoDB Docker Container
  
Now pull the latest MongoDB Docker image and run a MongoDb container on port 27017
```Bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

  4. Verify MongoDB is running
```Bash
docker ps
```
*This command lists all running containers. You should see the mongodb container listed*

*You should also see the container running in the Docker Desktop app*


### Setting up GitHub Authentication token

This project uses GitHub’s API in order to access public repositories. In order to create, update or delete files in public repositories that belong to you (or your logged in account) it requires a GitHub Authentication Token. This is to send to the GitHub API alongside the request as only the logged-in user can make changes to their repo.

### How to set up the Authentication token 

There are three methods to adding the Authentication token (.zshrc, .zprofile, IDEenvironment). The preferred method is updating your .zshrc file

#### Get the Authentication token
    1. log in to your github to see the home page
    2. Select your icon in the top right
    3. select settings
    4. select developer settings
    5. Open "Personal Access Tokens" and Navigate to Tokens (classic)
    6. Generate new token (classic)
    7. Select all options allowing you to CRUD the Repo's and your user profile but also for future development of workflows without creating another token.
    8. You can set it to timeout but if you come back to this project in future you will need another token. For now set this to not expire.

#### Update Your Environment Variable

**These instructions are if you are using the ZSH shell (default for mac).**

*If you are using Bash shell (default for most Linux) see file BashShell.md. If you are using Powershell (default for most windows) see file PowerShell.md.*


1. Open your terminal and run the following command: 

```zsh
nano ~/.zshrc
```

- or if using vim 

```zsh
vim ~/.zshrc
```

2. Add the following line to the file (replacing the github token with the one we retrieved before)

```zsh
export AuthPassword=your_github_token_here
```

3. Save and Close the file

   - for nano 
        - "CTRL + X" to exit 
        - "Y" - When prompted to save
        - "ENTER" - To save 
   - for vim 
        - "ESC" - switch to normal
        - "WQ" - save file and exit
        - "ENTER" - Execute save and quit 
     

4. Apply the changes made

in the terminal run
```zsh
source ~/.zshrc
```
5. Confirm insertion

```zsh
echo $AuthPassword
```

*Your Authentication Token is now set in an environment variable on your local machine. This will not be tracked by GitHub or within your project. If you have the project open close it and reopen it to ensure that it has the refreshed .zshrc file and Auth token.*

*The GitAuthToken file should now have your token. In Intellij you can extend this object with App to make it playable and print line the token if you would like to check. Remove the extends from App before continuing to avoid future clash.*

-----
## Future Developments: 

# Auth token authorisation: 
- In order to host this site live we would like to handle GitHub Auth Token authentication within the project
- The process to do this is currently unclear 
- There is a possibility of being able to receive and store as a local variable in the future
- Buttons

# Log in and Log Out to use a Database instance
- Currently, a logged-in user is stored in a local variable
- Updating the logged-in user to be a database instance would allow for persistent data beyond compiles

# Update front end to be responsive
- The front end requires further styling and design to be responsive to different device types and sizes
- Front end development may be possible with bootstrap or other solutions

# Update the user DataModel
- Updates to this model should include: 
  - retrieving avatar url's from Github
  - removal of gitHub boolean as this serves no current purpose

# Displaying the logged-in user globally
- Retrieving the logged-in user globally and utilising this within a display visible on all pages. 

# Updating the logged-in user's details
- The function exists to update user details in the database
- Moving the log in function to utilise the database would allow for this project to display varied data to GitHub.

# Updating the connector to handle errors
- The connector can be improved to catch the Json error status returned by GitHub's API 
- handling these errors in the connector would simplify the GitHubService

# Flashing: Success and Error
- Flashing can be used in order to flash a success or error message after successful CRUD operations

# More Robots
- self-explanatory- you can never have enough Robots! 

-----

*Collaboration of:* 

**Spencer Clarke-Griffiths- https://github.com/SpencerCGriffiths**

**Jamie Letts- https://github.com/jamie2210**

**Roshan Gautam- https://github.com/Roshangautam2052**