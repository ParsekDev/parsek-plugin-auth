package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.Permission
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import java.util.*

abstract class PermissionDao : Dao<Permission>(Permission::class) {
    abstract suspend fun isTherePermission(
        permission: Permission,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun isTherePermissionById(
        id: UUID,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun add(
        permission: Permission,
        jdbcPool: Pool
    )

    abstract suspend fun getPermissionId(
        permission: Permission,
        jdbcPool: Pool
    ): UUID

    abstract suspend fun getPermissionById(
        id: UUID,
        jdbcPool: Pool
    ): Permission?

    abstract suspend fun getPermissions(
        jdbcPool: Pool
    ): List<Permission>
}