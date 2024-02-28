package co.statu.rule.auth.event

import co.statu.rule.auth.AuthPlugin
import co.statu.rule.plugins.i18n.I18nSystem
import co.statu.rule.plugins.i18n.event.I18nEventListener

class I18nEventHandler : I18nEventListener {
    override fun onReady(i18nSystem: I18nSystem) {
        AuthPlugin.i18nSystem = i18nSystem
    }
}