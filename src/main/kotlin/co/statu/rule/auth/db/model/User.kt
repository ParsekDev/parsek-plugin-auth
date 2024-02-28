package co.statu.rule.auth.db.model

import co.statu.rule.database.DBEntity
import java.util.*

data class User(
    var id: UUID = UUID.randomUUID(),
    var name: String,
    var surname: String,
    var fullName: String = "$name $surname",
    var email: String,
    var permissionGroupId: UUID? = null,
    var registeredIp: String? = null,
    var registerDate: Long = System.currentTimeMillis(),
    var lastLoginDate: Long = System.currentTimeMillis(),
    var lastActivityTime: Long = System.currentTimeMillis(),
    var lastPanelActivityTime: Long = System.currentTimeMillis(),
    var lang: String = "EN",
    var active: Boolean = true
) : DBEntity()