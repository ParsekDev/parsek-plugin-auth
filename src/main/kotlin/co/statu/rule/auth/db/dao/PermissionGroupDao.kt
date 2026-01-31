package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.PermissionGroup
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import java.util.*

abstract class PermissionGroupDao : Dao<PermissionGroup>(PermissionGroup::class) {
    abstract suspend fun isThereByName(
        name: String,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun isThere(
        permissionGroup: PermissionGroup,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun isThereById(
        id: UUID,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun add(
        permissionGroup: PermissionGroup,
        jdbcPool: Pool
    ): UUID

    abstract suspend fun getPermissionGroupById(
        id: UUID,
        jdbcPool: Pool
    ): PermissionGroup?

    abstract suspend fun getPermissionGroupIdByName(
        name: String,
        jdbcPool: Pool
    ): UUID?

    abstract suspend fun getPermissionGroups(
        jdbcPool: Pool
    ): List<PermissionGroup>

    abstract suspend fun deleteById(
        id: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun update(
        permissionGroup: PermissionGroup,
        jdbcPool: Pool
    )

    abstract suspend fun getByListOfId(
        idList: Set<UUID>,
        jdbcPool: Pool
    ): Map<UUID, PermissionGroup>
}