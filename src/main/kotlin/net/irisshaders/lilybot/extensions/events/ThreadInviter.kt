/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.NewsChannelThread
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import net.irisshaders.lilybot.api.pluralkit.PK_API_DELAY
import net.irisshaders.lilybot.api.pluralkit.PluralKit
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent
import kotlin.time.Duration.Companion.seconds

class ThreadInviter : Extension() {
	override val name = "thread-inviter"

	override suspend fun setup() {
		/**
		 * Thread inviting system for Support Channels
		 * @author IMS212
		 */
		event<MessageCreateEvent> {
			/*
			Don't try to create a thread if
			 - the message is in DMs
			 - a config isn't set
			 - the message is a slash command
			 - the thread was manually created or the message is already in a thread
			 - the message was sent by Lily or another bot
			 - the message is in an announcements channel
			 */
			check {
				anyGuild()
				configPresent()
				failIf {
					event.message.type == MessageType.ChatInputCommand ||
							event.message.type == MessageType.ThreadCreated ||
							event.message.type == MessageType.ThreadStarterMessage ||
							event.message.author?.id == kord.selfId ||
							// Make use of getChannelOrNull here because the channel "may not exist". This is to help
							// fix an issue with the new ViT channels in Discord.
							event.message.getChannelOrNull() is TextChannelThread ||
							event.message.getChannelOrNull() is NewsChannel ||
							event.message.getChannelOrNull() is NewsChannelThread
				}
			}
			action {
				delay(PK_API_DELAY) // Allow the PK API to catch up
				val config = DatabaseHelper.getConfig(event.guildId!!)!!

				config.supportTeam ?: return@action
				config.supportChannel ?: return@action

				var userThreadExists = false
				var existingUserThread: TextChannelThread? = null
				val textChannel = event.message.getChannel().asChannelOf<TextChannel>()
				val guild = event.getGuild()
				val supportChannel = guild?.getChannelOf<TextChannel>(config.supportChannel)

				if (textChannel != supportChannel) return@action

				if (event.message.author?.isNullOrBot() == false &&
					PluralKit.isProxied(event.message.id)
				) return@action

				val userId = PluralKit.getProxiedMessageAuthorId(event.message.id) ?: event.member!!.id
				val user = UserBehavior(userId, kord)

				DatabaseHelper.getOwnerThreads(userId).forEach {
					try {
						val thread = guild.getChannel(it.threadId) as TextChannelThread
						if (thread.parent == supportChannel && !thread.isArchived) {
							userThreadExists = true
							existingUserThread = thread
						}
					} catch (e: EntityNotFoundException) {
						DatabaseHelper.deleteThread(it.threadId)
					} catch (e: IllegalArgumentException) {
						DatabaseHelper.deleteThread(it.threadId)
					}
				}

				if (userThreadExists) {
					val response = event.message.respond {
						content =
							"You already have a thread, please talk about your issue in it. " +
									existingUserThread!!.mention
					}
					event.message.delete("User already has a thread")
					response.delete(10000L, false)
				} else {
					val thread =
					// Create a thread with the message sent, title it with the users tag and set the archive
						// duration to the channels settings. If they're null, set it to one day
						textChannel.startPublicThreadWithMessage(
							event.message.id,
							"Support thread for " + user.asUser().username,
							event.message.getChannel().data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
						)

					DatabaseHelper.setThreadOwner(thread.id, userId)

					val threadMessageData = DatabaseHelper.getThreadMessageData(guild.id)

					if (threadMessageData != null) {
						thread.createMessage(
							threadMessageData.message.replace("\\n", "\n")
								.replace("@user", event.member!!.asUser().mention)
						)
					} else {
						thread.createMessage(
							user.asUser().mention + ", the " + event.getGuild()
								?.getRole(config.supportTeam)?.mention + " will be with you shortly!"
						)
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
		 * This code was adapted from [cozy](https://github.com/QuiltMC/cozy-discord) by QuiltMC
		 * and hence is subject to the terms of the Mozilla Public License V. 2.0
		 * A copy of this license can be found at https://mozilla.org/MPL/2.0/.
		 */
		event<ThreadChannelCreateEvent> {
			check {
				failIf {
					event.channel.ownerId == kord.selfId ||
							event.channel.member != null
				}
				configPresent()
			}

			action {
				val config = DatabaseHelper.getConfig(event.channel.guildId)!!
				val modRole = event.channel.guild.getRole(config.moderatorsPing)
				val threadOwner = event.channel.owner.asUser()

				DatabaseHelper.setThreadOwner(event.channel.id, threadOwner.id)

				var supportConfigSet = true
				if (config.supportTeam == null || config.supportChannel == null) {
					supportConfigSet = false
				}

				if (supportConfigSet && event.channel.parentId == config.supportChannel) {
					val supportRole = event.channel.guild.getRole(config.supportTeam!!)

					event.channel.withTyping { delay(2.seconds) }
					val message = event.channel.createMessage(
						content = "Hello there! Since you're in the support channel, I'll just grab the support" +
								" team for you..."
					)

					event.channel.withTyping { delay(4.seconds) }
					message.edit { content = "${supportRole.mention}, please help this person!" }

					event.channel.withTyping { delay(3.seconds) }
					message.edit {
						content = "Welcome to your support thread, ${threadOwner.mention}\nNext time though," +
								" you can just send a message in <#${config.supportChannel}> and I'll automatically" +
								" make a thread for you!\n\nOnce you're finished, use `/thread archive` to close" +
								" your thread. If you want to change the thread name, use `/thread rename`" +
								" to do so."
					}
				}

				if (!supportConfigSet || event.channel.parentId != config.supportChannel) {
					event.channel.withTyping { delay(2.seconds) }
					val message = event.channel.createMessage(
						content = "Hello there! Lemme just grab the moderators..."
					)

					event.channel.withTyping { delay(4.seconds) }
					message.edit { content = "${modRole.mention}, welcome to the thread!" }

					event.channel.withTyping { delay(4.seconds) }
					message.edit {
						content = "Welcome to your thread, ${threadOwner.mention}\nOnce you're finished, use" +
								" `/thread archive` to close it. If you want to change the thread name, use" +
								" `/thread rename` to do so."
					}

					delay(20.seconds)
					message.delete("Mods have been invited, message can go now!")
				}
			}
		}
	}
}
