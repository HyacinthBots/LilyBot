package org.hyacinthbots.lilybot.database

/**
 * This class stores the name of a collection, so it can be referenced easily, avoiding string duplication
 *
 * An example of how it should be used can be found below. The name property used in the get collection line comes from
 * this class
 * ```kt
 * class MagicCollection : KordExKoinComponent {
 * 	private val db: Database by inject()
 *
 * 	val collection = db.mainDatabase.getCollection<MagicCollectionData>(name)
 *
 * 	suspend fun get(): List<MagicCollectionData> = collection.find().toList()
 *
 * 	companion object : Collection("magicCollection")
 * }
 * ```
 *
 * @property name The name of the collection
 */
abstract class Collection(val name: String)
