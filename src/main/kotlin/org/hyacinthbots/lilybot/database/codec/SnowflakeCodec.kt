package org.hyacinthbots.lilybot.database.codec

import dev.kord.common.entity.Snowflake
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class SnowflakeCodec : Codec<Snowflake> {
	override fun encode(writer: BsonWriter, value: Snowflake?, encoderContext: EncoderContext?) {
		writer.writeString(value.toString())
	}

	override fun getEncoderClass(): Class<Snowflake> = Snowflake::class.java

	override fun decode(reader: BsonReader, decoderContext: DecoderContext): Snowflake = Snowflake(reader.readString())
}
