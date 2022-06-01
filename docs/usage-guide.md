# Using LilyBot

This is a guide on how to **add the official instance of Lily to your sever**.
If you're looking to set up a development environment for Lily,
try our [development guide](https://github.com/IrisShaders/LilyBot/blob/main/docs/development-guide.md).

If you have issues with this guide, please join [our discord](https://discord.gg/hy2329fcTZ) for support.

# Step 1 - Obtain an invitation link
Click on the official LilyBot instance, where you'll be able to use the `Add To Server` button

Alternatively, use [this link](
https://discord.com/api/oauth2/authorize?client_id=876278900836139008&permissions=1428479371270&scope=bot%20applications.commands) 
to invite LilyBot.

# Step 2 - Using your invitation link
The button will open new tab in your browser, where you can select the server you'd like to invite LilyBot too. You'll need to be an administrator in that server.
Click `Authorize`.

If all has gone successfully so far, you should see LilyBot in the sidebar of your server.
You'll then need to run the `/config set` command, inputting all the necessary values.
In order to get the `guildid` argument, you will need to have Discord's `Developer Mode` enabled.
This is located in `User Settings/Advanced/Developer Mode`.
Once that's enabled, simply right-click on your server's icon in the server list, select `Copy ID`,
and paste that into the command argument.

# Step 3 - Profit!
Congrats! You now have the official instance of LilyBot running in your server!

For more information on all of Lily's commands,
check out the [command list](https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md).

If you're running this your own server, please be sure to allow the `Send Messages`, `Send Messages In Threads`,
`Add Reactions`, and `Use Application Commands` permissions for your moderator role and any other role you want to be
able to send messages when the server is locked.

We suggest joining [our discord](https://discord.gg/hy2329fcTZ)
for support, announcements of releases, Lily's online status, and even more!
