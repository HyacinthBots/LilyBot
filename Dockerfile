FROM openjdk:21-jdk-slim

RUN mkdir /bot
RUN mkdir /data

COPY build/libs/LilyBot-*-all.jar /usr/local/lib/LilyBot.jar

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Optional: SENTRY_DSN

ENV TEST_GUILD_ID=934324779811483718
ENV ONLINE_STATUS_CHANNEL=1001491767323005008
ENV ENVIRONMENT=production

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2048M", "-Xmx3072M", "-XX:+DisableExplicitGC", "-jar", "/usr/local/lib/LilyBot.jar"]
