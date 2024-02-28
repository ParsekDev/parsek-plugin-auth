package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.database.DBEntity.Companion.from
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class UserDaoImpl : UserDao() {

    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `name` String NOT NULL,
                            `surname` String NOT NULL,
                            `fullName` String NOT NULL,
                            `email` String NOT NULL,
                            `permissionGroupId` Nullable(UUID),
                            `registeredIp` String NOT NULL,
                            `registerDate` Int64 NOT NULL,
                            `lastLoginDate` Int64 NOT NULL,
                            `lastActivityTime` Int64 DEFAULT 0,
                            `lastPanelActivityTime` Int64 DEFAULT 0,
                            `lang` String DEFAULT 'EN',
                            `active` Bool DEFAULT true
                        ) ENGINE = MergeTree() order by `registerDate`;
            """
            )
            .execute()
            .await()
    }

    override suspend fun add(
        user: User,
        jdbcPool: JDBCPool
    ): UUID {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    user.id,
                    user.name,
                    user.surname,
                    user.fullName,
                    user.email,
                    user.permissionGroupId,
                    user.registeredIp,
                    user.registerDate,
                    user.lastLoginDate,
                    user.lastActivityTime,
                    user.lastPanelActivityTime,
                    user.lang,
                    user.active
                )
            )
            .await()

        return user.id
    }

    override suspend fun isEmailExists(
        email: String,
        jdbcPool: JDBCPool
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
            .await()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun getUserIdFromEmail(
        email: String,
        jdbcPool: JDBCPool
    ): UUID? {
        val query =
            "SELECT `id` FROM `${getTablePrefix() + tableName}` where `email` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(email))
            .await()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun isActive(userId: UUID, jdbcPool: JDBCPool): Boolean {
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
            .await()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun getEmailsByPermissionGroupId(
        permissionGroupId: UUID,
        limit: Long,
        jdbcPool: JDBCPool
    ): List<String> {
        val query =
            "SELECT `email` FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ? ${if (limit == -1L) "" else "LIMIT $limit"}"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(permissionGroupId))
            .await()

        val listOfEmails = mutableListOf<String>()

        rows.forEach { row ->
            listOfEmails.add(row.getString(0))
        }

        return listOfEmails
    }

    override suspend fun getPermissionGroupNameById(userId: UUID, jdbcPool: JDBCPool): String? {
        val query = """SELECT p_group.name
                    FROM `${getTablePrefix() + tableName}` u
                    JOIN `${getTablePrefix()}permission_group` p_group ON u.permissionGroupId = p_group.id
                    WHERE u.id = ?"""

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(userId)
            )
            .await()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getString(0)
    }

    override suspend fun getPermissionsById(userId: UUID, jdbcPool: JDBCPool): List<Permission> {
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
            .await()

        return Permission::class.from(rows)
    }

    override suspend fun updateLastActivityTime(userId: UUID, jdbcPool: JDBCPool) {
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
            .await()
    }

    override suspend fun updateLastPanelActivityTime(userId: UUID, jdbcPool: JDBCPool) {
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
            .await()
    }

    override suspend fun getById(
        userId: UUID,
        jdbcPool: JDBCPool
    ): User? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun updateLastLoginDate(userId: UUID, jdbcPool: JDBCPool) {
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
            .await()
    }

    override suspend fun getEmailFromUserId(userId: UUID, jdbcPool: JDBCPool): String? {
        val query =
            "SELECT `email` FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .await()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getString(0)
    }

    override suspend fun update(user: User, jdbcPool: JDBCPool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `name` = ?, `surname` = ?, `fullName` = ?, `email` = ?, `permissionGroupId` = ?, `lang` = ?, `active` = ? WHERE `id` = ?"

        val parameters = Tuple.tuple()

        parameters.addString(user.name)
        parameters.addString(user.surname)
        parameters.addString(user.fullName)
        parameters.addString(user.email)
        parameters.addUUID(user.permissionGroupId)
        parameters.addString(user.lang)
        parameters.addBoolean(user.active)

        parameters.addUUID(user.id)

        jdbcPool
            .preparedQuery(query)
            .execute(parameters)
            .await()
    }
}