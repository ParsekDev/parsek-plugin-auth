package co.statu.rule.auth.event

import co.statu.parsek.PluginEventManager
import co.statu.parsek.api.annotation.EventListener
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.CoreEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.InvitationCodeSystem
import co.statu.rule.auth.config.migration.*
import co.statu.rule.auth.provider.AuthProvider
import org.slf4j.Logger

@EventListener
class CoreEventHandler(
    private val authPlugin: AuthPlugin,
    private val logger: Logger
) : CoreEventListener {
    private val invitationCodeSystem by lazy {
        authPlugin.pluginBeanContext.getBean(InvitationCodeSystem::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        val pluginConfigManager = PluginConfigManager(
            configManager,
            authPlugin,
            AuthConfig::class.java,
            listOf(
                ConfigMigration1to2(),
                ConfigMigration2to3(),
                ConfigMigration3to4(),
                ConfigMigration4to5(),
                ConfigMigration5to6(),
                ConfigMigration6to7()
            ),
            listOf("auth")
        )

        authPlugin.pluginBeanContext.beanFactory.registerSingleton(
            pluginConfigManager.javaClass.name,
            pluginConfigManager
        )

        logger.info("Initialized plugin config")

        invitationCodeSystem.start()

        AuthPlugin.authProvider = authProvider

        authPlugin.registerSingletonGlobal(authProvider)

        val handlers = PluginEventManager.getEventListeners<AuthEventListener>()

        handlers.forEach {
            it.onReady(authProvider)
        }
    }
}