/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import net.irisshaders.lilybot.utils.DatabaseHelper
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class ThreadInviter : Extension() {
	override val name = "threadinviter"

	override suspend fun setup() {
		/**
		 * Thread inviting system for Support Channels
		 * @author IMS212
		 */
		event<MessageCreateEvent> {
			check { failIf(event.message.type == MessageType.ChatInputCommand) } // Don't try to create if the message is a slash command
			check { failIf(event.message.type == MessageType.ThreadCreated || event.message.type == MessageType.ThreadStarterMessage) } // Don't try and run this if the thread is manually created
			check { failIf(event.message.author?.id == kord.selfId) }

			action {
				if (event.guildId == null) return@action
				// Try to get the moderator ping role from the config. If a config is not set, return@action
				val moderatorRoleId = DatabaseHelper.selectInConfig(event.guildId.toString(), "moderatorsPing")
				if (moderatorRoleId == null) { return@action }

				val supportTeam = DatabaseHelper.selectInConfig(event.guildId!!.toString(), "supportTeam")
				val supportChannel = DatabaseHelper.selectInConfig(event.guildId!!.toString(), "supportChannel")

				var userThreadExists = false
				var existingUserThread: TextChannelThread? = null
				val textChannel = event.message.getChannel() as TextChannel

				if (textChannel != event.getGuild()?.getChannel(Snowflake(supportChannel))) {
					return@action
				}

				//TODO: this is incredibly stupid, there has to be a better way to do this.
				textChannel.activeThreads.collect {
					if (it.name == "Support thread for " + event.member!!.asUser().username) {
						userThreadExists = true
						existingUserThread = it
					}
				}

				if (userThreadExists) {
					val response = event.message.respond {
						content =
							"You already have a thread, please talk about your issue in it. " + existingUserThread!!.mention
					}
					event.message.delete("User already has a thread")
					response.delete(10000L, false)
				} else {
					val thread =
						textChannel.startPublicThreadWithMessage(
							event.message.id,
							"Support thread for " + event.member!!.asUser().username,
							ArchiveDuration.Hour
						)
					val editMessage = thread.createMessage("edit message")

					editMessage.edit {
						this.content =
							event.member!!.asUser().mention + ", the " + event.getGuild()
								?.getRole(Snowflake(supportTeam))?.mention + " will be with you shortly!"
					}

					if (textChannel.messages.last().author?.id == kord.selfId) {
						textChannel.deleteMessage(
							textChannel.messages.last().id,
							"Automatic deletion of thread creation message"
						)
					}

					val response = event.message.reply {
						content = "A thread has been created for you: " + thread.mention
					}
					response.delete(10000L, false)
				}
			}
		}

		/**
		 * System for inviting moderators or support team to threads
		 *
		 * This code was adapted from QuiltMC's [cozy](https://github.com/QuiltMC/cozy-discord)
		 * and hence subject to the terms of the Mozilla Public License V. 2.0
		 * A copy of this license can be found at https://mozilla.org/MPL/2.0/.
		 */
		event<ThreadChannelCreateEvent> {
			check { failIf(event.channel.ownerId == kord.selfId) }
			check { failIf(event.channel.member != null) } // To avoid running on thread join, rather than creation only

			action {
				if (event.channel.guildId == null) return@action
				// Try to get the moderator ping role from the config. If a config is not set, return@action
				val moderatorRoleId = DatabaseHelper.selectInConfig(event.channel.guildId.toString(), "moderatorsPing")
				if (moderatorRoleId == null) { return@action }

				val supportTeam = DatabaseHelper.selectInConfig(event.channel.guildId.toString(), "supportTeam")
				val supportChannel = DatabaseHelper.selectInConfig(event.channel.guildId.toString(), "supportChannel")

				if (supportTeam != null) {
					if (try {
							event.channel.parentId == Snowflake(supportChannel)
						} catch (e: NumberFormatException) {
							false
						}
					) {
						val threadOwner = event.channel.owner.asUser()

						val supportRole = event.channel.guild.getRole(Snowflake(supportTeam))

						event.channel.withTyping {
							delay(2.seconds)
						}

						val message = event.channel.createMessage(
							content = "Hello there! Since you're in the support channel, I'll just grab" +
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

					} else if (moderatorRoleId != null) {
						if (
							try {
								event.channel.parentId != Snowflake(supportChannel)
							} catch (e: NumberFormatException) {
								false
							}
						) {
							val threadOwner = event.channel.owner.asUser()

							val modRole = event.channel.guild.getRole(Snowflake(moderatorRoleId))

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
										"`/thread archive` to close it. If you want to change the thread name, use" +
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
