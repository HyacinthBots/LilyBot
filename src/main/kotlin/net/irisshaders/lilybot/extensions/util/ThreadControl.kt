/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.thread.ThreadChannel
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent

class ThreadControl : Extension() {

	override val name = "thread-control"

	override suspend fun setup() {
		publicSlashCommand {
			name = "thread"
			description = "The parent command for all /thread commands"

			ephemeralSubCommand(::ThreadRenameArgs) {
				name = "rename"
				description = "Rename a thread!"

				check { isInThread() }
				check { configPresent() }

				action {
					val threadChannel = channel.asChannel() as ThreadChannel
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

				check { isInThread() }
				check { configPresent() }

				action {
					val threadChannel = channel.asChannel() as ThreadChannel
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

				check { isInThread() }
				check { configPresent() }

				action {
					val threadChannel = channel.asChannel() as ThreadChannel
					val member = user.asMember(guild!!.id)

					val oldOwnerId = DatabaseHelper.getThreadOwner(threadChannel.id) ?: threadChannel.ownerId
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

					DatabaseHelper.setThreadOwner(threadChannel.id, arguments.newOwner.id)

					respond { content = "Ownership transferred." }

					var content = "Thread ownership transferred from ${oldOwner.mention} " +
							"to ${arguments.newOwner.mention}."

					if (member != oldOwner) content += " Transferred by ${member.mention}"

					threadChannel.createMessage(content)
				}
			}
		}
	}

	inner class ThreadRenameArgs : Arguments() {
		val newThreadName by string {
			name = "newName"
			description = "The new name to give to the thread"
		}
	}

	inner class ThreadArchiveArgs : Arguments() {
		val lock by defaultingBoolean {
			name = "lock"
			description = "Whether to lock the thread if you are a moderator. Default is false"
			defaultValue = false
		}
	}

	inner class ThreadTransferArgs : Arguments() {
		val newOwner by member {
			name = "newOwner"
			description = "The user you want to transfer ownership of the thread to"
		}
	}

	private suspend fun EphemeralSlashCommandContext<*>.ownsThreadOrModerator(
		inputThread: ThreadChannel,
		inputMember: Member
	): Boolean {
		val databaseThreadOwner = DatabaseHelper.getThreadOwner(inputThread.id)

		if (inputMember.hasPermission(Permission.ModerateMembers) || databaseThreadOwner == inputMember.id) {
			return true
		}

		respond { content = "**Error:** This is not your thread!" }
		return false
	}
}
