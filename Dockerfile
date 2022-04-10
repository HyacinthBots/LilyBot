FROM openjdk:17-jdk-slim

RUN mkdir /bot
RUN mkdir /data

COPY build/libs/LilyBot-*-all.jar /usr/local/lib/LilyBot.jar
COPY commands.toml /bot/commands.toml

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Also required: GITHUB_OAUTH, JDBC_URL
# Optional: SENTRY_DSN

ENV CUSTOM_COMMANDS_PATH=/bot/commands.toml
ENV TEST_GUILD_ID=934324779811483718
ENV ONLINE_STATUS_CHANNEL=941669186533474344

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "/usr/local/lib/LilyBot.jar"]
