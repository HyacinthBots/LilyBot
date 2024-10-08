package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.hyacinthbots.lilybot.utils.ENV
import kotlin.time.Duration.Companion.seconds

class StatusPing : Extension() {
	override val name = "status-ping"

	private val scheduler = Scheduler()

	private lateinit var task: Task

	private val client = HttpClient {}

	private val logger = KotlinLogging.logger("Status ping")

	override suspend fun setup() {
		if (ENV != null) {
			task = scheduler.schedule(30.seconds, repeat = true, callback = ::post)
		}
	}

	private suspend fun post() {
		logger.debug { "Pinging!" }
		if (ENV != null) {
			client.get(ENV)
		}
	}
}
