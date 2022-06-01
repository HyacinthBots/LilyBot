package net.irisshaders.lilybot.utils.docs

/**
 * This is the class for the data from the commanddocs.toml file, used for creating the help command.
 *
 * @property command The list of all commands in the TOML file
 *
 * @since 3.3.0
 */
data class CommandDocs(
	val command: List<Command>,
	val header: List<Headers>
) {
	/**
	 * This class represents the structure of the commanddocs.toml file. These are all the values that will be
	 * read from the file is this order.
	 *
	 * @property overview Usually the group of the command
	 * @property name The name of the command providing help for
	 * @property args The arguments of the command
	 * @property result The result of the command
	 * @property permissions The required permissions to run the command
	 *
	 * @since 3.3.0
	 */
	data class Command(
		val overview: String,
		val name: String,
		val args: String,
		val result: String,
		val permissions: String?
	)

	/**
	 * This class represents the format required to add headers to the generated Markdown docs.
	 *
	 * @property title The title of the header
	 * @property description The description tied to the header
	 */
	data class Headers(
		val title: String,
		val description: String
	)
}
