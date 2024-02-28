package co.statu.rule.auth.event

import co.statu.parsek.api.event.RouterEventListener
import co.statu.parsek.model.Route
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.route.account.DeleteAccountAPI
import co.statu.rule.auth.route.auth.*
import co.statu.rule.auth.route.profile.GetProfileAPI
import co.statu.rule.auth.route.profile.UpdateProfileAPI

class RouterEventHandler : RouterEventListener {
    override fun onInitRouteList(routes: MutableList<Route>) {
        val databaseManager = AuthPlugin.databaseManager
        val pluginConfigManager = AuthPlugin.pluginConfigManager
        val mailManager = AuthPlugin.mailManager
        val authProvider = AuthPlugin.authProvider
        val tokenProvider = AuthPlugin.tokenProvider
        val pluginEventManager = AuthPlugin.INSTANCE.context.pluginEventManager
        val i18nSystem = AuthPlugin.i18nSystem
        val invitationCodeSystem = AuthPlugin.invitationCodeSystem

        routes.addAll(
            listOf(
                GetCredentialsAPI(authProvider, databaseManager),
                LogoutAPI(authProvider, pluginConfigManager),
                RegisterAPI(authProvider, tokenProvider, databaseManager, pluginConfigManager),
                SendMagicLinkAPI(authProvider, mailManager, pluginConfigManager, databaseManager, invitationCodeSystem),
                VerifyMagicLinkAPI(authProvider, tokenProvider, pluginConfigManager, databaseManager),
                DeleteAccountAPI(mailManager, pluginConfigManager, authProvider, databaseManager),
                GetProfileAPI(authProvider, databaseManager, pluginEventManager),
                UpdateProfileAPI(authProvider, databaseManager, i18nSystem)
            )
        )
    }
}