package org.hyacinthbots.lilybot.database.codec

import com.kotlindiscord.kord.extensions.storage.StorageType
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

// FIXME Currently broken idk how to fix
class StorageTypeCodec : Codec<StorageType> {
	override fun encode(writer: BsonWriter, value: StorageType?, encoderContext: EncoderContext?) {
		writer.writeString(value.toString())
	}

	override fun getEncoderClass(): Class<StorageType> = StorageType::class.java

	override fun decode(reader: BsonReader, decoderContext: DecoderContext): StorageType =
		when (val string = reader.readString()) {
			StorageType.Config.type -> StorageType.Config
			StorageType.Data.type -> StorageType.Data

			else -> error("Unknown storage type: $string")
		}
}
