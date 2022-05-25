package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.PublicResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.kord.common.Locale
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.commandDocs

class InfoCommands : Extension() {
	override val name = "infocommands"

	override suspend fun setup() {
		val pagesObj = Pages()

		publicSlashCommand {
			name = "help"
			description = "Help for Lily's commands!"

			action {
				commandDocs!!.command.forEach {
					pagesObj.addPage(
						Page {
							title = it.overview
							description = it.name
							field {
								name = "Arguments:"
								value = it.args
							}
							field {
								name = "Permisions:"
								value = it.permissions ?: "none"
							}
							field {
								name = "Result"
								value = it.result
							}
							timestamp = Clock.System.now()
						}
					)
				}

				val paginator = PublicResponsePaginator(
					pages = pagesObj,
					keepEmbed = true,
					owner = event.interaction.user,
					timeoutSeconds = 500,
					locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
					interaction = interactionResponse
				)

				paginator.send()
			}
		}
	}
}
