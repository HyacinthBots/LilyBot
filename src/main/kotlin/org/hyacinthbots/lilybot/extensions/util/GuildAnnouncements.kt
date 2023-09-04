package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalSnowflake
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.getFirstUsableChannel
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.getSystemChannelWithPerms

class GuildAnnouncements : Extension() {
	override val name = "guild-announcements"

	override suspend fun setup() {
		ephemeralSlashCommand(::GuildAnnouncementArgs, ::GuildAnnouncementModal) {
			name = "announcement"
			description = "Send an announcement to all guilds Lily is in"

			guild(TEST_GUILD_ID)
			requirePermission(Permission.Administrator)

			check {
				hasPermission(Permission.Administrator)
			}

			action { modal ->
				var response: EphemeralFollowupMessage? = null
				response = respond {
					content = "Would you like to send this message? " +
							if (arguments.targetGuild == null) {
								"It will be delivered to all servers this bot is in."
							} else {
								"It will be delivered to your specified target guild: ${arguments.targetGuild}"
							}
					components {
						ephemeralButton {
							label = "Yes"
							style = ButtonStyle.Success

							action ButtonAction@{
								response?.edit {
									content = "Message sent!"
									components { removeAll() }
								}

								if (arguments.targetGuild != null) {
									val guild = event.kord.getGuildOrNull(arguments.targetGuild!!)
									if (guild == null) {
										respond { content = "Target guild not found" }
										return@ButtonAction
									}

									val channel = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)
										?: getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild)
										?: getSystemChannelWithPerms(guild)
										?: getFirstUsableChannel(guild)

									if (channel == null) {
										respond { content = "Couldn't find an available channel to send a message in" }
										return@ButtonAction
									}

									channel.createEmbed {
										title = modal?.header?.value
										description = modal?.body?.value
										color = Color(0x7B52AE)
										footer {
											text = "This message was delivered to this server alone"
										}
									}
								} else {
								    event.kord.guilds.toList().chunked(15).forEach { chunk ->
										for (i in chunk) {
											val channel =
												getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, i)
													?: getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, i)
													?: getSystemChannelWithPerms(i)
													?: getFirstUsableChannel(i)
													?: continue

											try {
												channel.createEmbed {
													title = modal?.header?.value
													description = modal?.body?.value
													color = Color(0x7B52AE)
													footer {
														text = "Sent by ${user.asUserOrNull()?.username}"
													}
												}
											} catch (_: KtorRequestException) {
												continue
											}
										}
									}
								}
							}
						}

						ephemeralButton {
							label = "No"
							style = ButtonStyle.Danger

							action {
								response?.edit {
									content = "Message not sent."
									components { removeAll() }
								}
							}
						}
					}
				}
			}
		}
	}

	inner class GuildAnnouncementArgs : Arguments() {
		val targetGuild by optionalSnowflake {
			name = "target-guild"
			description = "The guild to send the announcement too"
		}
	}

	inner class GuildAnnouncementModal : ModalForm() {
		override var title = "Send an announcement"

		val header = lineText {
			label = "Announcement Header"
			placeholder = "Version 7.6.5!"
			maxLength = 250
			required = false
		}

		val body = paragraphText {
			label = "Announcement Body"
			placeholder = "We're happy to announce Lily is now written in Rust! It turns out we really like crabs"
			maxLength = 1750
			required = true
		}
	}
}
