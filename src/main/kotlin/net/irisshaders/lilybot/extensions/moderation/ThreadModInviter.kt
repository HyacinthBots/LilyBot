/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
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
					if (try {
							event.channel.parentId == Snowflake(supportChannel!!)
						} catch (e: NumberFormatException) {
							false
						}
					) {
						val threadOwner = event.channel.owner.asUser()

						val supportRole = event.channel.guild.getRole(Snowflake(supportTeamId!!))

						event.channel.withTyping {
							delay(2.seconds)
						}

						val message = event.channel.createMessage(
							content = "Hello there! Cool thread, since you're in the support channel, I'll just grab" +
									" support team for you..."
						)

						event.channel.withTyping {
							delay(4.seconds)
						}

						message.edit {
							content = "${supportRole.mention}, could you help this person please!"
						}

						event.channel.withTyping {
							delay(3.seconds)
						}

						message.edit {
							content = "Welcome to your support thread, ${threadOwner.mention}\nNext time though," +
									" you can just send a message in <#$supportChannel> and I'll automatically make a" +
									" thread for you"
						}

					} else if (!moderatorRoleError) {
						if (
							try {
								event.channel.parentId != Snowflake(supportChannel!!)
							} catch (e: NumberFormatException) {
								false
							}
						) {
							val threadOwner = event.channel.owner.asUser()

							val modRole = event.channel.guild.getRole(Snowflake(moderatorRole!!))

							event.channel.withTyping {
								delay(2.seconds)
							}
							val message = event.channel.createMessage(
								content = "Hello there! Lemme just grab the moderators..."
							)

							event.channel.withTyping {
								delay(4.seconds)
							}

							message.edit {
								content = "${modRole.mention}, welcome to the thread!"
							}

							event.channel.withTyping {
								delay(4.seconds)
							}

							message.edit {
								content = "Welcome to your thread, ${threadOwner.mention}\nOnce you're finished, use" +
										"`/thread archive` to close it. If you want to change the threads name, use" +
										"`/thread rename` to do so"
							}

							delay(20.seconds)

							message.delete("Mods have been invited, message can go now!")
						}
					}
				}
			}
		}
	}
}
