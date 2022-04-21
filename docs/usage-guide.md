# Using LilyBot

This is a guide on how to **add the official instance of Lily to your sever**.
If you're looking to host Lily for yourself,
try our [hosting guide](https://github.com/IrisShaders/LilyBot/blob/main/docs/hosting-guide.md).
If you're looking to set up a development environment for Lily,
try our [development guide](https://github.com/IrisShaders/LilyBot/blob/main/docs/development-guide.md).

If you have issues with this guide, please join [our discord](https://discord.gg/hy2329fcTZ) for support.

# Step 1 - Obtain an invitation link
Currently, Lily is not a fully public bot.
If you want to add her to your server, please contact `IMS#7902` or `NoComment#6411` on Discord.
They will provide you with a link for inviting Lily to your server.

# Step 2 - Using your invitation link
Once you've received this link, simply open it in your browser. You may need to log into your Discord account.
You should then select the server you want to add Lily to. You'll need to be an administrator in that server.
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

*Please note that it is not currently possible to edit Lily's custom commands per guild on the official instance.
This functionality will be added in a later version.*
