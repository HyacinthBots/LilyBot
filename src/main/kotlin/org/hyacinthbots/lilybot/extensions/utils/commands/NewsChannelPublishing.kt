package org.hyacinthbots.lilybot.extensions.utils.commands

import dev.kord.common.Locale
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.channelType
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.channel
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.pagination.EphemeralResponsePaginator
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.pagination.pages.Pages
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.NewsChannelPublishingCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

class NewsChannelPublishing : Extension() {
	override val name = "news-channel-publishing"

	override suspend fun setup() {
		event<MessageCreateEvent> {
			check {
				anyGuild()
				channelType(ChannelType.GuildNews)
				failIf(event.message.author == null)
			}

			action {
				if (event.guildId == null) return@action
				NewsChannelPublishingCollection().getAutoPublishingChannel(event.guildId!!, event.message.channelId)
					?: return@action

				val permissions =
					event.message.channel.asChannelOfOrNull<NewsChannel>()?.getEffectivePermissions(kord.selfId)

				if (permissions?.contains(
						Permissions(Permission.SendMessages, Permission.ManageChannels, Permission.ManageMessages)
					) == false
				) {
					val channel =
						getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, event.getGuildOrNull()!!) ?: return@action
					channel.createEmbed {
						title = "Unable to Auto-publish news channel!"
						description =
							"Please ensure I have the `Send Messages`, `Manage Channel` or `Manage Messages` permission"
						field {
							name = "Channel:"
							value = event.message.channel.mention
						}
						color = DISCORD_RED
						timestamp = Clock.System.now()
					}
					return@action
				}

				event.message.publish()
			}
		}

		ephemeralSlashCommand {
			name = "news-publishing"
			description = "The parent command for news publishing channels"

			ephemeralSubCommand(::PublishingSetArgs) {
				name = "set"
				description = "Set this channel to automatically publish messages."

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					if (channel.asChannelOfOrNull<NewsChannel>()?.getEffectivePermissions(event.kord.selfId)
							?.contains(Permissions(Permission.SendMessages, Permission.ManageChannels)) == false
					) {
						respond {
							content = "I don't have permission for this channel; Please ensure I have the " +
									"`Send Messages` or `Manage Channel` permission"
						}
						return@action
					}

					NewsChannelPublishingCollection().addAutoPublishingChannel(guild!!.id, arguments.channel.id)

					respond {
						content = "${arguments.channel.mention} has been set to automatically publish messages!"
					}

					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, getGuild()!!)?.createEmbed {
						title = "News Channel set to Auto-Publish"
						field {
							name = "Channel:"
							value = arguments.channel.mention
						}
						footer {
							text = "Set by ${user.asUserOrNull()?.username}"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}
			}

			ephemeralSubCommand(::PublishingRemoveArgs) {
				name = "remove"
				description = "Stop a news channel from auto-publishing messages"

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					if (NewsChannelPublishingCollection().getAutoPublishingChannel(
							guild!!.id,
							arguments.channel.id
						) == null
					) {
						respond {
							content = "**Error:** ${arguments.channel.mention} does not automatically publish messages!"
						}
						return@action
					}

					NewsChannelPublishingCollection().removeAutoPublishingChannel(guild!!.id, arguments.channel.id)

					respond {
						content = "${arguments.channel.mention} will no longer automatically publish messages!"
					}

					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, getGuild()!!)?.createEmbed {
						title = "News Channel will no longer Auto-Publish"
						field {
							name = "Channel:"
							value = arguments.channel.mention
						}
						footer {
							text = "Removed by ${user.asUserOrNull()?.username}"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}
			}

			ephemeralSubCommand {
				name = "list"
				description = "List Auto-publishing channels"

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					val pagesObj = Pages()
					val channelsData = NewsChannelPublishingCollection().getAutoPublishingChannels(guild!!.id)

					if (channelsData.isEmpty()) {
						pagesObj.addPage(
							Page {
								description = "There are no news channels set for this guild."
							}
						)
					} else {
						channelsData.chunked(10).forEach { channelDataChunk ->
							var response = ""
							channelDataChunk.forEach { data ->
								val channel = guild!!.getChannelOrNull(data.channelId)
								response += "${channel?.mention ?: "Unable to get channel!"} (${channel?.name ?: ""}"
							}
							pagesObj.addPage(
								Page {
									title = "Auto-publishing channels for this guild"
									description = "These are all news channels that automatically publish messages"
									field {
										value = response
									}
								}
							)
						}
					}

					EphemeralResponsePaginator(
						pages = pagesObj,
						owner = event.interaction.user,
						timeoutSeconds = 500,
						locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
						interaction = interactionResponse
					).send()
				}
			}

			ephemeralSubCommand {
				name = "remove-all"
				description = "Remove all auto-publishing channels for this guild"

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					if (NewsChannelPublishingCollection().getAutoPublishingChannels(guild!!.id).isEmpty()) {
						respond {
							content = "**Error**: There are no auto-publishing channels for this guild!"
						}
					} else {
						NewsChannelPublishingCollection().clearAutoPublishingForGuild(guild!!.id)
						respond {
							content = "Cleared all auto-publishing channels from this guild!"
						}
					}
				}
			}
		}
	}

	inner class PublishingSetArgs : Arguments() {
		val channel by channel {
			name = "channel"
			description = "The channel to set auto-publishing for"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildNews)
		}
	}

	inner class PublishingRemoveArgs : Arguments() {
		val channel by channel {
			name = "channel"
			description = "The channel to stop auto-publishing for"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildNews)
		}
	}
}
