package co.statu.rule.auth

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.db.dao.InvitationCodeDao
import co.statu.rule.auth.db.model.InvitationCode
import co.statu.rule.auth.error.InvalidInviteCode
import co.statu.rule.database.Dao.Companion.get
import co.statu.rule.database.DatabaseManager
import org.slf4j.Logger

class InvitationCodeSystem private constructor(
    private val pluginConfigManager: PluginConfigManager<AuthConfig>,
    private val databaseManager: DatabaseManager,
    private val logger: Logger
) {
    companion object {
        internal suspend fun create(
            pluginConfigManager: PluginConfigManager<AuthConfig>,
            databaseManager: DatabaseManager,
            logger: Logger
        ): InvitationCodeSystem {
            val invitationCodeSystem = InvitationCodeSystem(
                pluginConfigManager,
                databaseManager,
                logger
            )

            invitationCodeSystem.start()

            return invitationCodeSystem
        }
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

    private val invitationCodeDao by lazy {
        get<InvitationCodeDao>(AuthPlugin.tables)
    }

    private suspend fun start() {
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