# Get Started with Lily

Here is an in depth guide on how to set up Lily for your own personal use. If you have issues with this guide, please join [Iris's discord](https://discord.gg/jQJnav2jPu) for support.

### Step 1 - Set up a bot using the discord developer portal

Head over to the [Discord Developer Portal](https://discord.com/developers/applications) and press the "New Application" button in the top right corner.

Give it a name and press create. Then, head to the bot tab on the left and click "Add Bot". Click "Yes, do it!" Once you have your bot, scroll down and make sure that the "PRESENCE INTENT", "SERVER MEMBERS INTENT", and "MESSAGE CONTENT INTENT" switches are all toggled to on. Once that's done, go to the OAuth2 tab on the left sidebar, scroll down to the checkboxes, and select the ones for "bot" and "applications.commands". Copy the URL, enter it into your browser, select the server you want to add your Lily to, and click authorize. If everything worked correctly, your Lily instance should appear in the members list of that server.

### Step 2 - Clone the repository

To clone the Lily repository, open the command line for your respective OS and run `git clone https://github.com/IrisShaders/LilyBot.git`
This will clone Lily to your home directory.

### Step 3 - Set up your .env file

In the root directory of the files you've cloned (It has files such as `README.md` and `CONTRIBUTING.md`), create a file named .env. You should fill your file using the format below with the relevant details filled in.

```
TOKEN= TOKEN_YEEF
MODERATOR_ROLE= ROLE_ID
TRIAL_MODERATOR_ROLE= ROLE_ID
MUTED_ROLE= ROLE_ID
GUILD_ID= SERVER_ID
ACTION_LOG= CHANNEL_ID
JOIN_MESSAGES= CHANNEL_ID
OWNER= OWNERS_ID
GITHUB_OAUTH= GITHUB_OAUTH
CONFIG_PATH= CONFIG_PATH
```

To get the token for your bot, return to the Discord developers portal for your bot, got to the bot tab on the left sidebar, and under where it says TOKEN click copy. BE VERY CAREFUL TO NEVER SHARE THIS WITH ANYONE AS IT PROVIDES ACCESS TO YOUR BOT. If you ever do accidentally leak it, immediately head to that page and click the regenerate button.

In order to generate the majority of this data you will need to have Discord's `Developer Mode` enabled. This is located in `User Settings/Advanced/Developer Mode`. Once you've done this, right-click on the channel name you want to use for "ACTION_LOG" and select "Copy ID". Do the same but for the roles you'd like to use for "MODERATOR_ROLE", "TRIAL_MODERATOR_ROLE", and "MUTED_ROLE". Copy the owner of the server's ID and put that in "OWNER". Copy the server's ID and put that in "GUILD_ID"

CONFIG_PATH should be lily.properties by default. See Step 7 for more details on custom configurations for Lily.

GITHUB_OAUTH is personal access token, similar to the token for your bot. You can make one by going to [Settings/Developer settings/Personal access tokens](https://github.com/settings/tokens) and clicking generate new token. You don't need to select any scopes. As with the token for your bot, DO NOT SHARE THIS WITH ANYONE.

### Step 4 - Building Lily

After your `.env` file is successfully established, reopen the command line. Run `cd LilyBot` and then run `gradlew build` if you're on Windows and `./gradlew build` if you're on Mac or Linux. This might take a while but the output should eventually say `BUILD SUCCESSFUL`.

### Step 5 - Running Lily
To run Lily, open your favorite file manager and navigate to `LilyBot/builds/lib` Find the file with a `-all` at the end, for example `LilyBot-1.0.jar`. Copy this file to the directory that contains your `.env` and `lily.properties` files. Then, reopen the command line and run the command `java -jar LilyBot-1.0.jar` where `LilyBot-1.0.jar` is the name of the `.jar` file you found before. You should receive a message in your designated action log channel saying that Lily is online.

### Step 6 - Profit
Congrats! You now have your own fully functioning version of LilyBot!

### Step 7 - Configuring Lily by editing lily.properties (optional)
You can add your own custom commands in a file with the `.properties` suffix. You will need to specify the path and name of this file in the `CONFIG_PATH` section of your `.env` file. At the top of the file you will need to specify a default status and a list of commands. Then for each command you will need a value for help, title, and description. It is also possible for a command to have sub-commands (children). The default `lily.properties` does a very good job of illustrating all of these concepts so check that out.