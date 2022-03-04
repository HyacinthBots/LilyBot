# LilyBot

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
* [Kord](https://github.com/kordlib/kord), the Kotlin API for Discord. ![GitHub release (latest by date)](https://img.shields.io/github/v/release/kordlib/kord)
* [KordEx](https://github.com/Kord-Extensions/kord-extensions), an integrated commands and extensions framework for Kord. ![GitHub release (latest by date)](https://img.shields.io/github/v/release/Kord-Extensions/kord-extensions)
* KordEx's [Minecraft Mappings](https://github.com/Kord-Extensions/ext-mappings) and [Phishing](https://github.com/Kord-Extensions/kord-extensions/tree/develop/extra-modules/extra-phishing) extensions.
* [JetBrains Exposed](https://github.com/JetBrains/Exposed), [SQLite](https://github.com/xerial/sqlite-jdbc), and [HikariCP](https://github.com/brettwooldridge/HikariCP) to manage the database used for warnings, server configurations, and other small things. ![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.exposed/exposed-core?label=Latest%20maven-central) ![Maven Central](https://img.shields.io/maven-central/v/org.xerial/sqlite-jdbc?label=Latest%20maven-central) ![Maven Central](https://img.shields.io/maven-central/v/com.zaxxer/HikariCP?label=Latest%20maven-central)
* [Groovy](https://www.groovy-lang.org/), allows us to use groovy files for logback, ![Maven Central](https://img.shields.io/maven-central/v/org.apache.groovy/groovy?label=Latest%20maven-central)
* [Logback](https://github.com/qos-ch/logback), a library that makes logging prettier. ![Maven Central](https://img.shields.io/maven-central/v/ch.qos.logback/logback-classic?label=Latest%20maven-central)
* [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging), a lightweight logging that wraps slf4j with kotlin extensions ![Maven Central](https://img.shields.io/maven-central/v/io.github.microutils/kotlin-logging?label=Latest%20maven-central)
* [Github-API](https://github.com/hub4j/github-api), the API utilized by the GitHub commands. ![Maven Central](https://img.shields.io/maven-central/v/org.kohsuke/github-api?label=Latest%20maven-central)
* [TOML](https://github.com/Jezza/toml) library for custom commands ![Maven Central](https://img.shields.io/maven-central/v/com.github.jezza/toml?label=Latest%20maven-central)
* [Shadow Gradle Plugin](https://github.com/johnrengelman/shadow), allows us to make a big fatjar containing all dependencies

#### This repo is open to contributions by the community. Please check our [Contributor Guidelines](https://github.com/IrisShaders/LilyBot/blob/main/CONTRIBUTING.md) before doing so. 

### Contributors
![GitHub contributors](https://img.shields.io/github/contributors/IrisShaders/LilyBot?label=Total%20Contributors)

<a href="https://github.com/IrisShaders/LilyBot/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=IrisShaders/LilyBot" />
</a>
