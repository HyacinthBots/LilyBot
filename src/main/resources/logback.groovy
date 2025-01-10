/*
* This code was utilized from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
* and hence is subject to the terms of the Mozilla Public License V. 2.0
* A copy of this license can be found at https://mozilla.org/MPL/2.0/.
*/

// I'd love to optimise imports but these are actually used, shush idea
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.joran.spi.ConsoleTarget
import java.util.ArrayList
import org.hyacinthbots.lilybot.utils.DiscordLogAppender

def environment = System.getenv().getOrDefault("ENVIRONMENT", "production")
def logUrl = System.getenv().getOrDefault("DISCORD_LOGGER_URL", null)

def defaultLevel = TRACE
def defaultTarget = ConsoleTarget.SystemOut

if (environment == "development") {
    defaultLevel = DEBUG
    defaultTarget = ConsoleTarget.SystemErr

    // Silence warning about missing native PRNG
    logger("io.ktor.util.random", ERROR)
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%boldMagenta(%d{dd-MM-yyyy HH:mm:ss}) %gray(|) %boldCyan(%-30.-30thread) %gray(|) %highlight(%-5level) %gray(|) %boldGreen(%-40.40logger{40}) %gray(|) %msg%n"

        withJansi = true
    }

    target = defaultTarget
}

def loggers = ["CONSOLE"]

if (logUrl != null) {
    appender("DISCORD_WEBHOOK", DiscordLogAppender) {
        level = WARN
        url = logUrl
    }

    loggers << "DISCORD_WEBHOOK"
}

root(defaultLevel, loggers)
