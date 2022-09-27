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

				@Suppress("UnusedPrivateMember")
				val title = interaction.textInputs["title"]!!.value!!

				@Suppress("UnusedPrivateMember")
				val body = interaction.textInputs["body"]!!.value!!
				val modalResponse = interaction.deferEphemeralResponse()

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

									sendMessage(title, body, user)
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

	@Suppress("UnusedPrivateMember")
	private suspend fun UnsafeInteractionContext.sendMessage(
        announcementTitle: String,
        announcementBody: String,
        user: UserBehavior
    ) {
		event.kord.guilds.toList().chunked(20).forEach { chunk ->
			chunk.forEach {
				val channel = it.getSystemChannel()
				channel?.getEffectivePermissions(event.kord.selfId)
					?.contains(Permissions(Permission.SendMessages, Permission.EmbedLinks)) ?: return

				channel.createEmbed {
					title = announcementTitle
					description = announcementBody
					color = Color(0x7B52AE)
					footer {
						text = "This announcement was sent by ${user.asUser().tag}"
					}
				}
			}
		}
	}
}
