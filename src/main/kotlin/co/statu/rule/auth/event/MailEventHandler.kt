package co.statu.rule.auth.event

import co.statu.rule.auth.AuthPlugin
import co.statu.rule.mail.MailManager
import co.statu.rule.mail.event.MailEventListener

class MailEventHandler : MailEventListener {
    override fun onReady(mailManager: MailManager) {
        AuthPlugin.mailManager = mailManager
    }
}