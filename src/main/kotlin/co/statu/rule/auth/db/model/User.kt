package co.statu.rule.auth.db.model

import co.statu.rule.database.DBEntity
import io.vertx.core.json.JsonObject
import java.util.*

data class User(
    var id: UUID = UUID.randomUUID(),
    var email: String,
    var permissionGroupId: UUID? = null,
    var registeredIp: String? = null,
    var registerDate: Long = System.currentTimeMillis(),
    var lastLoginDate: Long = System.currentTimeMillis(),
    var lastActivityTime: Long = System.currentTimeMillis(),
    var lastPanelActivityTime: Long = System.currentTimeMillis(),
    var active: Boolean = true,
    var additionalFields: JsonObject = JsonObject()
) : DBEntity()