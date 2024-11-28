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
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
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
		val translations = Translations.Utility.UtilityEvents

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

				if (allowed.isBlank()) allowed = translations.noOverrides.translate()
				if (denied.isBlank()) denied = translations.noOverrides.translate()

				getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!)?.createEmbed {
					title = translations.createTitle.translate(writeChannelType(event.channel.type))
					description =
						translations.createDesc.translate(event.channel.mention, event.channel.data.name.value)
					field {
						name = translations.allowed.translate()
						value = allowed
						inline = true
					}
					field {
						name = translations.denied.translate()
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
					title = translations.deleteTitle.translate(writeChannelType(event.channel.type))
					description = translations.deleteDesc.translate(event.channel.data.name.value)
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
					title = translations.updateTitle.translate(writeChannelType(event.channel.type))
					oldNewEmbedField(
						translations.typeChange, writeChannelType(event.old?.type), writeChannelType(event.channel.type)
					)
					oldNewEmbedField(translations.nameChange, oldData?.name?.value, newData.name.value)
					oldNewEmbedField(translations.topicChange, oldData?.topic?.value, newData.topic.value)
					oldNewEmbedField(
						translations.parentChange,
						kord.getChannelOf<Category>(oldData?.parentId.value!!)?.mention,
						kord.getChannelOf<Category>(newData.parentId.value!!)?.mention
					)
					if (event.channel.data.nsfw != event.old?.data?.nsfw) {
						field {
							name = translations.nsfwChange.translate()
							value = event.channel.data.nsfw.discordBoolean.toString()
						}
					}
					oldNewEmbedField(translations.positionChanged, oldData?.position.value, newData.position.value)
					oldNewEmbedField(
						translations.slowmodeChange,
						oldData?.rateLimitPerUser?.value?.toString() ?: "0",
						newData.rateLimitPerUser.value?.toString() ?: "0"
					)
					oldNewEmbedField(translations.bitrateChange, oldData?.bitrate.value, newData.bitrate.value)
					oldNewEmbedField(
						translations.userlimitChange,
						oldData?.userLimit.value ?: 0,
						newData.userLimit.value ?: 0
					)
					oldNewEmbedField(
						translations.regionChanged,
						oldData?.rtcRegion?.value ?: "Automatic",
						newData.rtcRegion.value ?: "Automatic"
					)
					oldNewEmbedField(
						translations.videoQualityChanged,
						oldData?.videoQualityMode?.value.afterDot(),
						newData.videoQualityMode.value.afterDot()
					)
					oldNewEmbedField(
						translations.defaultAutoChanged,
						oldData?.defaultAutoArchiveDuration?.value?.duration.toString(),
						newData.defaultAutoArchiveDuration.value?.duration.toString()
					)
					oldNewEmbedField(
						translations.defaultSortChanged,
						oldData?.defaultSortOrder?.value.afterDot(),
						newData.defaultSortOrder.value.afterDot()
					)
					oldNewEmbedField(
						translations.defaultLayoutChanged,
						oldData?.defaultForumLayout?.value.afterDot(),
						newData.defaultForumLayout.value.afterDot()
					)
					oldNewEmbedField(
						translations.availableTagsChanged,
						formatAvailableTags(oldData?.availableTags?.value),
						formatAvailableTags(newData.availableTags.value)
					)
					oldNewEmbedField(
						translations.appliedTagsChanged,
						oldAppliedTags.joinToString(", "),
						newAppliedTags.joinToString(", ")
					)
					oldNewEmbedField(
						translations.defaultReactionChanged,
						oldData?.defaultReactionEmoji?.value?.emojiName,
						newData.defaultReactionEmoji.value?.emojiName
					)
					oldNewEmbedField(
						translations.defaultThreadChanged,
						oldData?.defaultThreadRateLimitPerUser?.value.toString(),
						newData.defaultThreadRateLimitPerUser.value.toString()
					)
					if (oldAllowed != newAllowed) {
						field {
							name = translations.newAllowed.translate()
							value = newAllowed
							inline = true
						}
						field {
							name = translations.oldAllowed.translate()
							value = oldAllowed
							inline = true
						}
					}
					if (oldDenied != newDenied) {
						field {
							name = translations.newDenied.translate()
							value = newDenied
							inline = false
						}
						field {
							name = translations.oldDenied.translate()
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
					title = translations.scheduledCreateTitle.translate()
					description = translations.scheduledCreateDesc.translate()
					guildEventEmbed(event, guild)
					// This appears to exist in the front end but the api doesn't have it anywhere, api payloads contain
					// a recurrence field that would fill this but the api doesn't mention it
// 					field {
// 						name = "Repeating Frequency"
// 						value = event.scheduledEvent.
// 					}
					field {
						name = translations.guildMembersOnly.translate()
						value = if (event.scheduledEvent.privacyLevel == GuildScheduledEventPrivacyLevel.GuildOnly) {
							Translations.Basic.`true`
						} else {
							Translations.Basic.`false`
						}.translate()
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
					title = translations.scheduledDeleteTitle.translate()
					description = translations.scheduledDeleteDesc.translate()
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
					title = translations.scheduledUpdateTitle.translate()
					description = translations.scheduledUpdateDesc.translate()
					oldNewEmbedField(translations.nameChange, oldEvent?.name, newEvent.name)
					oldNewEmbedField(translations.descriptionChange, oldEvent?.description, newEvent.description)
					oldNewEmbedField(
						translations.locationChange,
						oldEvent?.channelId?.let { guild.getChannelOrNull(it) }?.mention
							?: Translations.Basic.UnableTo.channel.translate(),
						newEvent.channelId?.let { guild.getChannelOrNull(it) }?.mention
							?: Translations.Basic.UnableTo.channel.translate()
					)
					oldNewEmbedField(
						translations.startChanged,
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
					title = translations.inviteCreateTitle.translate()
					description = translations.inviteCreateDesc.translate()
					field {
						name = translations.code.translate()
						value = event.code
					}
					field {
						name = translations.targetChannel.translate()
						value = event.channel.mention
					}
					field {
						name = translations.maxUses.translate()
						value = event.maxUses.toString()
					}
					field {
						name = translations.duration.translate()
						value = event.maxAge.toIsoString()
					}
					field {
						name = translations.tempMembershipInvite.translate()
						value = event.isTemporary.toString()
					}
					footer {
						text = Translations.Basic.createdBy.translate(event.getInviterAsMemberOrNull()?.mention)
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
					title = translations.inviteDeleteTitle.translate()
					description = translations.inviteDeleteDesc.translate()
					field {
						name = translations.code.translate()
						value = event.code
					}
					field {
						name = translations.targetChannel.translate()
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
					title = translations.createdRoleTitle.translate()
					description = translations.createdRoleDesc.translate()
					field {
						name = translations.roleName.translate()
						value = event.role.name
						inline = true
					}
					field {
						name = translations.roleColor.translate()
						value = event.role.color.rgb.toString()
						inline = true
					}
					field {
						name = translations.rolePosition.translate()
						value = event.role.rawPosition.toString()
						inline = true
					}
					field {
						name = translations.roleDisplaySep.translate()
						value = event.role.hoisted.toString()
						inline = true
					}
					field {
						name = translations.roleMention.translate()
						value = event.role.mentionable.toString()
						inline = true
					}
					field {
						name = translations.roleIcon.translate()
						value = event.role.icon?.cdnUrl?.toUrl() ?: translations.noRoleIcon.translate()
						inline = true
					}
					field {
						name = translations.emoji.translate()
						value = event.role.unicodeEmoji ?: translations.noEmoji.translate()
						inline = true
					}
					field {
						name = translations.managed.translate()
						value = event.role.managed.toString()
						inline = true
					}
					field {
						name = translations.permissions.translate()
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
					title = translations.deletedRoleTitle.translate()
					description = translations.deletedRoleDesc.translate()
					field {
						name = translations.roleName.translate()
						value = event.role?.name ?: translations.unableToName.translate()
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
						title = translations.updatedRoleTitle.translate()
						description = event.role.mention + translations.updatedRoleDesc.translate()
						oldNewEmbedField(translations.nameChange, event.old?.name, event.role.name)
						oldNewEmbedField(translations.displaySepChanged, event.old?.hoisted, event.role.hoisted)
						oldNewEmbedField(translations.mentionChanged, event.old?.mentionable, event.role.mentionable)
						oldNewEmbedField(
							translations.positionChanged,
							event.old?.getPosition(),
							event.role.getPosition()
						)
						oldNewEmbedField(
							translations.iconChanged,
							event.old?.icon?.cdnUrl?.toUrl() ?: translations.noRoleIcon.translate(),
							event.role.icon?.cdnUrl?.toUrl() ?: translations.noRoleIcon.translate()
						)
						oldNewEmbedField(
							translations.emojiChanged,
							event.old?.unicodeEmoji ?: translations.noRoleIcon.translate(),
							event.role.unicodeEmoji ?: translations.noRoleIcon.translate()
						)
						oldNewEmbedField(
							translations.permissionsChanged,
							event.old?.permissions?.let { formatPermissionSet(it) } ?: "Unable to get permissions",
							formatPermissionSet(event.role.permissions)
						)
						color = DISCORD_GREEN
						timestamp = Clock.System.now()
					}
					if (event.old?.color != event.role.color) {
						embed {
							description = translations.oldColor.translate()
							color = if (event.old?.color?.rgb != 0) event.old?.color else null
						}
						embed {
							description = translations.newColor.translate()
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
					title = translations.createTitle.translate(writeChannelType(event.channel.type))
					description =
						translations.createDesc.translate(event.channel.mention, event.channel.data.name.value)
					field {
						name = translations.parentChannel.translate()
						value = "${event.channel.parent.mention} (`${event.channel.parent.asChannelOrNull()?.name}`)"
					}
					field {
						name = translations.archiveDuration.translate()
						value = event.channel.autoArchiveDuration.duration.toString()
					}
					if (appliedTags.isNotEmpty()) {
						field {
							name = translations.appliedTags.translate()
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
					title = translations.deleteTitle.translate(writeChannelType(event.channel.type))
					description = translations.deleteDesc.translate(event.channel.data.name.value)
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
	private fun EmbedBuilder.oldNewEmbedField(detailName: Key, oldValue: String?, newValue: String?) {
		if (newValue != oldValue) {
			field {
				name = detailName.translate()
				value =
					"${Translations.Basic.old.translate()}: $oldValue\n${Translations.Basic.new.translate()}: $newValue"
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
	private fun EmbedBuilder.oldNewEmbedField(detailName: Key, oldValue: Int?, newValue: Int?) =
		oldNewEmbedField(detailName, oldValue.toString(), newValue.toString())

	/**
	 * A version of [oldNewEmbedField] that takes booleans and converts them itself.
	 *
	 * @see oldNewEmbedField
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	private fun EmbedBuilder.oldNewEmbedField(detailName: Key, oldValue: Boolean?, newValue: Boolean?) =
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
		ChannelType.GuildCategory -> Translations.Utility.UtilityEvents.Channel.category
		ChannelType.GuildNews -> Translations.Utility.UtilityEvents.Channel.announcement
		ChannelType.GuildForum -> Translations.Utility.UtilityEvents.Channel.forum
		ChannelType.GuildStageVoice -> Translations.Utility.UtilityEvents.Channel.stage
		ChannelType.GuildText -> Translations.Utility.UtilityEvents.Channel.text
		ChannelType.GuildVoice -> Translations.Utility.UtilityEvents.Channel.voice
		ChannelType.PublicGuildThread -> Translations.Utility.UtilityEvents.Channel.thread
		ChannelType.PrivateThread -> Translations.Utility.UtilityEvents.Channel.privateThread
		else -> null
	}?.translate()

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
			tagString += "\n* ${Translations.Utility.UtilityEvents.name.translate()}: ${it.name}\n" +
				"* ${Translations.Utility.UtilityEvents.moderated.translate()}: ${it.moderated}\n" +
				"* ${Translations.Utility.UtilityEvents.emoji.translate()}: " +
				"${if (it.emojiId != null) "<!${it.emojiId}>" else it.emojiName}\n---"
		}
		return tagString.ifNullOrEmpty { Translations.Basic.none.translate() }
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
		val translations = Translations.Utility.UtilityEvents
		field {
			name = translations.eventName.translate()
			value = event.scheduledEvent.name
		}
		field {
			name = translations.eventDesc.translate()
			value = event.scheduledEvent.description ?: translations.eventDescNone.translate()
		}
		field {
			name = translations.eventLocation.translate()
			value = if (event.scheduledEvent.channelId != null) {
				guild.getChannelOrNull(event.scheduledEvent.channelId!!)?.mention ?: Translations.Basic.UnableTo.channel.translate()
			} else {
				translations.externalChannel.translate()
			}
		}
		field {
			name = translations.startTime.translate()
			value = event.scheduledEvent.scheduledStartTime.toDiscord(TimestampType.ShortDateTime)
		}
		image = event.scheduledEvent.image?.cdnUrl?.toUrl().ifNullOrEmpty { translations.noImage.translate() }
	}
}
