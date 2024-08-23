# Setting environment variable for PowerShell (Windows)

1. Open your PowerShell profile script:

```PowerShell 
notepad $PROFILE
```

2. Add the following line to the file (replacing the github token with the one we retrieved before)
```PowerShell
$env
= "your_github_token_here"
```


3. Save and close the file in Notepad:

- Click "File" > "Save"
- Close Notepad
  
4. Apply the changes:

in the terminal run
```PowerShell
. $PROFILE
```

Confirm insertion
```PowerShell
echo $env
```

*Your Authentication Token is now set in an environment variable on your local machine. This will not be tracked by GitHub or within your project. If you have the project open, close it and reopen it to ensure that it has the refreshed .bashrc file and Auth token.*

*The GitAuthToken file should now have your token. In IntelliJ, you can extend this object with App to make it playable and print line the token if you would like to check. Remove the extends from App before continuing to avoid future clash.*

-----

*Collaboration of:*

**Spencer Clarke-Griffiths- https://github.com/SpencerCGriffiths**

**Jamie Letts- https://github.com/jamie2210**

**Roshan Gautam- https://github.com/Roshangautam2052**
