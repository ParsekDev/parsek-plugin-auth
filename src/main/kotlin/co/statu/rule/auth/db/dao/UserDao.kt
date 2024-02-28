package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class UserDao : Dao<User>(User::class) {
    abstract suspend fun add(
        user: User,
        jdbcPool: JDBCPool
    ): UUID

    abstract suspend fun isEmailExists(
        email: String,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun getUserIdFromEmail(
        email: String,
        jdbcPool: JDBCPool
    ): UUID?

    abstract suspend fun isActive(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun getEmailsByPermissionGroupId(
        permissionGroupId: UUID,
        limit: Long,
        jdbcPool: JDBCPool
    ): List<String>

    abstract suspend fun getPermissionGroupNameById(
        userId: UUID,
        jdbcPool: JDBCPool
    ): String?

    abstract suspend fun getPermissionsById(
        userId: UUID,
        jdbcPool: JDBCPool
    ): List<Permission>

    abstract suspend fun updateLastActivityTime(
        userId: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun updateLastPanelActivityTime(
        userId: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun getById(
        userId: UUID,
        jdbcPool: JDBCPool
    ): User?

    abstract suspend fun updateLastLoginDate(
        userId: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun getEmailFromUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): String?

    abstract suspend fun update(
        user: User,
        jdbcPool: JDBCPool
    )
}