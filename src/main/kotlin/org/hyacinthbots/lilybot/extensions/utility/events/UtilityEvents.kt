package org.hyacinthbots.lilybot.extensions.utility.events

import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.ForumTag
import dev.kord.common.entity.GuildScheduledEventPrivacyLevel
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.Category
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.channel.ChannelDeleteEvent
import dev.kord.core.event.channel.ChannelUpdateEvent
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.channel.thread.ThreadChannelDeleteEvent
import dev.kord.core.event.guild.GuildScheduledEventCreateEvent
import dev.kord.core.event.guild.GuildScheduledEventDeleteEvent
import dev.kord.core.event.guild.GuildScheduledEventEvent
import dev.kord.core.event.guild.GuildScheduledEventUpdateEvent
import dev.kord.core.event.guild.InviteCreateEvent
import dev.kord.core.event.guild.InviteDeleteEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import dev.kord.core.event.role.RoleUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.afterDot
import org.hyacinthbots.lilybot.utils.formatPermissionSet
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
import kotlin.collections.forEach

/** A String identifier to use for the permission map to get allowed permissions. */
private const val ALLOWED = "Allowed"

/** A String identifier to use for the permission map to get denied permissions. */
private const val DENIED = "Denied"

class UtilityEvents : Extension() {
	override val name: String = "utility-events"

