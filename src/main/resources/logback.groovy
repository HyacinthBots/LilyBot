import ch.qos.logback.core.joran.spi.ConsoleTarget

// Silence warning about missing native PRNG
logger("io.ktor.util.random", ERROR)

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%boldMagenta(%d{dd-MM-yyyy HH:mm:ss}) %gray(|) %boldCyan(%-30.-30thread) %gray(|) %highlight(%-5level) %gray(|) %boldGreen(%-40.40logger{40}) %gray(|) %msg%n"
    }

    target = ConsoleTarget.SystemErr
}

root(INFO, ["CONSOLE"])
