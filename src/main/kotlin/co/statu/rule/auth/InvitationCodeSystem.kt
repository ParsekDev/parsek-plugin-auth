package co.statu.rule.auth

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.db.dao.InvitationCodeDao
import co.statu.rule.auth.db.impl.InvitationCodeDaoImpl
import co.statu.rule.auth.db.model.InvitationCode
import co.statu.rule.auth.error.InvalidInviteCode
import co.statu.rule.database.DatabaseManager
import org.slf4j.Logger
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class InvitationCodeSystem(
    private val authPlugin: AuthPlugin,
    private val logger: Logger,
) {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val jdbcPool by lazy {
        databaseManager.getConnectionPool()
    }

    private val config by lazy {
        pluginConfigManager.config
    }

    private val invitationConfig by lazy {
        config.invitationConfig
    }

    private val invitationCodeDao: InvitationCodeDao = InvitationCodeDaoImpl()

    internal suspend fun start() {
        if (!invitationConfig.enabled) {
            return
        }

        val count = invitationCodeDao.count(jdbcPool)

        if (count < invitationConfig.defaultAmount) {
            val neededAmount = invitationConfig.defaultAmount - count

            val invitationCodes = List(neededAmount.toInt()) {
                InvitationCode()
            }

            invitationCodeDao.addAll(invitationCodes, jdbcPool)

            logger.info("Generated and saved {} amount of invitation codes.", neededAmount)
        }
    }

    suspend fun validateCode(code: String, email: String) {
        if (!invitationConfig.enabled) {
            return
        }

        val invitationCode = invitationCodeDao.byCode(code, jdbcPool) ?: throw InvalidInviteCode()

        if (invitationCode.expiresAt != null && invitationCode.expiresAt < System.currentTimeMillis()) {
            throw InvalidInviteCode()
        }

        if (invitationCode.usageLimit != null && invitationCode.usedByEmails.filter { it != email }.size >= invitationCode.usageLimit) {
            throw InvalidInviteCode()
        }

        if (invitationCode.usedByEmails.any { it == email }) {
            return
        }

        invitationCode.usedByEmails += email

        invitationCodeDao.update(invitationCode, jdbcPool)
    }
}