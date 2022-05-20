package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI

class ConfigExtension : Extension() {
	override val name: String = "config"
	override val bundle: String = "config"

	@Suppress("UnnecessaryOptInAnnotation")
	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		configCommand()
	}
}
