# Developing LilyBot

This is a guide on how to set up Lily **for development purposes**.
If you're looking to add Lily to your server,
try our [usage guide](https://github.com/HyacinthBots/LilyBot/blob/main/docs/usage-guide.md).
If you have issues with this guide, please join [our discord](https://discord.gg/hy2329fcTZ) for support.

## Step 1 - Install tools
This tutorial utilizes IntelliJ IDEA due to it having full Kotlin support.
If you don't already have it installed, you can download it [here](https://www.jetbrains.com/idea/download/).
While by no means required, having ultimate edition will make debugging database related issues simpler.
If you're a student, you can get it for free through [GitHub Education](https://education.github.com/).

If you don't already have it, you will need to install [Java](https://adoptium.net/).
Kotlin comes bundled with Intellij.

Lily utilizes MongoDB. If you don't already have that installed,
you can find a tutorial on how to install it [here](https://docs.mongodb.com/manual/administration/install-community/).

## Step 2 - Clone the Lily repository
When you first open IntelliJ, select the `Get from VCS` button in the top right,
enter `https://github.com/HyacinthBots/LilyBot.git`, and press clone.
Wait for IntelliJ to finish setting up the project.

## Step 3 - Setting a `.env` file
In the root directory your project, create a file named `.env`.
You should fill your file using the format below with the relevant details filled in.

```
TOKEN=
TEST_GUILD_ID=
ONLINE_STATUS_CHANNEL=
MONGO_URI=
SENTRY_DSN=
ENVIRONMENT=
```

To get a token, join [our discord](https://discord.gg/hy2329fcTZ) and ask.
We'll provide you with a token for one of the testing instances in that server.
You can also find example `.env` files there. Please don't share these tokens.

To get any channel or guild IDs, you will need to have Discord's `Developer Mode` enabled.
This is located in `User Settings/Advanced/Developer Mode`.
You then simply right-click the channel, guild, user, or role and select `Copy ID`.

If you're running Mongo locally, you don't need to include a `MONGO_URI`.
If you're not running Mongo locally, you can learn how to obtain a URI
[here](https://docs.mongodb.com/guides/server/drivers/#obtain-your-mongodb-connection-string).

`GITHUB_OAUTH` is only needed if you plan on using the GitHub commands.
You can make one by going to [Settings/Developer settings/Personal access tokens](https://github.com/settings/tokens)
and clicking generate new token. You don't need to select any scopes. DO NOT SHARE THIS WITH ANYONE.

`SENTRY_DSN` is a connection string for Sentry, a logging tool.
You can find out more about it [here]( https://sentry.io/welcome/).

## Step 4 - Starting your Mongo database
Open a terminal window and start your Mongo database using the respective command for your OS.
You can find information on this command in the
[installation guide](https://docs.mongodb.com/manual/administration/install-community/) for your OS.

You'll want to use the command to stop the database whenever you finish a development session.
This command can be found in the same place as the one for starting the database.

## Step 5 - Profit!
Congrats! You now have a development instance of Lily fully up and running.
For more information on all of Lily's commands,
check out the [command list](https://github.com/HyacinthBots/LilyBot/blob/main/docs/commands.md).

If you're running this your own server, please be sure to allow the `Send Messages`, `Send Messages In Threads`,
`Add Reactions`, and `Use Application Commands` permissions for your moderator role and any other role you want to be
able to send messages when the server is locked.

We suggest joining [our discord](https://discord.gg/hy2329fcTZ)
for support, announcements of releases, Lily's online status, and even more!
