package org.hyacinthbots.lilybot.extensions.utility.commands

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
import lilybot.i18n.Translations
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
						title = Translations.Utility.NewsChannel.NewsPublishing.errorTitle.translate()
						description = Translations.Utility.NewsChannel.NewsPublishing.missingPerms.translate()
						field {
							name = Translations.Utility.NewsChannel.NewsPublishing.embedChannelField.translate()
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
			name = Translations.Utility.NewsChannel.NewsPublishing.name
			description = Translations.Utility.NewsChannel.NewsPublishing.description

			ephemeralSubCommand(::PublishingSetArgs) {
				name = Translations.Utility.NewsChannel.NewsPublishing.Set.name
				description = Translations.Utility.NewsChannel.NewsPublishing.Set.description

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					val translations = Translations.Utility.NewsChannel.NewsPublishing.Set
					if (channel.asChannelOfOrNull<NewsChannel>()?.getEffectivePermissions(event.kord.selfId)
							?.contains(Permissions(Permission.SendMessages, Permission.ManageChannels)) == false
					) {
						respond {
							content =
								translations.noPerms.translate() + Translations.Utility.NewsChannel.NewsPublishing.missingPerms.translate()
						}
						return@action
					}

					NewsChannelPublishingCollection().addAutoPublishingChannel(guild!!.id, arguments.channel.id)

					respond {
						content = translations.success.translate(arguments.channel.mention)
					}

					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, getGuild()!!)?.createEmbed {
						title = translations.embedTitle.translate()
						field {
							name = Translations.Utility.NewsChannel.NewsPublishing.embedChannelField.translate()
							value = arguments.channel.mention
						}
						footer {
							text = translations.setBy.translate(user.asUserOrNull()?.username)
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}
			}

			ephemeralSubCommand(::PublishingRemoveArgs) {
				name = Translations.Utility.NewsChannel.NewsPublishing.Remove.name
				description = Translations.Utility.NewsChannel.NewsPublishing.Remove.description

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					val translations = Translations.Utility.NewsChannel.NewsPublishing.Remove
					if (NewsChannelPublishingCollection().getAutoPublishingChannel(
							guild!!.id,
							arguments.channel.id
						) == null
					) {
						respond {
							content = translations.noAuto.translate(arguments.channel.mention)
						}
						return@action
					}

					NewsChannelPublishingCollection().removeAutoPublishingChannel(guild!!.id, arguments.channel.id)

					respond {
						content = translations.success.translate(arguments.channel.mention)
					}

					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, getGuild()!!)?.createEmbed {
						title = translations.embedTitle.translate()
						field {
							name = Translations.Utility.NewsChannel.NewsPublishing.embedChannelField.translate()
							value = arguments.channel.mention
						}
						footer {
							text = translations.removedBy.translate(user.asUserOrNull()?.username)
								"Removed by ${user.asUserOrNull()?.username}"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Utility.NewsChannel.NewsPublishing.List.name
				description = Translations.Utility.NewsChannel.NewsPublishing.List.description

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					val pagesObj = Pages()
					val channelsData = NewsChannelPublishingCollection().getAutoPublishingChannels(guild!!.id)

					val translations = Translations.Utility.NewsChannel.NewsPublishing.List

					if (channelsData.isEmpty()) {
						pagesObj.addPage(
							Page {
								description = translations.none.translate()
							}
						)
					} else {
						channelsData.chunked(10).forEach { channelDataChunk ->
							var response = ""
							channelDataChunk.forEach { data ->
								val channel = guild!!.getChannelOrNull(data.channelId)
								response +=
									"${channel?.mention ?: Translations.Basic.UnableTo.channel.translate()} (${channel?.name ?: ""}"
							}
							pagesObj.addPage(
								Page {
									title = translations.title.translate()
									description = translations.desc.translate()
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
				name = Translations.Utility.NewsChannel.NewsPublishing.RemoveAll.name
				description = Translations.Utility.NewsChannel.NewsPublishing.RemoveAll.description

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					if (NewsChannelPublishingCollection().getAutoPublishingChannels(guild!!.id).isEmpty()) {
						respond {
							content = Translations.Utility.NewsChannel.NewsPublishing.RemoveAll.noChannels.translate()
						}
					} else {
						NewsChannelPublishingCollection().clearAutoPublishingForGuild(guild!!.id)
						respond {
							content = Translations.Utility.NewsChannel.NewsPublishing.RemoveAll.success.translate()
						}
					}
				}
			}
		}
	}

	inner class PublishingSetArgs : Arguments() {
		val channel by channel {
			name = Translations.Utility.NewsChannel.NewsPublishing.Arguments.Channel.name
			description = Translations.Utility.NewsChannel.NewsPublishing.Arguments.Channel.description
			requiredChannelTypes = mutableSetOf(ChannelType.GuildNews)
		}
	}

	inner class PublishingRemoveArgs : Arguments() {
		val channel by channel {
			name = Translations.Utility.NewsChannel.NewsPublishing.Arguments.Channel.name
			description = Translations.Utility.NewsChannel.NewsPublishing.Arguments.Channel.description
			requiredChannelTypes = mutableSetOf(ChannelType.GuildNews)
		}
	}
}
