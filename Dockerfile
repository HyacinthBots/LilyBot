FROM openjdk:17-jdk-slim

RUN mkdir /bot
RUN mkdir /data

COPY build/libs/LilyBot-*-all.jar /usr/local/lib/LilyBot.jar
COPY lily.toml /bot/lily.toml

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Also required: GITHUB_OAUTH, JDBC_URL
# Optional: SENTRY_DSN

ENV CONFIG_PATH=/bot/lily.toml
ENV GUILD_ID=774352792659820594

ENV JOIN_CHANNEL=774353381057495061
ENV MESSAGE_LOGS=924785382720172063
ENV MODERATOR_PING_ROLE=899422678199468073
ENV MOD_ACTION_LOG=924785099994726410
ENV SUPPORT_CHANNEL=849360601267830825
ENV SUPPORT_ROLE=849153510028345366

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "/usr/local/lib/LilyBot.jar"]
