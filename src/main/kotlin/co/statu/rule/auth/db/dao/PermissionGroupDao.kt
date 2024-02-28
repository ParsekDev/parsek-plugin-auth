package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.PermissionGroup
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class PermissionGroupDao : Dao<PermissionGroup>(PermissionGroup::class) {
    abstract suspend fun isThereByName(
        name: String,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun isThere(
        permissionGroup: PermissionGroup,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun isThereById(
        id: UUID,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun add(
        permissionGroup: PermissionGroup,
        jdbcPool: JDBCPool
    ): UUID

    abstract suspend fun getPermissionGroupById(
        id: UUID,
        jdbcPool: JDBCPool
    ): PermissionGroup?

    abstract suspend fun getPermissionGroupIdByName(
        name: String,
        jdbcPool: JDBCPool
    ): UUID?

    abstract suspend fun getPermissionGroups(
        jdbcPool: JDBCPool
    ): List<PermissionGroup>

    abstract suspend fun deleteById(
        id: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun update(
        permissionGroup: PermissionGroup,
        jdbcPool: JDBCPool
    )

    abstract suspend fun getByListOfId(
        idList: Set<UUID>,
        jdbcPool: JDBCPool
    ): Map<UUID, PermissionGroup>
}