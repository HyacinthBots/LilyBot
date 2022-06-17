FROM openjdk:17-jdk-slim

RUN mkdir /bot
RUN mkdir /data

COPY build/libs/LilyBot-*-all.jar /usr/local/lib/LilyBot.jar

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Optional: SENTRY_DSN

ENV TEST_GUILD_ID=934324779811483718
ENV ONLINE_STATUS_CHANNEL=941669186533474344
ENV ENVIRONMENT=production

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-XX:+DisableExplicitGC", "-jar", "/usr/local/lib/LilyBot.jar"]
