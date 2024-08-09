package org.hyacinthbots.lilybot.extensions.logging.config

import com.kotlindiscord.kord.extensions.components.forms.ModalForm

class PublicLoggingModal : ModalForm() {
	override var title = "Public logging configuration"

	val joinMessage = paragraphText {
		label = "What would you like sent when a user joins"
		placeholder = "Welcome to the server!"
		required = true
	}

	val leaveMessage = paragraphText {
		label = "What would you like sent when a user leaves"
		placeholder = "Adiós amigo!"
		required = true
	}

	val ping = lineText {
		label = "Type `yes` to ping new users when they join"
		placeholder = "Defaults to false if input is invalid or not `yes`"
	}
}
