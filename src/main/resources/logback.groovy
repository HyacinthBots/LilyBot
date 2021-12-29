import ch.qos.logback.core.joran.spi.ConsoleTarget

def environment = System.getenv().getOrDefault("ENVIRONMENT", "production")
def defaultLevel = INFO

if (environment == "debug") {
    defaultLevel = DEBUG

    // Silence warning about missing native PRNG on Windows
    logger("io.ktor.util.random", ERROR)
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{dd-MM-yyyy HH:mm:ss:SSS Z}| %boldCyan(%-30.-30thread) | %highlight(%-6level) | %boldGreen(%-35.35logger{0}) | %msg%n"
    }

    target = ConsoleTarget.SystemErr
}

root(defaultLevel, ["CONSOLE"])