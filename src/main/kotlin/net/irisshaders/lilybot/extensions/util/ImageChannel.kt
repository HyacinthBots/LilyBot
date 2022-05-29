package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import com.kotlindiscord.kord.extensions.utils.respond
import com.soywiz.klock.seconds
import dev.kord.common.entity.Permission
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.delay
import net.irisshaders.lilybot.utils.DatabaseHelper.deleteImageChannel
import net.irisshaders.lilybot.utils.DatabaseHelper.getImageChannels
import net.irisshaders.lilybot.utils.DatabaseHelper.setImageChannel
import net.irisshaders.lilybot.utils.configPresent

/**
 * The class the holds the systems that allow a guild to set a channel as image only.
 *
 * @since 3.3.0
 */
class ImageChannel : Extension() {
	override val name = "imagechannel"

	override suspend fun setup() {
		/**
		 * Image channel commands.
		 * @author NoComment1105
		 * @since 3.3.0
		 */
		ephemeralSlashCommand {
			name = "image-channel"
			description = "The parent command for image channel setting"

			/**
			 * The command that sets the image channel.
			 */
			ephemeralSubCommand {
				name = "set"
				description = "Set a channel as an image channel"

				check {
					anyGuild()
					configPresent()
					hasPermission(Permission.ManageGuild)
				}

				action {
					setImageChannel(guild!!.id, channel.asChannel().id)

					respond {
						content = "Set channel as image only"
					}
				}
			}

			/**
			 * The command that unsets the image channel.
			 */
			ephemeralSubCommand {
				name = "unset"
				description = "Unset a channel as an image channel"

				check {
					anyGuild()
					configPresent()
					hasPermission(Permission.ManageGuild)
				}

				action {
					deleteImageChannel(guild!!.id, channel.asChannel().id)

					respond {
						content = "Unset channel as image only"
					}
				}
			}

			/**
			 * The command that returns a list of all image channels for a particular guild.
			 */
			ephemeralSubCommand {
				name = "list"
				description = "List all image channels in the guild"

				check {
					anyGuild()
				}

				action {
					val imageChannels = getImageChannels(guild!!.id)
					var channels = ""

					imageChannels.forEach {
						channels += "<#${it.channelId}> "
					}

					respond {
						embed {
							title = "Image channels"
							description = "Here are the image only channels in this guild."
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
				failIf { event.message.author.isNullOrBot() } // Fail if null in case an ephemeral message is created
			}

			action {
				val imageChannels = getImageChannels(guildFor(event)!!.id)

				for (i in imageChannels) {
					// If there are no attachments to the message and the channel we're in is an image channel
					if (event.message.channelId == i.channelId && event.message.attachments.isEmpty()) {
						// We delay to give the message a chance to populate with an embed, if it is a link to imgur etc.
						delay(0.1.seconds.millisecondsLong)
						if (event.message.embeds.isEmpty()) { // If there is still no embed, we delete the message
							// and explain why
							val response = event.message.respond {
								content = "This channel is for images only!"
							}

							event.message.delete()

							try {
								// Delete the explanation after 3 seconds. If an exception is thrown, the
								// message has already been deleted
								response.delete(3.seconds.millisecondsLong)
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
