package org.hyacinthbots.lilybot.database

import dev.kord.common.entity.Snowflake
import org.bson.BsonInvalidOperationException
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

object SnowflakeCodec : Codec<Snowflake> {
	override fun encode(writer: BsonWriter, value: Snowflake, encoderContext: EncoderContext) {
		writer.writeInt64(value.value.toLong())
	}

	override fun decode(reader: BsonReader, decoderContext: DecoderContext): Snowflake = try {
		Snowflake(reader.readInt64())
	} catch (_: BsonInvalidOperationException) {
		Snowflake(reader.readString())
	}

	override fun getEncoderClass(): Class<Snowflake> = Snowflake::class.java
}
