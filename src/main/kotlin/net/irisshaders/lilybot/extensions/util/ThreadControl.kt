/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.channel.thread.ThreadUpdateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.ThreadsCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent

class ThreadControl : Extension() {

	override val name = "thread-control"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "thread"
			description = "The parent command for all /thread commands"

			ephemeralSubCommand(::ThreadRenameArgs) {
				name = "rename"
				description = "Rename a thread!"

				check {
					isInThread()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)
					if (!ownsThreadOrModerator(threadChannel, member)) return@action

					threadChannel.edit {
						name = arguments.newThreadName
						reason = "Renamed by ${member.tag}"
					}

					respond {
						content = "Thread Renamed!"
					}
				}
			}

			ephemeralSubCommand(::ThreadArchiveArgs) {
				name = "archive"
				description = "Archive this thread"

				check {
					isInThread()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)
					if (!ownsThreadOrModerator(threadChannel, member)) return@action

					ThreadsCollection().getAllThreads().forEach {
						if (it.threadId == threadChannel.id) {
							val preventingArchiving = ThreadsCollection().getThread(it.threadId)?.preventArchiving
							ThreadsCollection().removeThread(it.threadId)
							ThreadsCollection().setThreadOwner(it.threadId, it.ownerId, false)
							guild!!.getChannelOf<GuildMessageChannel>(
								ModerationConfigCollection().getConfig(guild!!.id)!!.channel
							).createEmbed {
									title = "Thread archive prevention disabled"
									if (preventingArchiving == true) {
										description =
											"Archive prevention has been disabled, as `/thread archive` was used."
									}
									color = DISCORD_FUCHSIA

									field {
										name = "User"
										value = user.asUser().tag
									}
									field {
										name = "Thread"
										value = threadChannel.mention
									}
								}
						}
					}

					if (threadChannel.isArchived) {
						edit { content = "**Error:** This channel is already archived!" }
						return@action
					}

					threadChannel.edit {
						this.archived = true
						this.locked = arguments.lock && member.hasPermission(Permission.ModerateMembers)

						reason = "Archived by ${user.asUser().tag}"
					}

					respond {
						content = "Thread archived"
						if (arguments.lock && member.hasPermission(Permission.ModerateMembers)) {
							content += " and locked"
						}
						content += "!"
					}
				}
			}

			ephemeralSubCommand(::ThreadTransferArgs) {
				name = "transfer"
				description = "Transfer ownership of this thread"

				check {
					isInThread()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)

					val oldOwnerId = ThreadsCollection().getThread(threadChannel.id)?.ownerId ?: threadChannel.ownerId
					val oldOwner = guild!!.getMember(oldOwnerId)

					if (!ownsThreadOrModerator(threadChannel, member)) return@action

					if (arguments.newOwner.id == oldOwnerId) {
						respond { content = "That person already owns the thread!" }
						return@action
					}

					if (arguments.newOwner.isBot) {
						respond { content = "You cannot transfer ownership of a thread to a bot." }
						return@action
					}

					ThreadsCollection().setThreadOwner(threadChannel.id, arguments.newOwner.id)

					respond { content = "Ownership transferred." }

					var content = "Thread ownership transferred from ${oldOwner.mention} " +
							"to ${arguments.newOwner.mention}."

					if (member != oldOwner) content += " Transferred by ${member.mention}"

					threadChannel.createMessage(content)
				}
			}

			ephemeralSubCommand {
				name = "prevent-archiving"
				description = "Stop a thread from being archived"

				check {
					isInThread()
					configPresent(ConfigType.MODERATION)
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)
					if (!ownsThreadOrModerator(threadChannel, member)) return@action

					if (threadChannel.isArchived) {
						threadChannel.edit {
							archived = false
							reason = "`/thread prevent-archiving` run by ${member.tag}"
						}
					}

					val threads = ThreadsCollection().getAllThreads()
					var message: EphemeralMessageInteractionResponse? = null
					threads.forEach {
						if (it.threadId == threadChannel.id && it.preventArchiving) {
							message = edit {
								content = "Thread archiving is already being prevented, would you like to remove this?"
							}.edit {
								components {
									ephemeralButton {
										label = "Yes"
										style = ButtonStyle.Primary

										action {
											ThreadsCollection().setThreadOwner(it.threadId, it.ownerId, false)
											edit { content = "Thread archiving will no longer be prevented" }
											guild!!.getChannelOf<GuildMessageChannel>(config.channel)
												.createMessage {
													embed {
														title = "Thread archive prevention disabled"
														color = DISCORD_FUCHSIA

														field {
															name = "User"
															value = user.asUser().tag
														}
														field {
															name = "Thread"
															value = threadChannel.mention
														}
													}
												}
											message!!.edit { components { removeAll() } }
										}
									}
									ephemeralButton {
										label = "No"
										style = ButtonStyle.Secondary

										action {
											edit { content = "Thread archiving will remain prevented" }
											message!!.edit { components { removeAll() } }
										}
									}
								}
							}
							return@action
						} else if (it.threadId == threadChannel.id && !it.preventArchiving) {
							ThreadsCollection().setThreadOwner(it.threadId, it.ownerId, true)
							try {
								guild!!.getChannelOf<GuildMessageChannel>(config.channel).createMessage {
									embed {
										title = "Thread archive prevention enabled"
										color = DISCORD_FUCHSIA

										field {
											name = "User"
											value = user.asUser().tag
										}
										field {
											name = "Thread"
											value = threadChannel.mention
										}
									}
								}
								edit { content = "Thread archiving will now be prevented" }
							} catch (e: EntityNotFoundException) {
								edit {
									content = "Thread archiving will now be prevented\nNote: Failed to send a log" +
											"to your specified mod action log. Please check the channel exists and " +
											"permissions are right"
								}
							}
						}
					}
				}
			}
		}

		event<ThreadUpdateEvent> {
			action {
				val channel = event.channel
				val ownedThread = ThreadsCollection().getThread(channel.id)

				if (channel.isArchived && ownedThread != null && ownedThread.preventArchiving) {
					channel.edit {
						archived = false
						reason = "Preventing thread from being archived."
					}
				}
			}
		}
	}

	inner class ThreadRenameArgs : Arguments() {
		/** The new name for the thread. */
		val newThreadName by string {
			name = "newName"
			description = "The new name to give to the thread"
		}
	}

	inner class ThreadArchiveArgs : Arguments() {
		/** Whether to lock the thread or not. */
		val lock by defaultingBoolean {
			name = "lock"
			description = "Whether to lock the thread if you are a moderator. Default is false"
			defaultValue = false
		}
	}

	inner class ThreadTransferArgs : Arguments() {
		/** The new thread owner. */
		val newOwner by member {
			name = "newOwner"
			description = "The user you want to transfer ownership of the thread to"
		}
	}

	/**
	 * Run a check to see if the provided [Member] owns this [ThreadChannel].
	 *
	 * @param inputThread The thread being checked
	 * @param inputMember The Member to check
	 * @return [Boolean]. whether the [inputMember] owns the [inputThread]
	 * @author tempest15
	 * @since 3.2.0
	 */
	private suspend fun EphemeralSlashCommandContext<*>.ownsThreadOrModerator(
		inputThread: ThreadChannel,
		inputMember: Member
	): Boolean {
		val databaseThreadOwner = ThreadsCollection().getThread(inputThread.id)?.ownerId

		if (inputMember.hasPermission(Permission.ModerateMembers) || databaseThreadOwner == inputMember.id) {
			return true
		}

		respond { content = "**Error:** This is not your thread!" }
		return false
	}
}
