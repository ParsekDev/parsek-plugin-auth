package co.statu.rule.auth.db.model

import co.statu.rule.auth.util.InvitationCodeGenerator
import co.statu.rule.database.DBEntity
import java.util.*

class InvitationCode(
    val id: UUID = UUID.randomUUID(),
    val code: String = InvitationCodeGenerator.generate(),
    var usedByEmails: Array<String> = arrayOf(),
    val usageLimit: Long? = 1,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : DBEntity()