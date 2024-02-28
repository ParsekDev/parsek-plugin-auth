package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.PermissionGroupPerms
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class PermissionGroupPermsDao : Dao<PermissionGroupPerms>(PermissionGroupPerms::class) {
    abstract suspend fun getPermissionGroupPerms(
        jdbcPool: JDBCPool
    ): List<PermissionGroupPerms>

    abstract suspend fun getPermissionGroupPermsByPermissionId(
        permissionId: UUID,
        jdbcPool: JDBCPool
    ): List<PermissionGroupPerms>

    abstract suspend fun doesPermissionGroupHavePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun addPermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun removePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun removePermissionGroup(
        permissionGroupId: UUID,
        jdbcPool: JDBCPool
    )
}