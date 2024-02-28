package co.statu.rule.auth.db.model

import co.statu.rule.database.DBEntity
import java.util.*

data class PermissionGroup(
    val id: UUID = UUID.randomUUID(),
    val name: String
) : DBEntity()