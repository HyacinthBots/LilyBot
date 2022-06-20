package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.tables.ThreadData
import org.litote.kmongo.eq

object ThreadsDatabase {
	/**
	 * Using the provided [inputThreadId] the thread is returned.
	 *
	 * @param inputThreadId The ID of the thread you wish to find the owner for
	 *
	 * @return null or the thread
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun getThread(inputThreadId: Snowflake): ThreadData? {
		val collection = database.getCollection<ThreadData>()
		return collection.findOne(ThreadData::threadId eq inputThreadId)
	}

	/**
	 * Gets all threads into a list and return them to the user.
	 *
	 * @author NoComment1105
	 * @since 3.4.1
	 */
	suspend inline fun getAllThreads(): List<ThreadData> {
		val collection = database.getCollection<ThreadData>()
		return collection.find().toList()
	}

	/**
	 * Using the provided [inputOwnerId] the list of threads that person owns is returned from the database.
	 *
	 * @param inputOwnerId The ID of the member whose threads you wish to find
	 *
	 * @return null or a list of threads the member owns
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun getOwnerThreads(inputOwnerId: Snowflake): List<ThreadData> {
		val collection = database.getCollection<ThreadData>()
		return collection.find(ThreadData::ownerId eq inputOwnerId).toList()
	}

	/**
	 * Add or update the ownership of the given [inputThreadId] to the given [newOwnerId].
	 *
	 * @param inputThreadId The ID of the thread you wish to update or set the owner for
	 * @param newOwnerId The new owner of the thread
	 * @param preventArchiving Whether to stop the thread from being archived or not
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun setThreadOwner(
		inputThreadId: Snowflake,
		newOwnerId: Snowflake,
		preventArchiving: Boolean = false
	) {
		val collection = database.getCollection<ThreadData>()
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
		collection.insertOne(ThreadData(inputThreadId, newOwnerId, preventArchiving))
	}

	/**
	 * This function deletes the ownership data stored in the database for the given [inputThreadId].
	 *
	 * @param inputThreadId The ID of the thread to delete
	 *
	 * @author henkelmax
	 * @since 3.2.2
	 */
	suspend inline fun removeThread(inputThreadId: Snowflake) {
		val collection = database.getCollection<ThreadData>()
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
	}
}
