package net.irisshaders.lilybot.extensions.util

import com.github.jezza.Toml
import com.github.jezza.TomlArray
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.PublicResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import kotlinx.datetime.Clock
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

private val commandDocs: TomlTable = Toml.from(Files.newInputStream(Path("docs/commanddocs.toml")))

class InfoCommands : Extension() {
	override val name = "infocommands"

	override suspend fun setup() {
		val pagesObj = Pages()

		val commands: TomlArray = commandDocs.get("command") as TomlArray
		publicSlashCommand {
			name = "help"
			description = "Help for Lily's commands!"

			action {
				for (cmds in commands) {
					val cmd = cmds as TomlTable

					pagesObj.addPage(
						Page {
							title = cmd.get("overview") as String
							description = cmd.get("name") as String
							field {
								name = "Arguments:"
								value = cmd.get("args") as String
							}
							field {
								name = "Permisions:"
								value = cmd.get("permissions") as String
							}
							field {
								name = "Result"
								value = cmd.get("result") as String
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
					locale = Locale.UK,
					interaction = interactionResponse
				)

				paginator.send()
			}
		}
	}
}
