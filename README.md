# LilyBot

## Links

* **Visit [our website](https://irisshaders.net) for downloads and pretty screenshots!**
* Visit [our Discord server](https://discord.gg/jQJnav2jPu) to chat about the mod and get support! It's also a great place to get development updates right as they're happening.
* Visit [my Patreon page](https://www.patreon.com/coderbot) to support the continued development of Iris!

### Why?
* Late on in August of 2021, Discord.py was discontinued. This is previously what the Iris Project's Discord bot was written in, So [NoComment](https://github.com/NoComment1105), [Miss Corruption](https://github.com/Miss-Corruption), [Maxigator](https://github.com/Maxigator) and [chalkyjeans](https://github.com/chalkyjeans) took on the task of rewiring the bot.
* Discord.py was discontinued due to Discord's announcement of Slash commands being required. An unfortunate thing that is being enforced around the 2022 mark. So this bot is written in JDA

### How can I use LilyBot in my own server!?

1. Make sure you have a bot set up in the discord developer portal
2. Download the Repo
3. Create a `.env` file and fill it with something like this:
```
TOKEN= TOKEN_YEEF
MODERATOR_ROLE= ROLE ID
MUTED_ROLE= ROLE_ID
GUILD_ID= SERVER_ID
ACTION_LOG= CHANNEL_ID
OWNER= OWNERS_ID
GITHUB_OAUTH= GITHUB_OAUTH
CONFIG_PATH= CONFIG_PATH
```
4. Run the main class
5. Profit

### What was used to write this bot?
* JDA: https://github.com/DV8FromTheWorld/JDA
* JDA Chewtils: https://github.com/Chew/JDA-Chewtils (well [chalkyjeans's fork](https://github.com/chalkyjeans/JDA-Chewtils) of it)
* Logback: https://github.com/qos-ch/logback
* dotenv: https://github.com/cdimascio/dotenv-java
* Github-API: https://github.com/hub4j/github-api
#### What does each of those do?
* JDA: The Java API for discord
* JDA Chewtils: A forked fork of JDA Utils for useful implementations
* Logback: Removes some fake errors
* dotenv: For accessing `.env` files that contain the bots sensitive data
* Github-API: The API for github commands. Helps to produce the Github Repo, issue and profile embeds

#### This repo is open to contributions by the community. Please contribute as and where you feel required
