package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import java.util.*

abstract class UserDao : Dao<User>(User::class) {
    abstract suspend fun add(
        user: User,
        jdbcPool: Pool
    ): UUID

    abstract suspend fun isEmailExists(
        email: String,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun isAdditionalFieldUnique(
        additionalField: String,
        value: String,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun getUserIdFromEmail(
        email: String,
        jdbcPool: Pool
    ): UUID?

    abstract suspend fun isActive(
        userId: UUID,
        jdbcPool: Pool
    ): Boolean

    abstract suspend fun getByPermissionGroupId(
        permissionGroupId: UUID,
        limit: Long,
        jdbcPool: Pool
    ): List<User>

    abstract suspend fun getPermissionGroupNameById(
        userId: UUID,
        jdbcPool: Pool
    ): String?

    abstract suspend fun getPermissionsById(
        userId: UUID,
        jdbcPool: Pool
    ): List<Permission>

    abstract suspend fun updateLastActivityTime(
        userId: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun updateLastPanelActivityTime(
        userId: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun getById(
        userId: UUID,
        jdbcPool: Pool
    ): User?

    abstract suspend fun updateLastLoginDate(
        userId: UUID,
        jdbcPool: Pool
    )

    abstract suspend fun getEmailFromUserId(
        userId: UUID,
        jdbcPool: Pool
    ): String?

    abstract suspend fun update(
        user: User,
        jdbcPool: Pool
    )
}