	override suspend fun setup() {
		event<ChannelCreateEvent> {
			check {
				failIf {
					// We get more detail from the specific event
					event.channel.type != ChannelType.PublicGuildThread || event.channel.type != ChannelType.PrivateThread
				}
			}
			action {
				val guildId = event.channel.data.guildId.value
				// Do not log if the channel logging option is false
				if (guildId?.let { UtilityConfigCollection().getConfig(it)?.logChannelUpdates } == false) return@action
				val guild = guildId?.let { GuildBehavior(it, event.kord) }
				val perms = formatPermissionsForDisplay(guild, event.channel)
				var allowed = perms.getValue(ALLOWED)
				var denied = perms.getValue(DENIED)

				if (allowed.isBlank()) allowed = "None overrides set"
				if (denied.isBlank()) denied = "None overrides set"

				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!)?.createEmbed {
					title = "${writeChannelType(event.channel.type)} Created"
					description = "${event.channel.mention} (${event.channel.data.name.value}) was created."
					field {
						name = "Allowed Permissions"
						value = allowed
						inline = true
					}
					field {
						name = "Denied Permissions"
						value = denied
						inline = true
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}
		event<ChannelDeleteEvent> {
			action {
				val guildId = event.channel.data.guildId.value
				// Do not log if the channel logging option is false
				if (guildId?.let { UtilityConfigCollection().getConfig(it)?.logChannelUpdates } == false) return@action
				val guild = guildId?.let { GuildBehavior(it, event.kord) }
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!)?.createEmbed {
					title = "${writeChannelType(event.channel.type)} Deleted"
					description = "`${event.channel.data.name.value}` was deleted."
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
		event<ChannelUpdateEvent> {
			check {
				failIf {
					// Data from old threads is almost always null, so we should not try to display changes
					event.channel.type != ChannelType.PublicGuildThread || event.channel.type != ChannelType.PrivateThread
				}
			}
			action {
				val guildId = event.channel.data.guildId.value
				// Do not log if the channel logging option is false
				if (guildId?.let { UtilityConfigCollection().getConfig(it)?.logChannelUpdates } == false) return@action
				val guild = guildId?.let { GuildBehavior(it, event.kord) }
				val oldPerms = formatPermissionsForDisplay(guild, event.old)
				val newPerms = formatPermissionsForDisplay(guild, event.channel)
				val oldAllowed = oldPerms.getValue(ALLOWED)
				val oldDenied = oldPerms.getValue(DENIED)
				val newAllowed = newPerms.getValue(ALLOWED)
				val newDenied = newPerms.getValue(DENIED)
				val oldData = event.old?.data
				val newData = event.channel.data
				val oldAppliedTags = mutableListOf<String>()
				newData.appliedTags.value?.forEach { tag ->
					event.old?.asChannelOrNull()?.data?.availableTags?.value?.filter { it.id == tag }
						?.get(0)?.name?.let { oldAppliedTags.add(it) }
				}
				val newAppliedTags = mutableListOf<String>()
				newData.appliedTags.value?.forEach { tag ->
					event.channel.asChannelOrNull()?.data?.availableTags?.value?.filter { it.id == tag }
						?.get(0)?.name?.let { newAppliedTags.add(it) }
				}

				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!)?.createEmbed {
					title = "${writeChannelType(event.channel.type)} Updated"
					oldNewEmbedField(
						"Type Change", writeChannelType(event.old?.type), writeChannelType(event.channel.type)
					)
					oldNewEmbedField("Name Change", oldData?.name?.value, newData.name.value)
					oldNewEmbedField("Topic Changed", oldData?.topic?.value, newData.topic.value)
					oldNewEmbedField(
						"Parent Category Change",
						kord.getChannelOf<Category>(oldData?.parentId.value!!)?.mention,
						kord.getChannelOf<Category>(newData.parentId.value!!)?.mention
					)
					if (event.channel.data.nsfw != event.old?.data?.nsfw) {
						field {
							name = "NSFW Setting"
							value = event.channel.data.nsfw.discordBoolean.toString()
						}
					}
					oldNewEmbedField("Position Changed", oldData?.position.value, newData.position.value)
					oldNewEmbedField(
						"Slowmode time changed",
						oldData?.rateLimitPerUser?.value?.toString() ?: "0",
						newData.rateLimitPerUser.value?.toString() ?: "0"
					)
					oldNewEmbedField("Bitrate changed", oldData?.bitrate.value, newData.bitrate.value)
					oldNewEmbedField("User limit changed", oldData?.userLimit.value ?: 0, newData.userLimit.value ?: 0)
					oldNewEmbedField(
						"Region Changed",
						oldData?.rtcRegion?.value ?: "Automatic",
						newData.rtcRegion.value ?: "Automatic"
					)
					oldNewEmbedField(
						"Video Quality Changed",
						oldData?.videoQualityMode?.value.afterDot(),
						newData.videoQualityMode.value.afterDot()
					)
					oldNewEmbedField(
						"Default Auto-Archive Duration",
						oldData?.defaultAutoArchiveDuration?.value?.duration.toString(),
						newData.defaultAutoArchiveDuration.value?.duration.toString()
					)
					oldNewEmbedField(
						"Default Sort Changed",
						oldData?.defaultSortOrder?.value.afterDot(),
						newData.defaultSortOrder.value.afterDot()
					)
					oldNewEmbedField(
						"Default Layout Changed",
						oldData?.defaultForumLayout?.value.afterDot(),
						newData.defaultForumLayout.value.afterDot()
					)
					oldNewEmbedField(
						"Available tags Changed",
						formatAvailableTags(oldData?.availableTags?.value),
						formatAvailableTags(newData.availableTags.value)
					)
					oldNewEmbedField(
						"Applied tags Changed",
						oldAppliedTags.joinToString(", "),
						newAppliedTags.joinToString(", ")
					)
					oldNewEmbedField(
						"Default Reaction Emoji Changed",
						oldData?.defaultReactionEmoji?.value?.emojiName,
						newData.defaultReactionEmoji.value?.emojiName
					)
					oldNewEmbedField(
						"Default Thread Slowmode Changed",
						oldData?.defaultThreadRateLimitPerUser?.value.toString(),
						newData.defaultThreadRateLimitPerUser.value.toString()
					)
					if (oldAllowed != newAllowed) {
						field {
							name = "New Allowed Permissions"
							value = newAllowed
							inline = true
						}
						field {
							name = "Old Allowed Permissions"
							value = oldAllowed
							inline = true
						}
					}
					if (oldDenied != newDenied) {
						field {
							name = "New Denied Permissions"
							value = newDenied
							inline = false
						}
						field {
							name = "Old Denied Permissions"
							value = oldDenied
							inline = true
						}
					}
					color = DISCORD_YELLOW
					timestamp = Clock.System.now()
				}
			}
		}
		event<GuildScheduledEventCreateEvent> {
			check { anyGuild() }
			action {
				// Do not log if event updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logEventUpdates == false) return@action
				val guild = GuildBehavior(event.guildId, kord)
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Scheduled Event Created!"
					description = "An event has been created!"
					guildEventEmbed(event, guild)
					// This appears to exist in the front end but the api doesn't have it anywhere, api payloads contain
					// a recurrence field that would fill this but the api doesn't mention it
// 					field {
// 						name = "Repeating Frequency"
// 						value = event.scheduledEvent.
// 					}
					field {
						name = "Guild Members only"
						value = if (event.scheduledEvent.privacyLevel == GuildScheduledEventPrivacyLevel.GuildOnly) {
							"True"
						} else {
							"False"
						}
					}
					color = DISCORD_GREEN
					footer {
						text =
							"Created by ${event.scheduledEvent.creatorId?.let { guild.getMemberOrNull(it) }?.username}"
						icon =
							event.scheduledEvent.creatorId?.let { guild.getMemberOrNull(it) }?.avatar?.cdnUrl?.toUrl()
					}
				}
			}
		}
		event<GuildScheduledEventDeleteEvent> {
			check { anyGuild() }
			action {
				// Do not log if event updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logEventUpdates == false) return@action
				val guild = GuildBehavior(event.guildId, kord)
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Scheduled Event Deleted!"
					description = "An event has been deleted"
					guildEventEmbed(event, guild)
					color = DISCORD_RED
					footer {
						text =
							"Originally created by ${event.scheduledEvent.creatorId?.let { guild.getMemberOrNull(it) }?.username}"
						icon =
							event.scheduledEvent.creatorId?.let { guild.getMemberOrNull(it) }?.avatar?.cdnUrl?.toUrl()
					}
				}
			}
		}
		event<GuildScheduledEventUpdateEvent> {
			check { anyGuild() }
			action {
				// Do not log if event updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logEventUpdates == false) return@action
				val guild = GuildBehavior(event.guildId, kord)
				val oldEvent = event.oldEvent
				val newEvent = event.scheduledEvent
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Scheduled Event Updated!"
					description = "An event has been updated"
					oldNewEmbedField("Name Changed", oldEvent?.name, newEvent.name)
					oldNewEmbedField("Description Changed", oldEvent?.description, newEvent.description)
					oldNewEmbedField(
						"Location Changed",
						oldEvent?.channelId?.let { guild.getChannelOrNull(it) }?.mention ?: "Unable to get channel",
						newEvent.channelId?.let { guild.getChannelOrNull(it) }?.mention ?: "Unable to get channel"
					)
					oldNewEmbedField(
						"Start time changed",
						oldEvent?.scheduledStartTime?.toDiscord(TimestampType.ShortDateTime),
						newEvent.scheduledStartTime.toDiscord(TimestampType.ShortDateTime)
					)
					color = DISCORD_YELLOW
				}
			}
		}
		event<InviteCreateEvent> {
			check { anyGuild() }
			action {
				// Do not log if invite updates are disabled
				if (event.guildId?.let { UtilityConfigCollection().getConfig(it) }?.logInviteUpdates == false) return@action
				val guild = event.guildId?.let { GuildBehavior(it, kord) } ?: return@action
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Invite link created"
					description = "An invite has been created"
					field {
						name = "Code"
						value = event.code
					}
					field {
						name = "Target Channel"
						value = event.channel.mention
					}
					field {
						name = "Max uses"
						value = event.maxUses.toString()
					}
					field {
						name = "Duration of Invite"
						value = event.maxAge.toIsoString()
					}
					field {
						name = "Temporary Membership invite"
						value = event.isTemporary.toString()
					}
					footer {
						text = "Created by ${event.getInviterAsMemberOrNull()?.mention}"
						icon = event.getInviterAsMemberOrNull()?.avatar?.cdnUrl?.toUrl()
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}
		event<InviteDeleteEvent> {
			check { anyGuild() }
			action {
				// Do not log if invite updates are disabled
				if (event.guildId?.let { UtilityConfigCollection().getConfig(it) }?.logInviteUpdates == false) return@action
				val guild = event.guildId?.let { GuildBehavior(it, kord) } ?: return@action
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Invite link deleted"
					description = "An invite has been deleted"
					field {
						name = "Code"
						value = event.code
					}
					field {
						name = "Target Channel"
						value = event.channel.mention
					}
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
		event<RoleCreateEvent> {
			action {
				// Do not log if role updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logRoleUpdates == false) return@action
				val guild = GuildBehavior(event.guildId, kord)
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Created a Role"
					description = "A new role has been created"
					field {
						name = "Role name"
						value = event.role.name
						inline = true
					}
					field {
						name = "Role Color"
						value = event.role.color.rgb.toString()
						inline = true
					}
					field {
						name = "Position"
						value = event.role.rawPosition.toString()
						inline = true
					}
					field {
						name = "Display separately?"
						value = event.role.hoisted.toString()
						inline = true
					}
					field {
						name = "Mentionable"
						value = event.role.mentionable.toString()
						inline = true
					}
					field {
						name = "Icon"
						value = event.role.icon?.cdnUrl?.toUrl() ?: "No icon"
						inline = true
					}
					field {
						name = "Emoji"
						value = event.role.unicodeEmoji ?: "No emoji"
						inline = true
					}
					field {
						name = "Managed by integration?"
						value = event.role.managed.toString()
						inline = true
					}
					field {
						name = "Permissions"
						value = formatPermissionSet(event.role.permissions)
					}
					color = DISCORD_GREEN
					timestamp = Clock.System.now()
				}
			}
		}
		event<RoleDeleteEvent> {
			action {
				// Do not log if role updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logRoleUpdates == false) return@action
				val guild = GuildBehavior(event.guildId, kord)
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)?.createEmbed {
					title = "Role deleted"
					description = "A role has been deleted"
					field {
						name = "Role name"
						value = event.role?.name ?: "Unable to get name"
					}
					color = DISCORD_RED
					timestamp = Clock.System.now()
				}
			}
		}
		event<RoleUpdateEvent> {
			action {
				// Do not log if role updates are disabled
				if (UtilityConfigCollection().getConfig(event.guildId)?.logRoleUpdates == false) return@action
				// FIXME this sucks how can i make it not suck
				@Suppress("ComplexCondition") // Am i surprised? no.
				if (event.old?.name == event.role.name && event.old?.hoisted == event.role.hoisted &&
					event.old?.mentionable == event.role.mentionable && event.old?.getPosition() == event.role.getPosition() &&
					event.old?.icon == event.role.icon && event.old?.unicodeEmoji == event.role.unicodeEmoji &&
					event.old?.permissions == event.role.permissions && event.old?.color == event.role.color &&
					(
					    event.old?.managed != event.role.managed || event.old?.tags != event.role.tags ||
						event.old?.flags != event.role.flags
					)
				) {
						    return@action
						}
				val guild = GuildBehavior(event.guildId, kord)
				val channel = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)
				channel?.createMessage {
					embed {
						title = "Updated a Role"
						description = "A role has been updated"
						oldNewEmbedField("Name changed", event.old?.name, event.role.name)
						oldNewEmbedField("Display separately setting changed", event.old?.hoisted, event.role.hoisted)
						oldNewEmbedField("Mentionable setting changed", event.old?.mentionable, event.role.mentionable)
						oldNewEmbedField("Position changed", event.old?.getPosition(), event.role.getPosition())
						oldNewEmbedField(
							"Icon changed",
							event.old?.icon?.cdnUrl?.toUrl() ?: "No icon",
							event.role.icon?.cdnUrl?.toUrl() ?: "No icon"
						)
						oldNewEmbedField(
							"Emoji changed", event.old?.unicodeEmoji ?: "No icon", event.role.unicodeEmoji ?: "No icon"
						)
						oldNewEmbedField(
							"Permissions changed",
							event.old?.permissions?.let { formatPermissionSet(it) } ?: "Unable to get permissions",
							formatPermissionSet(event.role.permissions)
						)
						color = DISCORD_GREEN
						timestamp = Clock.System.now()
					}
					if (event.old?.color != event.role.color) {
						embed {
							description = "Old color"
							color = if (event.old?.color?.rgb != 0) event.old?.color else null
						}
						embed {
							description = "New color"
							color = event.role.color
						}
					}
				}
			}
		}
		event<ThreadChannelCreateEvent> {
			check { anyGuild() }
			action {
				// Do not log if the channel logging option is false
				if (UtilityConfigCollection().getConfig(event.channel.guild.id)?.logChannelUpdates == false) return@action
				val appliedTags = mutableListOf<String>()
				event.channel.appliedTags.forEach { tag ->
					event.channel.parent.asChannelOfOrNull<ForumChannel>()
						?.availableTags?.filter { it.id == tag }?.get(0)?.name?.let { appliedTags.add(it) }
				}

				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, event.channel.guild)?.createEmbed {
					title = "${writeChannelType(event.channel.type)} Created"
					description = "${event.channel.mention} (${event.channel.data.name.value}) was created."
					field {
						name = "Parent Channel"
						value = "${event.channel.parent.mention} (`${event.channel.parent.asChannelOrNull()?.name}`)"
					}
					field {
						name = "Archive duration"
						value = event.channel.autoArchiveDuration.duration.toString()
					}
					if (appliedTags.isNotEmpty()) {
						field {
							name = "Applied tags"
							value = appliedTags.joinToString(", ")
						}
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}
		event<ThreadChannelDeleteEvent> {
			check { anyGuild() }
			action {
				// Do not log if the channel logging option is false
				if (UtilityConfigCollection().getConfig(event.channel.guild.id)?.logChannelUpdates == false) return@action
				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, event.channel.guild)?.createEmbed {
					title = "${writeChannelType(event.channel.type)} Deleted"
					description = "`${event.channel.data.name.value}` was deleted."
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
		// No thread update event because the old object is almost always null, meaning that displaying the changes is
		// effectively pointless because no original values are available.
	}

	/**
	 * Compare two values to see if they've changed and format an embed field indicating the old value versus the new
	 * value.
	 *
	 * @param detailName The title for the embed field, the information on what has changed
	 * @param oldValue The original value
	 * @param newValue The new value
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun EmbedBuilder.oldNewEmbedField(detailName: String, oldValue: String?, newValue: String?) {
		if (newValue != oldValue) {
			field {
				name = detailName
				value = "Old: $oldValue\nNew: $newValue"
			}
		}
	}

	/**
	 * A version of [oldNewEmbedField] that takes integers and converts them itself.
	 *
	 * @see oldNewEmbedField
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun EmbedBuilder.oldNewEmbedField(detailName: String, oldValue: Int?, newValue: Int?) =
		oldNewEmbedField(detailName, oldValue.toString(), newValue.toString())

	/**
	 * A version of [oldNewEmbedField] that takes booleans and converts them itself.
	 *
	 * @see oldNewEmbedField
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun EmbedBuilder.oldNewEmbedField(detailName: String, oldValue: Boolean?, newValue: Boolean?) =
		oldNewEmbedField(detailName, oldValue.toString(), newValue.toString())

	/**
	 * Writes a [ChannelType] into a String to use as a reasonable title.
	 *
	 * @param type The type of the channel
	 * @return A String for the channel title
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun writeChannelType(type: ChannelType?): String? = when (type) {
		ChannelType.GuildCategory -> "Category"
		ChannelType.GuildNews -> "Announcement Channel"
		ChannelType.GuildForum -> "Forum Channel"
		ChannelType.GuildStageVoice -> "Stage Channel"
		ChannelType.GuildText -> "Text Channel"
		ChannelType.GuildVoice -> "Voice Channel"
		ChannelType.PublicGuildThread -> "Thread"
		ChannelType.PrivateThread -> "Private Thread"
		else -> null
	}

	/**
	 * Formats the permission overwrites for a [channel] to a string map of allowed and denied permissions.
	 *
	 * @param guild The guild the channel is in
	 * @param channel The channel object to get the permissions for
	 * @return A [Map] of strings for allowed and denied permissions
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private suspend inline fun formatPermissionsForDisplay(
		guild: GuildBehavior?,
		channel: Channel?
	): Map<String, String> {
		val map = mutableMapOf<String, String>()
		map[ALLOWED] = ""
		map[DENIED] = ""
		channel?.data?.permissionOverwrites?.value?.forEach {
			map[ALLOWED] += "${guild!!.getRoleOrNull(it.id)?.mention}: ${formatPermissionSet(it.allow)}\n"
			map[DENIED] += "${guild.getRoleOrNull(it.id)?.mention}: ${formatPermissionSet(it.deny)}\n"
		}
		return map
	}

	/**
	 * Formats the Available tags ([ForumTag]) into a readable bullet pointed display for the update embed.
	 *
	 * @param tagList A List of [ForumTag]s to format
	 * @return The formated string from [tagList]
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun formatAvailableTags(tagList: List<ForumTag>?): String {
		var tagString = ""
		tagList?.forEach {
			tagString += "\n* Name: ${it.name}\n* Moderated: ${it.moderated}\n" +
				"* Emoji: ${if (it.emojiId != null) "<!${it.emojiId}>" else it.emojiName}\n---"
		}
		return tagString.ifNullOrEmpty { "None" }
	}

	/**
	 * Fills out the content for Guild event updates to avoid repeating code.
	 *
	 * @param event The event instance
	 * @param guild The guild for the event
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private suspend fun EmbedBuilder.guildEventEmbed(event: GuildScheduledEventEvent, guild: GuildBehavior) {
		field {
			name = "Event Name"
			value = event.scheduledEvent.name
		}
		field {
			name = "Event Description"
			value = event.scheduledEvent.description ?: "No description provided"
		}
		field {
			name = "Event location"
			value = if (event.scheduledEvent.channelId != null) {
				guild.getChannelOrNull(event.scheduledEvent.channelId!!)?.mention ?: "Unable to get channel"
			} else {
				"External event, no channel."
			}
		}
		field {
			name = "Start time"
			value = event.scheduledEvent.scheduledStartTime.toDiscord(TimestampType.ShortDateTime)
		}
		image = event.scheduledEvent.image?.cdnUrl?.toUrl().ifNullOrEmpty { "No image" }
	}
}
