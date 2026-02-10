package org.hyacinthbots.lilybot.extensions.logging.config

import dev.kordex.core.components.forms.ModalForm
import lilybot.i18n.Translations

class PublicLoggingModal : ModalForm() {
    override var title = Translations.Config.Logging.Modal.title

    val joinMessage = paragraphText {
        label = Translations.Config.Logging.Modal.JoinMessage.label
        placeholder = Translations.Config.Logging.Modal.JoinMessage.placeholder
        required = true
    }

    val leaveMessage = paragraphText {
        label = Translations.Config.Logging.Modal.LeaveMessage.label
        placeholder = Translations.Config.Logging.Modal.LeaveMessage.placeholder
        required = true
    }

    val ping = lineText {
        label = Translations.Config.Logging.Modal.Ping.label
        placeholder = Translations.Config.Logging.Modal.Ping.placeholder
    }
}
