FROM eclipse-temurin:21-jdk

RUN mkdir -p /bot
RUN mkdir -p /data
RUN mkdir -p /dist/out

COPY [ "build/distributions/LilyBot-5.0.0.tar", "/dist" ]

RUN tar -xf /dist/LilyBot-5.0.0.tar -C /dist/out
RUN chmod +x /dist/out/LilyBot-5.0.0/bin/LilyBot

RUN rm /dist/LilyBot-5.0.0.tar

WORKDIR /bot

# Only place env vars below that are fine to be publicised. Private stuff needs to be
# applied deployment-side.
# Optional: SENTRY_DSN

ENV TEST_GUILD_ID=934324779811483718
ENV ONLINE_STATUS_CHANNEL=1001491767323005008
ENV ENVIRONMENT=production

WORKDIR /bot

ENTRYPOINT [ "/dist/out/LilyBot-5.0.0/bin/LilyBot" ]
