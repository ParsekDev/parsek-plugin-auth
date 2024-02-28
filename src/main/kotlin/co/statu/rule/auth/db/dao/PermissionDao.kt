package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.Permission
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class PermissionDao : Dao<Permission>(Permission::class) {
    abstract suspend fun isTherePermission(
        permission: Permission,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun isTherePermissionById(
        id: UUID,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun add(
        permission: Permission,
        jdbcPool: JDBCPool
    )

    abstract suspend fun getPermissionId(
        permission: Permission,
        jdbcPool: JDBCPool
    ): UUID

    abstract suspend fun getPermissionById(
        id: UUID,
        jdbcPool: JDBCPool
    ): Permission?

    abstract suspend fun getPermissions(
        jdbcPool: JDBCPool
    ): List<Permission>
}