package org.hyacinthbots.lilybot.utils.docs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.hyacinthbots.lilybot.commandDocs
import org.hyacinthbots.lilybot.docFile
import java.io.IOException
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * This Object contains the functions that enable the generation of the Markdown docs file that is seen in
 * `/docs/commands.md`.
 *
 * @since 3.3.0
 */
object DocsGenerator {
	val docsLogger = KotlinLogging.logger("Docs Generator")

	/**
	 * The function clears the documents file, allowing it to have the new documentation written in by [writeNewDocs].
	 * This function returns itself if it is being run in a production environment, since it is not needed there.
	 *
	 * @param environment Whether you're running the bot in production or development
	 * @author NoComment1105
	 * @since 3.3.0
	 * @see writeNewDocs
	 */
	suspend inline fun clearDocs(environment: String) {
		if (environment == "production") {
			docsLogger.info("Production environment detected. Skipping clearing docs")
			return
		}

		docsLogger.debug("Starting the clearing of existing documents!")
		if (!docFile.exists()) {
			docsLogger.error("File not found! Not clearing docs file!")
			return
		}

		docsLogger.debug("Clearing file contents...")
		val writer = docFile.bufferedWriter()
		withContext(Dispatchers.IO) {
			writer.write("")
			writer.flush()
			writer.close()
		}
		docsLogger.info("Cleared old documents!")
	}

	/**
	 * The function writes the documents into the file cleared by [clearDocs].
	 * This function returns itself if it is being run in a production environment, since it is not needed there.
	 *
	 * @param environment Whether you're running the bot in production or development
	 * @author NoComment1105
	 * @since 3.3.0
	 * @see clearDocs
	 */
	suspend inline fun writeNewDocs(environment: String) {
		if (environment == "production") {
			docsLogger.info("Production environment detected. Skipping writing docs")
			return
		}

		docsLogger.debug("Starting the writing of documents!")
		if (!docFile.exists()) { // If the documents file doesn't exist, for what ever reason...
			docsLogger.warn("Docs file not found! Attempting to create file...")
			try {
				docFile.createFile() // Create it!
			} catch (e: IOException) {
				docsLogger.error("Failed to create file! Not writing documents") // Print an error when it can't be made
				return
			}
			docsLogger.info("File created successfully")
		}

		docsLogger.debug("Writing new documents...")
		val writer = docFile.bufferedWriter() // Write the documents.
		withContext(Dispatchers.IO) {
			writer.write(
			    "# Commands List\n\nThe following is a list of commands, their arguments, and what they " +
					"do.\n\n---\n\n"
			)
			commandDocs!!.command.forEach {
				if (it.name.isNullOrEmpty() && it.result.isNullOrEmpty() && it.permissions.isNullOrEmpty() &&
					it.args.isNullOrEmpty() && it.category.isNotEmpty() && it.description!!.isNotEmpty()
				) {
					docsLogger.debug("Writing command header")
					writer.write(
						"\n## ${it.category}\n" +
								"${it.description}\n\n"
					)
					writer.flush()
				} else {
					docsLogger.debug("Writing command doc")
					writer.write(
						"### Name: `${it.name}`\n" +
								"**Arguments**:\n${it.args ?: "None"}\n\n" +
								"**Result**: ${it.result}\n\n" +
								"**Required Permissions**: `${it.permissions ?: "None"}`\n\n" +
								"**Command category**: `${it.category}`\n\n" +
								"---\n\n"
					)
					writer.flush()
				}
			}
			writer.flush()
			writer.close()
		}
		docsLogger.info("New Documents written successfully")
	}
}
