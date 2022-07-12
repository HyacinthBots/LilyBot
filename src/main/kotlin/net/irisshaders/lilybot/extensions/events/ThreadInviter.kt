/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
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
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.SupportConfigCollection
import net.irisshaders.lilybot.database.collections.ThreadsCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
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
				configPresent(ConfigType.SUPPORT)
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
				val config = SupportConfigCollection().getConfig(event.guildId!!)!!

				if (config.enabled) {
					var userThreadExists = false
					var existingUserThread: TextChannelThread? = null
					val textChannel = event.message.getChannel().asChannelOf<TextChannel>()
					val guild = event.getGuild()
					val supportChannel = guild?.getChannelOf<TextChannel>(config.channel)

					if (textChannel != supportChannel) return@action

					if (event.message.author?.isNullOrBot() == false &&
						PluralKit.isProxied(event.message.id)
					) return@action

					val userId = PluralKit.getProxiedMessageAuthorId(event.message.id) ?: event.member!!.id
					val user = UserBehavior(userId, kord)

					ThreadsCollection().getOwnerThreads(userId).forEach {
						try {
							val thread = guild.getChannel(it.threadId) as TextChannelThread
							if (thread.parent == supportChannel && !thread.isArchived) {
								userThreadExists = true
								existingUserThread = thread
							}
						} catch (e: EntityNotFoundException) {
							ThreadsCollection().removeThread(it.threadId)
						} catch (e: IllegalArgumentException) {
							ThreadsCollection().removeThread(it.threadId)
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

						ThreadsCollection().setThreadOwner(thread.id, userId)

						val editMessage = thread.createMessage("edit message")

						editMessage.edit {
							this.content =
								user.asUser().mention + ", the " + event.getGuild()
									?.getRole(config.team)?.mention + " will be with you shortly!"
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
				} else { return@action }
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
				configPresent(ConfigType.SUPPORT)
			}

			action {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)!!
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)!!
				val modRole = event.channel.guild.getRole(moderationConfig.team)
				val threadOwner = event.channel.owner.asUser()

				ThreadsCollection().setThreadOwner(event.channel.id, threadOwner.id)

				if (supportConfig.enabled && event.channel.parentId == supportConfig.channel) {
					val supportRole = event.channel.guild.getRole(supportConfig.team)

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
								" you can just send a message in <#${event.channel.guild.getChannel(
									supportConfig.channel
								).mention
								}> and I'll automatically make a thread for you!\n\nOnce you're finished, use" +
								" `/thread archive` to close  " +
								" your thread. If you want to change the thread name, use `/thread rename` to do so."
					}
				}

				if (!supportConfig.enabled || event.channel.parentId != supportConfig.channel) {
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
