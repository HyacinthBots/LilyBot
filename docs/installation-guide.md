# Get Started with Lily

Here is an in depth guide on how to set up Lily for your own personal use. If you have issues with this guide, please join [Iris's discord](https://discord.gg/jQJnav2jPu) for support.

### Step 0.5 - Install tools
If you don't already have it, you will need to [install Java](https://adoptium.net/).
If you're trying to run the Kotlin branch, you'll also need to [install Kotlin](https://kotlinlang.org/docs/command-line.html#snap-package).

### Step 1 - Set up a bot using the discord developer portal

Head over to the [Discord Developer Portal](https://discord.com/developers/applications) and press the "New Application" button in the top right corner.

Give it a name and press create. Then, head to the bot tab on the left and click "Add Bot". Click "Yes, do it!" Once you have your bot, scroll down and make sure that the "PRESENCE INTENT", "SERVER MEMBERS INTENT", and "MESSAGE CONTENT INTENT" switches are all toggled to on. Once that's done, go to the OAuth2 tab on the left sidebar, scroll down to the checkboxes, and select the ones for "bot" and "applications.commands". Copy the URL, enter it into your browser, select the server you want to add your Lily to, and click authorize. If everything worked correctly, your Lily instance should appear in the members list of that server.

### Step 2 - Clone the repository

To clone the Lily repository, open the command line for your respective OS and run `git clone https://github.com/IrisShaders/LilyBot.git`
This will clone Lily to your home directory.

### Step 3 - Set up your .env file

In the root directory of the files you've cloned (It has files such as `README.md` and `CONTRIBUTING.md`), create a file named .env. You should fill your file using the format below with the relevant details filled in.

```
TOKEN=BOT_TOKEN_YEEF
ADMIN_ROLE=ADMIN_ROLE_ID
MODERATOR_ROLE=MODERATOR_ROLE_ID
TRIAL_MODERATOR_ROLE=TRIAL_MODERATORS_ROLE_ID
MODERATOR_PING_ROLE=PING_MODERATORS_ROLE_ID
SUPPORT_ROLE=SUPPORT_TEAM_ROLE_ID
GUILD_ID=GUILD_ID
MOD_ACTION_LOG=MOD_ACTION_LOG_CHANNEL_ID
MESSAGE_LOGS=MESSAGE_LOGS_CHANNEL_ID
CONFIG_PATH=lily.toml
JOIN_CHANNEL=JOIN_CHANNEL_ID
SUPPORT_CHANNEL=SUPPORT_CHANNEL_ID
GITHUB_OAUTH=GITHUB_OAUTH_TOKEN
```

To get the token for your bot, return to the Discord developers portal for your bot, got to the bot tab on the left sidebar, and under where it says TOKEN click copy. BE VERY CAREFUL TO NEVER SHARE THIS WITH ANYONE AS IT PROVIDES ACCESS TO YOUR BOT. If you ever do accidentally leak it, immediately head to that page and click the regenerate button.

In order to generate the majority of this data you will need to have Discord's `Developer Mode` enabled. This is located in `User Settings/Advanced/Developer Mode`. Once you've done this, right-click on the channel name you want to use for "ACTION_LOG" and select "Copy ID". Do the same but for the roles you'd like to use for "MODERATOR_ROLE", "TRIAL_MODERATOR_ROLE", and "MUTED_ROLE". Copy the owner of the server's ID and put that in "OWNER". Copy the server's ID and put that in "GUILD_ID"

CONFIG_PATH should be lily.properties by default. See Step 7 for more details on custom configurations for Lily.

GITHUB_OAUTH is personal access token, similar to the token for your bot. You can make one by going to [Settings/Developer settings/Personal access tokens](https://github.com/settings/tokens) and clicking generate new token. You don't need to select any scopes. As with the token for your bot, DO NOT SHARE THIS WITH ANYONE.

### Step 4 - Building Lily

After your `.env` file is successfully established, reopen the command line. Run `cd LilyBot` and then run `gradlew build` if you're on Windows and `./gradlew build` if you're on Mac or Linux. This might take a while but the output should eventually say `BUILD SUCCESSFUL`.

### Step 5 - Running Lily
To run Lily, open your favorite file manager and navigate to `LilyBot/builds/lib` Find the file with a `-all` at the end, for example `LilyBot-1.0-all.jar`. Copy this file to the directory that contains your `.env` and `lily.properties` files. Then, reopen the command line and run the command `java -jar LilyBot-1.0-all.jar` where `LilyBot-1.0-all.jar` is the name of the `.jar` file you found before. You should receive a message in your designated action log channel saying that Lily is online.

### Step 6 - Profit
Congrats! You now have your own fully functioning version of LilyBot!

### Step 7 - Configuring Lily by editing lily.toml (optional)
You can add your own custom commands in a file with the `.toml` suffix. You will need to specify the path and name of this file in the `CONFIG_PATH` section of your `.env` file. The default `lily.properties` is what is used on the Iris server. Custom commands can only be used to create a simple slash command that sends an embed with text.

To establish your own configuration, create a `.toml` file. Anything stored in this file is in the form of a key (e.g. `status`) and a value (e.g. `Iris`) seperated by an equals sign (e.g. `status = Iris`) Each key and value should be on a new line. In the default file, keys are grouped by the command they create and loosely alphabetized. At the top of the file, create a key `commands` and specify, seperated with a space, a list of all the commands you would like. For example, if we wanted commands `/help`, `/rules`, and `/welcome` this first entry would look like `commands = help rules welcome`. Next, you will need to define the help, title, and description for each command. The help value is what appears in Discord's autofill when typing in the command, the title is displayed at the top of the command in bold and a slightly larger font, and the description is the body of the command. Each of these must be defined on its own line. So if we wanted the `/welcome` command to have a help of `Welcomes a user to to the server` a title of `Welcome!` and a body of `Welcome to the server! Be sure to read the rules and have a great time!` that would look like 
```toml
[[command]]
name = "really-interesting-command-name"
help = "This is really helpful!"
title = "The Really Interesting Command"
description = "This command is really interesting!"
```
The other component of custom commands is children. These are commands that all have the same prefix and various different second values (e.g. Iris uses children for our rule commands). If we wanted to have our `/rules` command to have 2 subcommands it would look like this:
```toml
[[command.subcommand]] 
name = ""
help = ""
title = ""
description = ""
[[command.subcommand]]
name = ""
help = ""
title = ""
description = ""
```
Each different subcommand needs to be seperated into a different array with `[[command.subcommand]]`, simialr to how we intially defined commands. Each command also needs a help, title, and description. For example, Iris's Discord's rule 1 commmand is definied as

```toml
[[command.subcommand]]
name = "Rule 1"
help = "Reminds the user of Rule 1: Be decent to one another."
title = "Rule 1"
description = "Be decent to one another. We're all human. Any and all forms of bigotry, harassment, doxxing, exclusionary, or otherwise abusive behavior will not be tolerated. Excessive rudeness, impatience, and hostility are not welcome. Do not rage out or make personal attacks against other people. Do not encourage users to brigade/raid other communities."
```

Be aware that it may take Discord a moment to refresh any commands changed in this way. You will need to restart the bot after you make any changes in this way.
