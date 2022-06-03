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
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.GalleryChannelData
import net.irisshaders.lilybot.utils.configPresent
import org.litote.kmongo.eq

/**
 * The class the holds the systems that allow a guild to set a channel as a gallery channel.
 *
 * @since 3.3.0
 */
class GalleryChannel : Extension() {
	override val name = "gallery-channel"

	override suspend fun setup() {
		/**
		 * This variable is a cached variable for gallery channels, present to avoid polling the database every message
		 * sent.
		 */
		var galleryChannels = DatabaseHelper.getGalleryChannels()

		/**
		 * gallery channel commands.
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
					configPresent()
					hasPermission(Permission.ManageGuild)
				}

				action {
					// Using the global var, find guild channels for the given guildId and iterate through them to
					// check for the presence of the channel and return if it is present
					val guildGalleryChannels =
						galleryChannels.find(GalleryChannelData::guildId eq guildFor(event)!!.id).toList()
					guildGalleryChannels.forEach {
						if (channel.asChannel().id == it.channelId) {
							respond {
								content = "This channel is already a gallery channel!"
							}
							return@action
						}
					}

					DatabaseHelper.setGalleryChannel(guild!!.id, channel.asChannel().id)

					// Update the global var
					galleryChannels = DatabaseHelper.getGalleryChannels()

					respond {
						content = "Set channel as gallery channel."
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
					configPresent()
					hasPermission(Permission.ManageGuild)
				}

				action {
					var channelFound = false

					val guildGalleryChannels =
						galleryChannels.find(GalleryChannelData::guildId eq guildFor(event)!!.id).toList()
					guildGalleryChannels.forEach {
						if (channel.asChannel().id == it.channelId) {
							DatabaseHelper.deleteGalleryChannel(guild!!.id, channel.asChannel().id)
							// Update the global var
							galleryChannels = DatabaseHelper.getGalleryChannels()
							channelFound = true
						}
					}

					if (channelFound) {
						respond {
							content = "Unset channel as gallery channel."
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
				}

				action {
					var channels = ""

					val guildGalleryChannels =
						galleryChannels.find(GalleryChannelData::guildId eq guildFor(event)!!.id).toList()
					guildGalleryChannels.forEach {
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
				failIf { event.message.author.isNullOrBot() } // Fail if null in case an ephemeral message is created
			}

			action {
				val guildGalleryChannels =
					galleryChannels.find(GalleryChannelData::guildId eq guildFor(event)!!.id).toList()

				for (i in guildGalleryChannels) {
					// If there are no attachments to the message and the channel we're in is an image channel
					if (event.message.channelId == i.channelId && event.message.attachments.isEmpty()) {
						// We delay to give the message a chance to populate with an embed, if it is a link to imgur etc.
						delay(0.25.seconds.millisecondsLong)
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
