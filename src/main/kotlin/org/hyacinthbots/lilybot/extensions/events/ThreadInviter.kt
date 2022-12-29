/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.NewsChannelThread
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs
import kotlin.time.Duration.Companion.seconds

// This is rewritten in another branch, but said branch should take care of making sure that the target roles are
//  ping able by Lily. Should/Could be done in a similar fashion to the method in extensions/config/Config.kt
class ThreadInviter : Extension() {
	override val name = "thread-inviter"

	// note: the requireConfigs checks in this file are not perfect,
	// but will be fully replaced with the thread inviting rewrite, so it's ok
	override suspend fun setup() {
		/**
		 * Thread inviting system for Support Channels
		 * @author IMS212
		 */
		event<ProxiedMessageCreateEvent> {
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
				requiredConfigs(
					ConfigOptions.SUPPORT_ENABLED,
					ConfigOptions.SUPPORT_CHANNEL,
					ConfigOptions.SUPPORT_ROLE
				)
				failIf {
					event.message.type == MessageType.ChatInputCommand ||
							event.message.type == MessageType.ThreadCreated ||
							event.message.type == MessageType.ThreadStarterMessage ||
							event.message.author?.id == kord.selfId ||
							// Make use of getChannelOrNull here because the channel "may not exist". This is to help
							// fix an issue with the new ViT channels in Discord.
							event.message.getChannel().type == ChannelType.GuildNews ||
							event.message.getChannel().type == ChannelType.GuildVoice ||
							event.message.getChannel().type == ChannelType.PublicGuildThread ||
							event.message.getChannel().type == ChannelType.PublicNewsThread
				}
			}
			action {
				val config = SupportConfigCollection().getConfig(event.getGuild().id)!!

				config.role ?: return@action
				config.channel ?: return@action

				val guild = event.getGuildOrNull() ?: return@action
				var userThreadExists = false
				var existingUserThread: TextChannelThread? = null
				val textChannel: TextChannel = guild.getChannelOfOrNull(event.pkMessage.channel) ?: return@action

				val supportChannel = getLoggingChannelWithPerms(ConfigOptions.SUPPORT_CHANNEL, guild) ?: return@action

				if (textChannel != supportChannel) return@action

				val userId = event.pkMessage.sender
				val user = UserBehavior(userId, kord)

				ThreadsCollection().getOwnerThreads(userId).forEach {
					try {
						val thread = guild.getChannelOfOrNull<TextChannelThread>(it.threadId)
						if (thread?.parent == supportChannel && !thread.isArchived) {
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
					val response = textChannel.createMessage {
						content = "${user.mention} You already have a thread, please talk about your issue in it.\n" +
								existingUserThread!!.mention
					}
					textChannel.getMessageOrNull(event.pkMessage.id)?.delete()
					response.delete(10.seconds.inWholeMilliseconds, false)
				} else {
					val thread =
						textChannel.startPublicThreadWithMessage(
							event.pkMessage.id,
							"Support thread for ${
								event.pkMessage.member?.name ?: event.kord.getUser(event.pkMessage.sender)?.username
							}",
							event.message.getChannel().data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
						)

					ThreadsCollection().setThreadOwner(guild.id, thread.parentId, thread.id)

					val startMessage =
						thread.createMessage("Welcome to your support thread! Let me grab the support team...")
					delay(2.seconds)

					startMessage.edit {
						content =
							"${user.asUser().mention}, the ${
								event.getGuildOrNull()?.getRole(config.role)?.mention
							} will be with you shortly!"
					}

					if (textChannel.messages.last().author?.id == kord.selfId) {
						textChannel.deleteMessage(
							textChannel.messages.last().id
						)
					}

					val response = textChannel.createMessage {
						content = "${user.mention} Your thread has been created for you:" +
								thread.mention
					}

					response.delete(10.seconds.inWholeMilliseconds, false)
				}
			}
		}

		event<UnProxiedMessageCreateEvent> {
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
				requiredConfigs(
					ConfigOptions.SUPPORT_ENABLED,
					ConfigOptions.SUPPORT_CHANNEL,
					ConfigOptions.SUPPORT_ROLE
				)
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
				val config = SupportConfigCollection().getConfig(event.guildId!!)!!

				if (!config.enabled) {
					return@action
				}

				var userThreadExists = false
				var existingUserThread: TextChannelThread? = null
				val textChannel = event.message.getChannel().asChannelOfOrNull<TextChannel>()
				val guild = event.getGuildOrNull() ?: return@action

				val supportChannel = getLoggingChannelWithPerms(ConfigOptions.SUPPORT_CHANNEL, guild) ?: return@action

				if (textChannel != supportChannel) return@action

				val userId = event.getUser().id
				val user = UserBehavior(userId, kord)

				ThreadsCollection().getOwnerThreads(userId).forEach {
					try {
						val thread = guild.getChannelOfOrNull<TextChannelThread>(it.threadId)
						if (thread?.parent == supportChannel && !thread.isArchived) {
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
					event.message.delete()
					response.delete(10.seconds.inWholeMilliseconds, false)
				} else {
					val thread =
					// Create a thread with the message sent, title it with the users tag and set the archive
						// duration to the channels settings. If they're null, set it to one day
						textChannel.startPublicThreadWithMessage(
							event.message.id,
							"Support thread for " + user.asUser().username,
							event.message.getChannel().data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
						)

					ThreadsCollection().setThreadOwner(guild.id, thread.parentId, thread.id)

					val startMessage =
						thread.createMessage("Welcome to your support thread! Let me grab the support team...")

					startMessage.edit {
						content = if (config.message.isNullOrEmpty()) {
							"${user.asUser().mention}, the ${
								event.getGuildOrNull()
									?.getRole(config.role!!)?.mention
							} will be with you shortly!"
						} else {
							"${user.asUser().mention} ${
								event.getGuildOrNull()?.getRole(config.role!!)?.mention
							}\n${config.message}"
						}
					}

					if (textChannel.messages.last().author?.id == kord.selfId) {
						textChannel.deleteMessage(
							textChannel.messages.last().id,
						)
					}

					val response = event.message.reply {
						content = "A thread has been created for you: " + thread.mention
					}
					response.delete(10.seconds.inWholeMilliseconds, false)
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
			}

			action {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				val threadOwner = event.channel.owner.asUser()

				ThreadsCollection().setThreadOwner(
					event.channel.guildId,
					event.channel.parentId,
					event.channel.id
				)

				if (supportConfig != null && supportConfig.enabled && event.channel.parentId == supportConfig.channel) {
					val supportRole = event.channel.guild.getRole(supportConfig.role!!)

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
								" you can just send a message in ${
									event.channel.guild.getChannel(
										supportConfig.channel
									).mention
								} and I'll automatically make a thread for you!\n\nOnce you're finished, use" +
								" `/thread archive` to close  " +
								" your thread. If you want to change the thread name, use `/thread rename` to do so."
					}
				}

				if (moderationConfig != null || supportConfig == null ||
					!supportConfig.enabled || event.channel.parentId != supportConfig.channel
				) {
					val modRole = event.channel.guild.getRole(moderationConfig?.role!!)
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
				}
			}
		}
	}
}
