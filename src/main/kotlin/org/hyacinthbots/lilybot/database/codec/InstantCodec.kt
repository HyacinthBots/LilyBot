package org.hyacinthbots.lilybot.database.codec

import kotlinx.datetime.Instant
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class InstantCodec : Codec<Instant> {
	override fun encode(writer: BsonWriter, value: Instant?, encoderContext: EncoderContext?) {
		writer.writeString(value.toString())
	}

	override fun getEncoderClass(): Class<Instant> = Instant::class.java

	override fun decode(reader: BsonReader, decoderContext: DecoderContext): Instant = Instant.parse(reader.readString())
}
