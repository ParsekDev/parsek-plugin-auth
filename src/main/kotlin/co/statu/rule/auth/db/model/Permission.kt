package co.statu.rule.auth.db.model

import co.statu.rule.database.DBEntity
import java.util.*

data class Permission(
    val id: UUID = UUID.randomUUID(),
    val name: String
) : DBEntity()