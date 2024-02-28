package co.statu.rule.auth.event

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.ParsekEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.AuthPlugin.Companion.logger
import co.statu.rule.auth.GoogleRecaptcha
import co.statu.rule.auth.InvitationCodeSystem
import co.statu.rule.auth.config.migration.*
import co.statu.rule.auth.provider.AuthProvider

class ParsekEventHandler : ParsekEventListener {
    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        AuthPlugin.pluginConfigManager = PluginConfigManager(
            configManager,
            AuthPlugin.INSTANCE,
            AuthConfig::class.java,
            logger,
            listOf(
                ConfigMigration1to2(),
                ConfigMigration2to3(),
                ConfigMigration3to4(),
                ConfigMigration4to5(),
                ConfigMigration5to6()
            ),
            listOf("auth")
        )

        logger.info("Initialized plugin config")

        val googleRecaptcha = GoogleRecaptcha.create(AuthPlugin.pluginConfigManager)

        AuthPlugin.invitationCodeSystem = InvitationCodeSystem.create(
            AuthPlugin.pluginConfigManager,
            AuthPlugin.databaseManager,
            logger
        )

        val authProvider = AuthProvider.create(
            AuthPlugin.databaseManager,
            AuthPlugin.tokenProvider,
            AuthPlugin.pluginConfigManager,
            googleRecaptcha,
            AuthPlugin.i18nSystem,
            AuthPlugin.webClient
        )

        AuthPlugin.authProvider = authProvider
    }
}