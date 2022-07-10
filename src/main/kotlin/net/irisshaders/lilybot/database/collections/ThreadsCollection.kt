package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.ThreadData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object stores all the functions for interacting with the [Threads Database][ThreadData]. This object contains
 * the functions for getting threads, based on different criteria such as owner, thread ID, or all of them. It also has
 * functions for setting and removing threads.
 *
 * @since 4.0.0
 * @see getThread
 * @see getAllThreads
 * @see getOwnerThreads
 * @see setThreadOwner
 * @see removeThread
 */
class ThreadsCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ThreadData>()

	/**
	 * Using the provided [inputThreadId] the thread is returned.
	 *
	 * @param inputThreadId The ID of the thread you wish to find the owner for
	 *
	 * @return null or the thread
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun getThread(inputThreadId: Snowflake): ThreadData? =
		collection.findOne(ThreadData::threadId eq inputThreadId)

	/**
	 * Gets all threads into a list and return them to the user.
	 *
	 * @author NoComment1105
	 * @since 3.4.1
	 */
	suspend inline fun getAllThreads(): List<ThreadData> =
		collection.find().toList()

	/**
	 * Using the provided [inputOwnerId] the list of threads that person owns is returned from the database.
	 *
	 * @param inputOwnerId The ID of the member whose threads you wish to find
	 *
	 * @return null or a list of threads the member owns
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun getOwnerThreads(inputOwnerId: Snowflake): List<ThreadData> =
		collection.find(ThreadData::ownerId eq inputOwnerId).toList()

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
	suspend inline fun removeThread(inputThreadId: Snowflake) =
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
}
