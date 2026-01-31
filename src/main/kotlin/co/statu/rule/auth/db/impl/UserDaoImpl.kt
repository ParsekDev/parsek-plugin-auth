package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.database.DBEntity.Companion.from
import co.statu.rule.database.annotation.Dao
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import java.util.*

@Dao
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class UserDaoImpl : UserDao() {

    override suspend fun init(jdbcPool: Pool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `email` String NOT NULL,
                            `permissionGroupId` Nullable(UUID),
                            `registeredIp` String NOT NULL,
                            `registerDate` Int64 NOT NULL,
                            `lastLoginDate` Int64 NOT NULL,
                            `lastActivityTime` Int64 DEFAULT 0,
                            `lastPanelActivityTime` Int64 DEFAULT 0,
                            `active` Bool DEFAULT true,
                            `additionalFields` String DEFAULT '{}'
                        ) ENGINE = MergeTree() order by `registerDate`;
            """
            )
            .execute()
            .coAwait()
    }

    override suspend fun add(
        user: User,
        jdbcPool: Pool
    ): UUID {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    user.id,
                    user.email,
                    user.permissionGroupId,
                    user.registeredIp,
                    user.registerDate,
                    user.lastLoginDate,
                    user.lastActivityTime,
                    user.lastPanelActivityTime,
                    user.active,
                    user.additionalFields.encode(),
                )
            )
            .coAwait()

        return user.id
    }

    override suspend fun isEmailExists(
        email: String,
        jdbcPool: Pool
    ): Boolean {
        val query =
            "SELECT COUNT(`email`) FROM `${getTablePrefix() + tableName}` where `email` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    email
                )
            )
            .coAwait()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun isAdditionalFieldUnique(
        additionalField: String,
        value: String,
        jdbcPool: Pool
    ): Boolean {
        val query = """
            SELECT field, count(*) AS occurrences
            FROM
                (
                    SELECT JSONExtractString(`additionalFields`, ?) AS field
                    FROM `${getTablePrefix() + tableName}`
                    ) AS extracted_field
            WHERE field = ?
            GROUP BY field
            HAVING occurrences > 0;
        """.trimIndent()

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    additionalField,
                    value
                )
            )
            .coAwait()

        return rows.toList().getOrNull(0) == null || rows.toList()[0].size() == 0
    }

    override suspend fun getUserIdFromEmail(
        email: String,
        jdbcPool: Pool
    ): UUID? {
        val query =
            "SELECT `id` FROM `${getTablePrefix() + tableName}` where `email` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(email))
            .coAwait()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun isActive(userId: UUID, jdbcPool: Pool): Boolean {
        val query =
            "SELECT COUNT(`email`) FROM `${getTablePrefix() + tableName}` WHERE `id` = ? and `active` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId,
                    true
                )
            )
            .coAwait()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun getByPermissionGroupId(
        permissionGroupId: UUID,
        limit: Long,
        jdbcPool: Pool
    ): List<User> {
        val query =
            "SELECT ${fields.toTableQuery()}  FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ? ${if (limit == -1L) "" else "LIMIT $limit"}"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(permissionGroupId))
            .coAwait()

        return rows.toEntities()
    }

    override suspend fun getPermissionGroupNameById(userId: UUID, jdbcPool: Pool): String? {
        val query = """SELECT p_group.name
                    FROM `${getTablePrefix() + tableName}` u
                    JOIN `${getTablePrefix()}permission_group` p_group ON u.permissionGroupId = p_group.id
                    WHERE u.id = ?"""

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(userId)
            )
            .coAwait()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getString(0)
    }

    override suspend fun getPermissionsById(userId: UUID, jdbcPool: Pool): List<Permission> {
        val query = """SELECT p.id, p.name
                    FROM `${getTablePrefix() + tableName}` u
                    JOIN `${getTablePrefix()}permission_group` p_group ON u.permissionGroupId = p_group.id
                    JOIN `${getTablePrefix()}permission_group_perms` p_group_perms ON p_group.id = p_group_perms.permissionGroupId
                    JOIN `${getTablePrefix()}permission` p ON p_group_perms.permissionId = p.id
                    WHERE u.id = ?"""

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(userId)
            )
            .coAwait()

        return Permission::class.from(rows)
    }

    override suspend fun updateLastActivityTime(userId: UUID, jdbcPool: Pool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `lastActivityTime` = ? WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    System.currentTimeMillis(),
                    userId
                )
            )
            .coAwait()
    }

    override suspend fun updateLastPanelActivityTime(userId: UUID, jdbcPool: Pool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `lastPanelActivityTime` = ? WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    System.currentTimeMillis(),
                    userId
                )
            )
            .coAwait()
    }

    override suspend fun getById(
        userId: UUID,
        jdbcPool: Pool
    ): User? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .coAwait()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun updateLastLoginDate(userId: UUID, jdbcPool: Pool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `lastLoginDate` = ? WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    System.currentTimeMillis(),
                    userId
                )
            )
            .coAwait()
    }

    override suspend fun getEmailFromUserId(userId: UUID, jdbcPool: Pool): String? {
        val query =
            "SELECT `email` FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .coAwait()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getString(0)
    }

    override suspend fun update(user: User, jdbcPool: Pool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `email` = ?, `permissionGroupId` = ?, `active` = ?, `additionalFields` = ? WHERE `id` = ?"

        val parameters = Tuple.tuple()

        parameters.addString(user.email)
        parameters.addUUID(user.permissionGroupId)
        parameters.addBoolean(user.active)
        parameters.addString(user.additionalFields.encode())

        parameters.addUUID(user.id)

        jdbcPool
            .preparedQuery(query)
            .execute(parameters)
            .coAwait()
    }
}