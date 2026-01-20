/*
* This code was utilized from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
* and hence is subject to the terms of the Mozilla Public License V. 2.0
* A copy of this license can be found at https://mozilla.org/MPL/2.0/.
*/

@file:OptIn(KordUnsafe::class, KordExperimental::class)

package org.hyacinthbots.lilybot.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import dev.kord.common.Color
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.execute
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_WHITE
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.koin.KordExKoinComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import kotlin.time.Instant

class DiscordLogAppender : AppenderBase<ILoggingEvent>(), KordExKoinComponent {
	lateinit var url: String
	var level: Level = Level.ALL

	private val webhookId: Snowflake by lazy {
		val parts = url.split("/").toMutableList()

		parts.removeLast()
		Snowflake(parts.removeLast())
	}

	private val webhookToken: String by lazy {
		val parts = url.split("/").toMutableList()
		parts.removeLast()
	}

	private val webhook: WebhookBehavior by lazy {
		kord.unsafe.webhook(webhookId)
	}

	private val logger = KotlinLogging.logger("org.hyacinthbots.utils.DiscordLogAppender")
	private val kord: Kord by inject()

	@Suppress("TooGenericExceptionCaught")
	override fun append(eventObject: ILoggingEvent) {
		if (!eventObject.level.isGreaterOrEqual(level)) {
			return
		}

		kord.launch {
			try {
				webhook.execute(webhookToken) {
					if (eventObject.level.levelInt == Level.ERROR_INT && DEV_ID != null) {
						// Ping NoComment if there is an error
						content = "<@$DEV_ID>"
					}
					embed {
						description = eventObject.message
						timestamp = Instant.fromEpochMilliseconds(eventObject.timeStamp)
						title = "Log message: ${eventObject.level.levelStr}"

						color = when (eventObject.level.levelInt) {
							Level.ERROR_INT -> DISCORD_RED
							Level.WARN_INT -> DISCORD_YELLOW
							Level.INFO_INT -> DISCORD_BLURPLE
							Level.DEBUG_INT -> DISCORD_WHITE
							Level.TRACE_INT -> DISCORD_BLACK

							else -> Color(0, 0, 0)
						}

						field {
							name = "Logger"
							value = "`${eventObject.loggerName}`"
						}

						field {
							name = "Thread name"
							value = "`${eventObject.threadName}`"
						}
					}
				}
			} catch (e: Exception) {
				logger.error(e) { "Failed to log message to Discord." }
			}
		}
	}
}
