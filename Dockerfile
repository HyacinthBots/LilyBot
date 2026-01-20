FROM eclipse-temurin:21-jdk

RUN mkdir -p /bot
RUN mkdir -p /data
RUN mkdir -p /dist/out

COPY [ "build/distributions/LilyBot-*.tar", "/dist" ]

RUN tar -xf /dist/LilyBot-*.tar -C /dist/out
RUN chmod +x /dist/out/LilyBot-*/bin/LilyBot

RUN rm /dist/LilyBot*.tar

WORKDIR /bot

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Optional: SENTRY_DSN

ENV TEST_GUILD_ID=934324779811483718
ENV ONLINE_STATUS_CHANNEL=1001491767323005008
ENV ENVIRONMENT=production

WORKDIR /bot

ENTRYPOINT [ "/dist/out/LilyBot-*/bin/LilyBot" ]
