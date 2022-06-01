package net.irisshaders.lilybot.utils.docs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.irisshaders.lilybot.commandDocs
import net.irisshaders.lilybot.docFile
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
	private val logger = KotlinLogging.logger("Docs Generator")

	/**
	 * The function clears the documents file, allowing it to have the new documentation written in by [writeNewDocs].
	 * This function returns itself if it is being run in a production environment, since it is not needed there.
	 *
	 * @param environment Whether you're running the bot in production or development
	 * @author NoComment1105
	 * @since 3.3.0
	 * @see writeNewDocs
	 */
	suspend fun clearDocs(environment: String) {
		if (environment == "production") {
			logger.info("Production environment detected. Skipping clearing docs")
			return
		}

		logger.info("Starting the clearing of existing documents!")
		if (!docFile.exists()) {
			logger.error("File not found! Not clearing docs file!")
			return
		}

		logger.info("Clearing file contents...")
		val writer = docFile.bufferedWriter()
		withContext(Dispatchers.IO) {
			writer.write("")
			writer.flush()
			writer.close()
		}
		logger.info("Cleared documents!")
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
	suspend fun writeNewDocs(environment: String) {
		if (environment == "production") {
			logger.info("Production environment detected. Skipping writing docs")
			return
		}
		logger.info("Starting the writing of documents!")
		if (!docFile.exists()) { // If the documents file doesn't exist, for what ever reason...
			logger.warn("Docs file not found! Attempting to create file...")
			try {
				docFile.createFile() // Create it!
			} catch (e: IOException) {
				logger.error("Failed to create file! Not writing documents") // Print an error when it can't be made
				return
			}
			logger.info("File created successfully")
		}

		logger.info("Writing documents...")
		val writer = docFile.bufferedWriter() // Write the documents.
		withContext(Dispatchers.IO) {
			writer.write(
			    "# Commands List\n\nThe following is a list of commands, their arguments, and what they " +
					"do.\n\n---\n\n"
			)
			commandDocs!!.command.forEach {
				writer.write(
					"### Name: `${it.name}`\n" +
							"**Arguments**:\n${it.args}\n\n" +
							"**Result**: ${it.result}\n\n" +
							"**Required Permissions**: `${it.permissions ?: "none"}`\n\n" +
							"**Command category**: `${it.category}`\n\n" +
							"---\n\n"
				)
			}
			writer.flush()
			writer.close()
		}
		logger.info("Documents written successfully")
	}
}
