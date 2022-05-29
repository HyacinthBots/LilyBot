package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.create.embed
import net.irisshaders.lilybot.utils.DatabaseHelper.deleteImageChannel
import net.irisshaders.lilybot.utils.DatabaseHelper.getImageChannels
import net.irisshaders.lilybot.utils.DatabaseHelper.setImageChannel
import net.irisshaders.lilybot.utils.configPresent

class ImageChannel : Extension() {
	override val name = "imagechannel"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "image-channel"
			description = "The parent command for image channel setting"

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
	}
}
