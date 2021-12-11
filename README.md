# LilyBot

## Links
* **Visit [our website](https://irisshaders.net) for downloads and pretty screenshots!**
* Visit [our Discord server](https://discord.gg/jQJnav2jPu) to chat about Iris projects (such as LilyBot) and get support!
* Visit [my Patreon page](https://www.patreon.com/coderbot) to support the continued development of Iris projects!

### Why?
* Late in August 2021, Discord.py was discontinued. This is previously what the Iris Project's Discord bot was written in, So [NoComment](https://github.com/NoComment1105), [Miss Corruption](https://github.com/Miss-Corruption), [Maxigator](https://github.com/Maxigator) and [chalkyjeans](https://github.com/chalkyjeans) took on the task of rewriting the bot in JDA.
* Then again in October 2021, we changed course and rewrote the bot *again*, this time in Kord and KordEX

### How can I use LilyBot in my own server!?
1. Set up a bot using the discord developer portal (https://discord.com/developers/applications)
2. Invite your bot to your discord server, while granting it the `applications.commands` scope located in the OAuth2 Settings tab	
3. Download the Repo
4. Create a `.env` file and fill it with this:
```
TOKEN= TOKEN_YEEF
MODERATOR_ROLE= ROLE_ID
TRIAL_MODERATOR_ROLE= ROLE_ID
MUTED_ROLE= ROLE_ID
GUILD_ID= SERVER_ID
ACTION_LOG= CHANNEL_ID
OWNER= OWNERS_ID
GITHUB_OAUTH= GITHUB_OAUTH
CONFIG_PATH= CONFIG_PATH
```
5. Replace the information accordingly (You'd need to have `Developer Mode` enabled, located in `Settings/Advanced/Developer Mode`)
6. Run the main class
7. Profit


### What was used to write this bot and what does each thing do?
* [Kord](https://github.com/kordlib/kord): The Kotlin API for Discord.
* [KordEx](https://github.com/Kord-Extensions/kord-extensions): Integrated commands and extensions framework for Kord.
* [JetBrains Exposed](https://github.com/JetBrains/Exposed): A Database library for storing various differnt things, such as warnings and mute timings
* [Logback](https://github.com/qos-ch/logback): Removes some fake errors.
* [Github-API](https://github.com/hub4j/github-api): The API for GitHub commands. Helps to produce the GitHub Repo, Issue and Profile embeds

#### This repo is open to contributions by the community. Please contribute if you see anywhere it's needed.
