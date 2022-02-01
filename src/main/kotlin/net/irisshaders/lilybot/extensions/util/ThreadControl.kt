/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.coroutines.flow.toList
import net.irisshaders.lilybot.utils.MODERATORS
import kotlin.time.ExperimentalTime

class ThreadControl : Extension() {

	override val name = "threadcontrol"

	override suspend fun setup() {
		publicSlashCommand {
			name = "thread"
			description = "The parent command for all /thread commands"

			ephemeralSubCommand(::ThreadRenameArgs) {
				name = "rename"
				description = "Rename a thread!"

				action {
					val channel = channel.asChannel() as ThreadChannel
					val member = user.asMember(guild!!.id)
					val roles = member.roles.toList().map { it.id }

					if (MODERATORS in roles) {
						channel.edit {
							name = arguments.newThreadName

							reason = "Renamed by ${member.tag}"
						}
						edit {
							content = "Thread Renamed!"
						}

						return@action
					}

					if (channel.ownerId != user.id) {
						edit { content = "**Error:** This is not your thread!" }

						return@action
					}

					channel.edit {
						name = arguments.newThreadName

						reason = "Renamed by ${member.tag}"
					}

					edit { content = "Thread Renamed." }
				}
			}

			ephemeralSubCommand(::ThreadArchiveArgs) {
				name = "archive"
				description = "Archive this thread"

				action {
					val channel = channel.asChannel() as ThreadChannel
					val member = user.asMember(guild!!.id)
					val roles = member.roles.toList().map { it.id }

					if (MODERATORS in roles) {
						channel.edit {
							this.archived = true
							this.locked = arguments.lock

							reason = "Archived by ${user.asUser().tag}"
						}

						edit {
							content = "Thread archived"

							if (arguments.lock) content += " and locked"

							content += "!"
						}

						return@action
					}

					if (channel.ownerId != user.id) {
						edit { content = "This is not your thread!" }

						return@action
					}

					if (channel.isArchived) {
						edit { content = "**Error:** This channel is already archived!" }

						return@action
					}

					channel.edit {
						archived = true

						reason = "Archived by ${user.asUser().tag}"
					}

					edit { content = "Thread archived!" }
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
			description = "Whether to lock this thread, if you are a moderator. Default is false"
			defaultValue = false
		}
	}
}
