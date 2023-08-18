package org.hyacinthbots.lilybot.database.codec

import kotlinx.datetime.DateTimePeriod
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class DateTimePeriodCodec : Codec<DateTimePeriod> {
	override fun encode(writer: BsonWriter, value: DateTimePeriod?, encoderContext: EncoderContext?) {
		writer.writeString(value.toString())
	}

	override fun getEncoderClass(): Class<DateTimePeriod> = DateTimePeriod::class.java

	override fun decode(reader: BsonReader, decoderContext: DecoderContext?): DateTimePeriod =
		DateTimePeriod.parse(reader.readString())
}
