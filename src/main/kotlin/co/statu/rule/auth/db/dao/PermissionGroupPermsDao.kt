package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.PermissionGroupPerms
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import java.util.*

abstract class PermissionGroupPermsDao : Dao<PermissionGroupPerms>(PermissionGroupPerms::class) {
    abstract suspend fun getPermissionGroupPerms(
        jdbcPool: Pool
    ): List<PermissionGroupPerms>

    abstract suspend fun getPermissionGroupPermsByPermissionId(
        permissionId: UUID,
        jdbcPool: Pool
    ): List<PermissionGroupPerms>

    abstract suspend fun doesPermissionGroupHavePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun addPermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun removePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun removePermissionGroup(
        permissionGroupId: UUID,
        jdbcPool: Pool
    )
}