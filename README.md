# LilyBot

## Links
* **Visit [our website](https://irisshaders.net) for downloads and pretty screenshots!**
* Visit [our Discord server](https://discord.gg/jQJnav2jPu) to chat about Iris projects (such as LilyBot) and get support!
* Visit [my Patreon page](https://www.patreon.com/coderbot) to support the continued development of Iris projects!

### Why?
* Late in August 2021, Discord.py was discontinued. This is previously what the Iris Project's Discord bot was written in, So [NoComment](https://github.com/NoComment1105), [Miss Corruption](https://github.com/Miss-Corruption), [Maxigator](https://github.com/Maxigator) and [chalkyjeans](https://github.com/chalkyjeans) took on the task of rewriting the bot in JDA.

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
JOIN_MESSAGES= CHANNEL_ID
OWNER= OWNERS_ID
GITHUB_OAUTH= GITHUB_OAUTH
CONFIG_PATH= CONFIG_PATH
```
5. Replace the information accordingly (You'd need to have `Developer Mode` enabled, located in `Settings/Advanced/Developer Mode`)
6. Run the main class
7. Profit

### What was used to write this bot?
* JDA: https://github.com/DV8FromTheWorld/JDA
* JDA Chewtils: https://github.com/Chew/JDA-Chewtils (well [chalkyjeans' fork](https://github.com/chalkyjeans/JDA-Chewtils) of it)
* Logback: https://github.com/qos-ch/logback
* dotenv: https://github.com/cdimascio/dotenv-java
* Github-API: https://github.com/hub4j/github-api

#### What do each of these do?
* JDA: The Java API for Discord
* JDA Chewtils: A forked fork of JDA Utils for useful implementations
* Logback: Removes some fake errors
* dotenv: For accessing `.env` files that contain the bots sensitive data
* Github-API: The API for GitHub commands. Helps to produce the GitHub Repo, Issue and Profile embeds

#### This repo is open to contributions by the community. Please contribute if you see anywhere it's needed.
