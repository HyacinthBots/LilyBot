# LilyBot
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/IrisShaders/LilyBot/Build%20Only?label=Build%20Only) ![GitHub Workflow Status](https://img.shields.io/github/workflow/status/IrisShaders/LilyBot/Build%20&%20Deploy?label=Build%20and%20Deploy)

![GitHub issues](https://img.shields.io/github/issues/IrisShaders/LilyBot?label=Issues)
![GitHub pull requests](https://img.shields.io/github/issues-pr/IrisShaders/LilyBot?label=Pull%20Requests) 

![GitHub](https://img.shields.io/github/license/IrisShaders/LilyBot?label=License) ![GitHub repo size](https://img.shields.io/github/repo-size/IrisShaders/LilyBot?label=Repository%20Size)

![GitHub commits since latest release](https://img.shields.io/github/commits-since/IrisShaders/LilyBot/latest/main?include_prereleases) ![GitHub commit activity](https://img.shields.io/github/commit-activity/w/IrisShaders/LilyBot/main?label=Commit%20Activity%20-%20Main) ![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/w/IrisShaders/LilyBot/develop?label=Commit%20Activity%20-%20Develop)
## Links
* **Visit [our website](https://irisshaders.net) for downloads and pretty screenshots!**
* Visit [our Discord server](https://discord.gg/jQJnav2jPu) to chat about Iris projects (such as LilyBot) and get support!
* Visit [Coderbot's Patreon page](https://www.patreon.com/coderbot) to support the continued development of Iris projects!

### Why?
* Iris had a need for a utility and moderation bot in our Discord. Available bots were largely closed source, and having an in-house bot was preferable anyway. Hence, LilyBot!

### History
* Late in August 2021, Discord.py, the library that our previous bot utilized, was discontinued. So [NoComment](https://github.com/NoComment1105), [Miss Corruption](https://github.com/Miss-Corruption), [Maxigator](https://github.com/Maxigator) and [chalkyjeans](https://github.com/chalkyjeans) took on the task of rewriting the bot in JDA.
* Then with need for features that JDA did not provide, another rewrite occurred! This time [NoComment](https://github.com/NoComment1105), [Miss Corruption](https://github.com/Miss-Corruption), [Maximum](https://github.com/maximumpower55) and [IMS](https://github.com/IMS212), began rewriting in Kotlin, using the [Kord API](https://github.com/kordlib/kord) and [KordEx Extension Library](https://github.com/Kord-Extensions/kord-extensions)
* In early 2022, we decided to expand the scope of LilyBot to be usable in other servers.

### How can I use LilyBot myself!?
Follow our in-depth [installation guide](https://github.com/IrisShaders/LilyBot/blob/main/docs/installation-guide.md).

### What was used to write this bot and what does each thing do?
* [Kord](https://github.com/kordlib/kord), the Kotlin API for Discord.
* [KordEx](https://github.com/Kord-Extensions/kord-extensions), an integrated commands and extensions framework for Kord. 
* KordEx's [Minecraft Mappings](https://github.com/Kord-Extensions/ext-mappings) and [Phishing](https://github.com/Kord-Extensions/kord-extensions/tree/develop/extra-modules/extra-phishing) extensions.
* [JetBrains Exposed](https://github.com/JetBrains/Exposed), [SQLite](https://github.com/xerial/sqlite-jdbc), and [HikariCP](https://github.com/brettwooldridge/HikariCP) to manage the database used for warnings, server configurations, and other small things.
* [Groovy](https://www.groovy-lang.org/), allows us to use groovy files for logback, 
* [Logback](https://github.com/qos-ch/logback), a library that makes logging prettier.
* [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging), a lightweight logging that wraps slf4j with kotlin extensions
* [Github-API](https://github.com/hub4j/github-api), the API utilized by the GitHub commands. 
* [TOML](https://github.com/Jezza/toml) library for custom commands 
* [Shadow Gradle Plugin](https://github.com/johnrengelman/shadow), allows us to make a big fatjar containing all dependencies

#### This repo is open to contributions by the community. Please check our [Contributor Guidelines](https://github.com/IrisShaders/LilyBot/blob/main/CONTRIBUTING.md) before doing so. 

### Contributors
![GitHub contributors](https://img.shields.io/github/contributors/IrisShaders/LilyBot?label=Total%20Contributors)

<a href="https://github.com/IrisShaders/LilyBot/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=IrisShaders/LilyBot" />
</a>
