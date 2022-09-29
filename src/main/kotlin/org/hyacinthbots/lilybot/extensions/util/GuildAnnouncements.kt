package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.getFirstUsableChannel
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.getSystemChannelWithPerms
import kotlin.time.Duration.Companion.seconds

@OptIn(UnsafeAPI::class)
class GuildAnnouncements : Extension() {
	override val name = "guild-announcements"

	override suspend fun setup() {
		unsafeSlashCommand {
			name = "announcement"
			description = "Send an announcement to all guilds Lily is in"

			initialResponse = InitialSlashCommandResponse.None

			guild(TEST_GUILD_ID)

			check {
				hasPermission(Permission.Administrator)
			}

			action {
				val footer = "Sent by ${user.asUser().tag}" // Useless, just needed for length calculations

				val modal = event.interaction.modal("Send an announcement", "announcementModal") {
					actionRow {
						textInput(TextInputStyle.Short, "header", "Announcement Header") {
							placeholder = "Version 7.6.5!"
							allowedLength = IntRange(1, 250)
							required = false
						}
					}
					actionRow {
						textInput(TextInputStyle.Paragraph, "body", "Announcement Body") {
							placeholder = "We're happy to announce Lily is now written in Rust! " +
									"It turns out we really like crabs."
							allowedLength = IntRange(1, 1750 - footer.length)
							required = false
						}
					}
				}

				val interaction =
					modal.kord.waitFor<ModalSubmitInteractionCreateEvent>(300.seconds.inWholeMilliseconds) {
						interaction.modalId == "announcementModal"
					}?.interaction

				if (interaction == null) {
					modal.createEphemeralFollowup {
						embed {
							description = "Announcement timed out"
						}
					}
					return@action
				}

				val body = interaction.textInputs["body"]!!.value
				val header = interaction.textInputs["header"]!!.value
				val modalResponse = interaction.deferEphemeralResponse()

				if (body.isNullOrEmpty() && header.isNullOrEmpty()) {
					modalResponse.respond {
						content = "Your announcement cannot be completely empty!"
					}
					return@action
				}

				var response: EphemeralMessageInteractionResponse? = null

				response = modalResponse.respond {
					content = "Would you like to send this message? It will be delivered to all servers this bot is in."
					components {
						ephemeralButton(0) {
							label = "Yes"
							style = ButtonStyle.Success

							action {
								response?.edit {
									content = "Message sent!"
									components { removeAll() }
								}

								event.kord.guilds.toList().chunked(15).forEach { chunk ->
									for (it in chunk) {
										val channel =
											getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, it)
												?: getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, it)
												?: getSystemChannelWithPerms(it)
												?: getFirstUsableChannel(it)
												?: return@forEach

										channel.createEmbed {
											title = header
											description = body
											color = Color(0x7B52AE)
											footer {
												text = footer
											}
										}
									}
								}
							}
						}

						ephemeralButton(0) {
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
}
