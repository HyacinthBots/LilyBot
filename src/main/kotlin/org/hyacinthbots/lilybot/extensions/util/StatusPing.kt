package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.entity.PresenceStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import mu.KotlinLogging
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import kotlin.time.Duration.Companion.seconds

class StatusPing : Extension() {
	override val name = "status-ping"

	private val scheduler = Scheduler()

	private lateinit var task: Task

	private val client = HttpClient {}

	private val env = envOrNull("STATUS_URL")

	private val logger = KotlinLogging.logger("Status ping")

	override suspend fun setup() {
		task = scheduler.schedule(30.seconds, repeat = true, callback = ::post)
	}

	private suspend fun post() {
		if (kord.getSelf().asMemberOrNull(TEST_GUILD_ID)
				?.getPresenceOrNull()?.status == PresenceStatus.Online && env != null
		) {
			logger.debug { "Pinging!" }
			client.post(env)
		}
	}
}
