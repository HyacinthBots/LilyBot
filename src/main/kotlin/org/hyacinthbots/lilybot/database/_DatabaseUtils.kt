package org.hyacinthbots.lilybot.database

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson

private fun <T : Any> MongoCollection<T>.find(vararg filters: Bson?): FindFlow<T> = find(and(*filters))

suspend fun <T : Any> MongoCollection<T>.findOne(filter: Bson): T? = find(filter).firstOrNull()

suspend fun <T : Any> MongoCollection<T>.findOne(vararg filters: Bson?): T? = find(*filters).firstOrNull()

suspend fun <T : Any> MongoCollection<T>.deleteOne() = deleteOne(Filters.empty())

// TODO Make more cool useful functions
