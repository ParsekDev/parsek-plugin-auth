package co.statu.rule.auth.db.model

import co.statu.rule.database.DBEntity
import java.util.*

data class PermissionGroupPerms(
    val id: UUID = UUID.randomUUID(),
    val permissionId: UUID,
    val permissionGroupId: UUID
) : DBEntity()