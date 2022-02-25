#Getting started with LilyBot
Here is an in depth guide on how to set up Lily. If you have issues with this guide, please join [Iris's discord](https://discord.gg/jQJnav2jPu) for support.

## Adding the official lily instance to your server
If you simply want to utilize all the great features Lily has to offer in your server without modification, this is the section of the guide for you.

Currently, Lily is not a fully public bot. If you want to add her to your server, please contact `IMS#7902` or `NoComment#6411` on Discord. They will provide you with a link for inviting Lily to your server.

Once you've received this link, simply open it in your browser. You may need to log into your Discord account. You should then select the server you want to add Lily to (you'll need to be an administrator in that server) and click `Authorize`.

If all has gone successfully so far, you should see LilyBot in the sidebar of your server. You'll then need to run the `/config set` command, inputting all the necessary values. In order to get the `guildid` argument, you will need to have Discord's `Developer Mode` enabled. This is located in `User Settings/Advanced/Developer Mode`. Once that's enabled, simply right-click on your server's icon in the server list, select `Copy ID`, and paste that into the command argument.

*Please note that it is not currently possible to edit Lily's custom commands per guild on the official instance. This functionality will be added in a later version*

## Setting up your own Lily instance
If you want to set up your own copy of Lily for development or just to host your own instance, this is the section of the guide for you.

### Step 1 - Install tools
If you don't already have them, you will need to install [Java](https://adoptium.net/) and [Kotlin](https://kotlinlang.org/docs/command-line.html#snap-package).

### Step 2 - Set up a bot using the discord developer portal

Head over to the [Discord Developer Portal](https://discord.com/developers/applications) and press the "New Application" button in the top right corner.

Give your application a name and press create. Then, head to the bot tab on the left and click "Add Bot". Click "Yes, do it!"
Once you have your bot, head to the bot tab on the left sidebar and make sure that `PRESENCE INTENT`, `SERVER MEMBERS INTENT`, and `MESSAGE CONTENT INTENT` switches are all toggled to on.
Then, select the OAuth2 tab and make sure that the scopes and permissions selected match the image below.
![OAuth2example](resources/OAuth2example.png)

Copy the URL generated, enter it into your browser, select the server(s) you want to add your Lily to, and click authorize.
If everything worked correctly, your Lily instance should appear in the members list of that server.

### Step 3 - Clone the repository

To clone the Lily repository, open the command line for your respective OS and run `git clone https://github.com/IrisShaders/LilyBot.git` This will clone Lily to your home directory.

### Step 4 - Set up your .env file

In the root directory of the files you've cloned, create a file named .env. You should fill your file using the format below with the relevant details filled in.

```
TOKEN=BOT_TOKEN_YEEF
CUSTOM_COMMANDS_PATH=commands.toml
TEST_GUILD_ID=GUILD_ID
TEST_GUILD_CHANNEL=CHANNEL_ID
GITHUB_OAUTH=GITHUB_OAUTH_TOKEN or null if you don't want GitHub commands
JDBC_URL=jdbc:sqlite:<path to where the database should go> This is optional and defaults to ./data in the root directory.
SENTRY_DSN=SENTRY_DSN or null if you don't want sentry integration
```

To get the token for your bot, return to the Discord developers portal for your bot, got to the bot tab on the left sidebar, and under where it says TOKEN click copy. BE VERY CAREFUL TO NEVER SHARE THIS WITH ANYONE AS IT PROVIDES ACCESS TO YOUR BOT. If you ever do accidentally leak it, immediately head to that page and click the regenerate button.

CUSTOM_COMMANDS_PATH should be commands.toml by default. See Step 7 for more details on custom command configuration.

To get any channel or guild IDs, you will need to have Discord's `Developer Mode` enabled. This is located in `User Settings/Advanced/Developer Mode`. This will also be useful later on when setting configs for guilds.

`TEST_GUILD_ID` is the server you plan to utilize for testing and want your online statuses sent. This can be the same as your main server. `TEST_GUILD_CHANNEL` is the channel online statues will be sent to within that server.

GITHUB_OAUTH is personal access token, similar to the token for your bot. You can make one by going to [Settings/Developer settings/Personal access tokens](https://github.com/settings/tokens) and clicking generate new token. You don't need to select any scopes. As with the token for your bot, DO NOT SHARE THIS WITH ANYONE.

### Step 5 - Building Lily

After your `.env` file is successfully established, reopen the command line. Run `cd LilyBot` and then run `gradlew build` if you're on Windows and `./gradlew build` if you're on Mac or Linux. This might take a while but the output should eventually say `BUILD SUCCESSFUL`.

### Step 6 - Running Lily
To run Lily, open your favorite file manager and navigate to `LilyBot/builds/lib` Find the file with a `-all` at the end, for example `LilyBot-1.0-all.jar`. Copy this file to the directory that contains your `.env` and `commands.toml` files. Then, reopen the command line and run the command `java -jar LilyBot-1.0-all.jar` where `LilyBot-1.0-all.jar` is the name of the `.jar` file you found before. You should receive a message in your designated action log channel saying that Lily is online.

### Step 7 - Profit
Congrats! You now have your own fully functioning version of LilyBot!

### Step 8 - Configuring Lily by editing commands.toml (optional)
You can add your own custom commands in a file with the `.toml` suffix. You will need to specify the path and name of this file in the `CONFIG_PATH` section of your `.env` file. The default `commands.toml` is what is used on the Iris server. Custom commands can only be used to create a simple slash command that sends an embed with text.

To establish your own configuration, create a `.toml` file. Anything stored in this file is in the form of a key (e.g. `name`) and a value (e.g. `Rule 1`) seperated by an equals sign (e.g. `name = Rule 1`) Each key and value should be on a new line. In the default file, keys are grouped by the command they create and loosely alphabetized. At the top of the file, create a key `commands` and specify, seperated with a space, a list of all the commands you would like. For example, if we wanted commands `/help`, `/rules`, and `/welcome` this first entry would look like `commands = help rules welcome`. Next, you will need to define the help, title, and description for each command. The help value is what appears in Discord's autofill when typing in the command, the title is displayed at the top of the command in bold and a slightly larger font, and the description is the body of the command. Each of these must be defined on its own line. So if we wanted the `/welcome` command to have a help of `Welcomes a user to to the server!` a title of `Welcome!` and a body of `Welcome to the server! Be sure to read the rules and have a great time!` that would look like 
```toml
[[command]]
name = "welcome"
help = "Welcomes a user to the server!"
title = "Welcome!"
description = "Welcome to the server! Be sure to read the rules and have a great time!"
```
The other component of custom commands is children. These are commands that all have the same prefix and various different second values (e.g. Iris uses children for our rule commands).
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
Each different subcommand needs to be seperated into a different array with `[[command.subcommand]]`, similar to how we initially defined commands. Each command also needs a help, title, and description. For example, Iris's Discord's rule 1 command is defined as

```toml
[[command.subcommand]]
name = "Rule 1"
help = "Reminds the user of Rule 1: Be decent to one another."
title = "Rule 1"
description = "Be decent to one another. We're all human. Any and all forms of bigotry, harassment, doxxing, exclusionary, or otherwise abusive behavior will not be tolerated. Excessive rudeness, impatience, and hostility are not welcome. Do not rage out or make personal attacks against other people. Do not encourage users to brigade/raid other communities."
```

Be aware that it may take Discord a moment to refresh any commands changed in this way. You will need to restart the bot after you make any changes in this way.
