/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import kotlinx.coroutines.delay
import net.irisshaders.lilybot.database.DatabaseManager
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Suppress("DuplicatedCode")
class ThreadModInviter : Extension() {
	override val name = "moderatingthreads"

	override suspend fun setup() {
		event<ThreadChannelCreateEvent> {
			check { failIf(event.channel.ownerId == kord.selfId) }
			check { failIf(event.channel.member != null) } // To avoid running on thread join, rather than creation only

			action {
				var supportError = false
				var supportChannel: String? = null
				var supportTeamId: String? = null
				newSuspendedTransaction {
					try {
						supportChannel = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq event.channel.guild.id.toString()
						}.single()[DatabaseManager.Config.supportChanel]

						supportTeamId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq event.channel.guild.id.toString()
						}.single()[DatabaseManager.Config.supportTeam]

					} catch (e: NoSuchElementException) {
						supportError = true
					}
				}
				var moderatorRoleError = false
				var moderatorRole: String? = null
				newSuspendedTransaction {
					try {
						moderatorRole = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq event.channel.guild.id.toString()
						}.single()[DatabaseManager.Config.moderatorsPing]
					} catch (e: NoSuchElementException) {
						moderatorRoleError = true
					}
				}

				if (!supportError) {
					if (event.channel.parentId == Snowflake(supportChannel!!.toLong())) {
						val threadOwner = event.channel.owner.asUser()

						sentry.breadcrumb(BreadcrumbType.Info) {
							category = "extensions.util.threadmodinviter.ThreadCreation"
							message =
								"A thread was created by ${threadOwner.tag} in the support channel, not using the" +
										"automatic system >:("
						}

						val supportRole = event.channel.guild.getRole(Snowflake(supportTeamId!!.toString()))

						event.channel.withTyping {
							delay(2.seconds)
						}

						val message = event.channel.createMessage(
							content = "Hello there! Cool thread, since you're in the support channel, I'll just grab" +
									" support team for ya"
						)

						event.channel.withTyping {
							delay(4.seconds)
						}

						message.edit {
							content = "Oi, ${supportRole.mention}, get here and help this person!"
						}

						event.channel.withTyping {
							delay(3.seconds)
						}

						message.edit {
							content =
								"Welcome to your support thread, ${threadOwner.mention}, here is your thread :D\nNext" +
										" time though, you can just send a message in <#$supportChannel> and I'll automatically" +
										" make a thread for you ;D"
						}
					}
				} else if (!moderatorRoleError) {
					if (event.channel.parentId != Snowflake(supportChannel!!.toLong())) {
						val threadOwner = event.channel.owner.asUser()

						sentry.breadcrumb(BreadcrumbType.Info) {
							category = "extensions.util.threadmodinviter.ThreadCreation"
							message = "A thread was created by ${threadOwner.tag}"
						}

						val modRole = event.channel.guild.getRole(Snowflake(moderatorRole!!.toLong()))

						event.channel.withTyping {
							delay(2.seconds)
						}
						val message = event.channel.createMessage(
							content = "Hello there! Cool thread, lemme just grab the moderators so they can see how cool" +
									" this is"
						)

						event.channel.withTyping {
							delay(4.seconds)
						}

						message.edit {
							content = "Oi, ${modRole.mention}, get here and see how cool this is!"
						}

						event.channel.withTyping {
							delay(4.seconds)
						}

						message.edit {
							content = "Welcome to your thread, ${threadOwner.mention}, here is your thread :D"
						}

						delay(10.seconds)

						message.delete("Mods have been invited, message can go now!")
					}
				}
			}
		}
	}
}
