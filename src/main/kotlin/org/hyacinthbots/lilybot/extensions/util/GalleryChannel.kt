package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import com.soywiz.klock.seconds
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.delay
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.getUtilityLogOrFirst

/**
 * The class the holds the systems that allow a guild to set a channel as a gallery channel.
 *
 * @since 3.3.0
 */
class GalleryChannel : Extension() {
	override val name = "gallery-channel"

	override suspend fun setup() {
		/**
		 * Gallery channel commands.
		 * @author NoComment1105
		 * @since 3.3.0
		 */
		ephemeralSlashCommand {
			name = "gallery-channel"
			description = "The parent command for image channel setting"

			/**
			 * The command that sets the gallery channel.
			 */
			ephemeralSubCommand {
				name = "set"
				description = "Set a channel as a gallery channel"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requireBotPermissions(Permission.ManageChannels, Permission.ManageMessages)
					botHasChannelPerms(Permissions(Permission.ManageChannels, Permission.ManageMessages))
				}

				action {
					val utilityLog =
						getLoggingChannelWithPerms(
							guild!!.asGuild(),
							getUtilityLogOrFirst(guild)?.id,
							ConfigType.UTILITY,
							interactionResponse
						)
							?: return@action

					GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
						if (channel.asChannel().id == it.channelId) {
							respond {
								content = "This channel is already a gallery channel!"
							}
							return@action
						}
					}

					GalleryChannelCollection().setChannel(guild!!.id, channel.asChannel().id)

					respond {
						content = "Set channel as gallery channel."
					}

					utilityLog.createEmbed {
						title = "New Gallery channel"
						description = "${channel.mention} was added as a Gallery channel"
						footer {
							text = "Requested by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
						color = DISCORD_GREEN
					}
				}
			}

			/**
			 * The command that unsets the gallery channel.
			 */
			ephemeralSubCommand {
				name = "unset"
				description = "Unset a channel as a gallery channel."

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requireBotPermissions(Permission.ManageChannels)
					botHasChannelPerms(Permissions(Permission.ManageChannels))
				}

				action {
					val utilityLog =
						getLoggingChannelWithPerms(
							guild!!.asGuild(),
							getUtilityLogOrFirst(guild)?.id,
							ConfigType.UTILITY,
							interactionResponse
						)
							?: return@action

					var channelFound = false

					GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
						if (channel.asChannel().id == it.channelId) {
							GalleryChannelCollection().removeChannel(guild!!.id, channel.asChannel().id)
							channelFound = true
						}
					}

					if (channelFound) {
						respond {
							content = "Unset channel as gallery channel."
						}

						utilityLog.createEmbed {
							title = "Removed Gallery channel"
							description = "${channel.mention} was removed as a Gallery channel"
							footer {
								text = "Requested by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
							color = DISCORD_RED
						}
					} else {
						respond {
							content = "This channel is not a gallery channel!"
						}
					}
				}
			}

			/**
			 * The command that returns a list of all image channels for a particular guild.
			 */
			ephemeralSubCommand {
				name = "list"
				description = "List all gallery channels in the guild"

				check {
					anyGuild()
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					var channels = ""

					GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
						channels += "<#${it.channelId}> "
					}

					respond {
						embed {
							title = "Gallery channels"
							description = "Here are the gallery channels in this guild."
							field {
								name = "Channels:"
								value = if (channels != "") channels.replace(" ", "\n") else "No channels found!"
							}
						}
					}
				}
			}
		}

		/**
		 * The event for checking a channel.
		 * @since 3.3.0
		 */
		event<MessageCreateEvent> {
			check {
				anyGuild()
				failIf { event.message.author?.id == kord.selfId }
			}

			action {
				GalleryChannelCollection().getChannels(event.guildId!!).forEach {
					// If there are no attachments to the message and the channel we're in is an image channel
					if (event.message.channelId == it.channelId && event.message.attachments.isEmpty()) {
						// We delay to give the message a chance to populate with an embed, if it is a link to imgur etc.
						delay(0.25.seconds.millisecondsLong)
						if (event.message.embeds.isEmpty()) { // If there is still no embed, we delete the message
							// and explain why
							if (event.message.type != MessageType.Default && event.message.type != MessageType.Reply) {
								event.message.delete()
								return@action
							}

							val response = event.message.respond {
								content = "This channel is for images only!"
							}

							event.message.delete()

							try {
								// Delete the explanation after 3 seconds. If an exception is thrown, the
								// message has already been deleted
								response.delete(2.5.seconds.millisecondsLong)
							} catch (e: EntityNotFoundException) {
								// The message that we're attempting to delete has already been deleted.
							}
						}
					}
				}
			}
		}
	}
}
