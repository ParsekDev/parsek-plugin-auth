package co.statu.rule.auth.event

import co.statu.parsek.PluginEventManager
import co.statu.parsek.api.annotation.EventListener
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.CoreEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthFieldManager
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.InvitationCodeSystem
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

    private val authFieldManager by lazy {
        authPlugin.pluginBeanContext.getBean(AuthFieldManager::class.java)
    }

    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        val pluginConfigManager = PluginConfigManager(
            authPlugin,
            AuthConfig::class.java
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

        authPlugin.registerSingletonGlobal(authFieldManager)

        authFieldManager.init()
    }
}