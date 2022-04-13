package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper

class Tags : Extension() {
	override val name = "tags"

	override suspend fun setup() {

		publicSlashCommand(::TagArgs) {
			name = "tag"
			description = "Tag commands!"

			check { anyGuild() }

			action {

				if (arguments.subTagName == null) {

					if (DatabaseHelper.getTag(guild!!.id, arguments.tagName, "name") == null) {
						respondEphemeral {
							content = "Unable to find tag! Does this tag exist?"
						}
						return@action
					}

					respond {
						embed {
							color = DISCORD_BLURPLE
							title = DatabaseHelper.getTag(guild!!.id, arguments.tagName, "tagTitle")!!.toString()
							description = DatabaseHelper.getTag(guild!!.id, arguments.tagName, "tagValue")!!.toString()
							timestamp = Clock.System.now()
						}
					}
				} else {

					if (DatabaseHelper.getSubTags(guild!!.id, arguments.tagName, arguments.subTagName!!, "name") == null) {
						respondEphemeral {
							content = "Unable to find sub tag! Does this sub tag exist?"
						}
						return@action
					}

					respond {
						embed {
							color = DISCORD_BLURPLE
							title = DatabaseHelper.getSubTags(guild!!.id, arguments.tagName, arguments.subTagName!!, "tagTitle")!!.toString()
							description = DatabaseHelper.getSubTags(guild!!.id, arguments.tagName, arguments.subTagName!!, "tagValue")!!.toString()
							timestamp = Clock.System.now()
						}
					}
				}
			}
		}

		ephemeralSlashCommand(::CreateTagArgs) {
			name = "create-tag"
			description = "Create a tag for your guild!"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			action {
				DatabaseHelper.setTag(guild!!.id, arguments.name, arguments.tagTitle, arguments.tagValue)

				respond {
					content = "Tag: `${arguments.name}` created"
				}
			}
		}

		ephemeralSlashCommand(::CreateSubTagArgs) {
			name = "create-sub-tag"
			description = "Create a sub tag"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			action {
				if (DatabaseHelper.getTag(guild!!.id, arguments.parentTag, "name") == null) {
					respond {
						content = "**Error:** Unable to find parent tag! Does the tag exist?"
					}
					return@action
				}
				DatabaseHelper.setSubTag(guild!!.id, arguments.parentTag, arguments.name, arguments.tagTitle, arguments.tagValue)

				respond {
					content = "Sub Tag: `${arguments.parentTag} ${arguments.name}` created"
				}
			}
		}

		ephemeralSlashCommand(::TagArgs) {
			name = "delete-tag"
			description = "Delete a tag from your guild"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			action {

				if (arguments.subTagName == null) {
					if (DatabaseHelper.getTag(guild!!.id, arguments.tagName, "name") == null) {
						respond {
							content = "Unable to find tag! Does this tag exist?"
						}
						return@action
					}

					DatabaseHelper.deleteTag(guild!!.id, arguments.tagName)

					respond {
						content = "Tag: `${arguments.tagName}` deleted"
					}
				} else {
					if (DatabaseHelper.getSubTags(guild!!.id, arguments.tagName, arguments.subTagName!!, "name") == null) {
						respond {
							content = "Unable to find sub tag! Does this sub tag exist?"
						}
						return@action
					}

					DatabaseHelper.deleteSubTag(guild!!.id, arguments.tagName, arguments.subTagName!!)

					respond {
						content = "Sub tag: `${arguments.tagName} ${arguments.subTagName}` deleted"
					}
				}
			}
		}
	}

	inner class TagArgs : Arguments() {
		val tagName by string {
			name = "tagName"
			description = "The name of the tag"
		}
		val subTagName by optionalString {
			name = "subTagName"
			description = "The name of the sub-tag, if any"
		}
	}

	inner class CreateTagArgs : Arguments() {
		val name by string {
			name = "tagName"
			description = "The name of the tag you're making"
		}
		val tagTitle by string {
			name = "title"
			description = "The title of the tag embed you're making"
		}
		val tagValue by string {
			name = "value"
			description = "The content of the tag embed you're making"
		}
	}

	inner class CreateSubTagArgs : Arguments() {
		val parentTag by string {
			name = "parentTagName"
			description = "The name of the parent tag"
		}
		val name by string {
			name = "tagName"
			description = "The name of the tag you're making"
		}
		val tagTitle by string {
			name = "title"
			description = "The title of the tag embed you're making"
		}
		val tagValue by string {
			name = "value"
			description = "The content of the tag embed you're making"
		}
	}
}
