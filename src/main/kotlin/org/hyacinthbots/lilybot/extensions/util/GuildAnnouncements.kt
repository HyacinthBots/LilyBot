package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.UnsafeInteractionContext
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
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
				val modal = event.interaction.modal("Send an announcement", "announcementModal") {
					actionRow {
						textInput(TextInputStyle.Short, "title", "Announcement Title") {
							placeholder = "Version 5.0.0!"
						}
					}

					actionRow {
						textInput(TextInputStyle.Paragraph, "body", "Announcement Body") {
							placeholder = "This is a big update!"
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

				val title = interaction.textInputs["title"]!!.value!!
				val body = interaction.textInputs["body"]!!.value!!
				val footer = "Sent by ${user.asUser().tag}" // Useless, just needed for length calculations
				val modalResponse = interaction.deferEphemeralResponse()

				if (title.length + body.length + footer.length > 2048) {
					modalResponse.respond {
						content = "Your announcement is just too long! I can only make announcements up to 2048 " +
								"characters. Please try again with a small announcement, or make two separate " +
								"announcements"
					}
					return@action
				}

				if (title.isEmpty() && body.isEmpty()) {
					modalResponse.respond {
						content = "Your announcement cannot be completely empty!"
					}
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

								sendMessage(title, body, user)
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

	/**
	 * Sends a message to each guild containing a given body and title.
	 *
	 * @param announcementTitle The title of the announcement embed
	 * @param announcementBody The title of the announcement body
	 * @param user The user who sent the announcement
	 *
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	private suspend fun UnsafeInteractionContext.sendMessage(
		announcementTitle: String,
		announcementBody: String,
		user: UserBehavior
	) {
		val footer = "Sent by ${user.asUser().tag}"
		event.kord.guilds.toList().chunked(15).forEach { chunk ->
			for (it in chunk) {
				val channel = it.getSystemChannel()
				if (channel?.getEffectivePermissions(event.kord.selfId)
						?.contains(Permissions(Permission.SendMessages, Permission.EmbedLinks)) == false
				) {
					continue
				}

				if (announcementTitle.isEmpty() && announcementBody.isEmpty()) {
					return // This case should theoretically never be possible, but just in case, catch it
				} else if (announcementTitle.isEmpty()) {
					channel?.createEmbed {
						description = announcementBody
						color = Color(0x7B52AE)
						footer {
							text = footer
						}
					}
				} else if (announcementBody.isEmpty()) {
					channel?.createEmbed {
						title = announcementTitle
						color = Color(0x7B52AE)
						footer {
							text = footer
						}
					}
				} else if (announcementTitle.length + announcementBody.length + footer.length >= 1000) {
					channel?.createEmbed {
						title = announcementTitle
						description = announcementBody.substring(0, announcementBody.length - announcementTitle.length)
						color = Color(0x7B52AE)
					}
					channel?.createEmbed {
						description = announcementBody.substring(
							announcementBody.length - announcementTitle.length,
							announcementBody.length
						)
						color = Color(0x7B52AE)
						footer {
							text = footer
						}
					}
				} else {
					channel?.createEmbed {
						title = announcementTitle
						description = announcementBody
						color = Color(0x7B52AE)
						footer {
							text = footer
						}
					}
				}
			}
		}
	}
}
