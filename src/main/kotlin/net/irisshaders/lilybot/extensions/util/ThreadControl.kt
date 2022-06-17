/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.channel.thread.ThreadUpdateEvent
import dev.kord.rest.builder.message.create.embed
import net.irisshaders.lilybot.database.DatabaseGetters
import net.irisshaders.lilybot.database.DatabaseSetters
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
					configPresent()
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
					configPresent()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)
					if (!ownsThreadOrModerator(threadChannel, member)) return@action

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
					configPresent()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)

					val oldOwnerId = DatabaseGetters.getThread(threadChannel.id)?.ownerId ?: threadChannel.ownerId
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

					DatabaseSetters.setThreadOwner(threadChannel.id, arguments.newOwner.id)

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
					anyGuild()
					isInThread()
					configPresent()
					requireBotPermissions(Permission.ManageThreads)
					botHasChannelPerms(Permissions(Permission.ManageThreads))
				}

				action {
					val config = DatabaseGetters.getConfig(guild!!.id)!!
					val threadChannel = channel.asChannelOf<ThreadChannel>()
					val member = user.asMember(guild!!.id)
					if (!ownsThreadOrModerator(threadChannel, member)) return@action

					if (threadChannel.isArchived) {
						threadChannel.edit {
							archived = false
							reason = "`/thread prevent-archiving` run by ${member.tag}"
						}
					}

					val threads = DatabaseGetters.getAllThreads()
					threads.forEach {
						if (it.threadId == threadChannel.id && it.preventArchiving) {
							edit {
								content = "Thread archiving is already being prevented!"
							}
							return@action
						} else if (it.threadId == threadChannel.id && !it.preventArchiving) {
							DatabaseSetters.setThreadOwner(it.threadId, it.ownerId, true)
						}
					}

					edit { content = "Thread archiving will now be prevented" }

					guild!!.getChannelOf<TextChannel>(config.moderationConfigData.channel).createMessage {
						embed {
							title = "Thread archiving disabled"
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
			}
		}

		event<ThreadUpdateEvent> {
			action {
				val channel = event.channel
				val ownedThread = DatabaseGetters.getThread(channel.id)

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
		val databaseThreadOwner = DatabaseGetters.getThread(inputThread.id)?.ownerId

		if (inputMember.hasPermission(Permission.ModerateMembers) || databaseThreadOwner == inputMember.id) {
			return true
		}

		respond { content = "**Error:** This is not your thread!" }
		return false
	}
}
