package io.zeko.db.sql.connections

import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import io.zeko.model.declarations.toMaps
import kotlinx.coroutines.delay
import org.joda.time.LocalDateTime
import java.io.InputStream
import java.lang.Exception
import java.math.BigDecimal
import java.net.URL
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp
import java.sql.ResultSet
import java.time.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

open class HikariDBSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: Connection
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as Connection
    }

    constructor(dbPool: DBPool, conn: DBConn, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as Connection
        this.throwOnDuplicate = throwOnDuplicate
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): Connection = rawConn

    protected fun throwDuplicateException(err: Exception) {
        if (this.throwOnDuplicate) {
            throwDuplicate(err)
        }
    }

    override suspend fun <A> once(operation: suspend (DBSession) -> A): A {
        try {
            val result: A = operation.invoke(this)
            return result
        } catch (e: Exception) {
            logger?.logError(e)
            throw e
        } finally {
            conn.close()
        }
    }

    override suspend fun <A> retry(numRetries: Int, delayTry: Long, operation: suspend (DBSession) -> A) {
        try {
            val result: A = operation.invoke(this)
        } catch (e: Exception) {
            if (e !is DuplicateKeyException) {
                if (numRetries > 0) {
                    if (delayTry > 0) {
                        delay(delayTry)
                    }
                    logger?.logRetry(numRetries, e)
                    retry(numRetries - 1, delayTry, operation)
                } else {
                    logger?.logError(e)
                    throw e
                }
            } else {
                logger?.logError(e)
                throw e
            }
        } finally {
            if (numRetries == 0) {
                conn.close()
            }
        }
    }

    override suspend fun <A> transaction(numRetries: Int, delayTry: Long, operation: suspend (DBSession) -> A) {
        try {
            conn.beginTx()
            operation.invoke(this)
            conn.commit()
        } catch (e: Exception) {
            if (e !is DuplicateKeyException) {
                if (numRetries > 0) {
                    conn.rollback()
                    conn.endTx()
                    if (delayTry > 0) {
                        delay(delayTry)
                    }
                    logger?.logRetry(numRetries, e)
                    transaction(numRetries - 1, delayTry, operation)
                } else {
                    conn.rollback()
                    logger?.logError(e)
                    throw e
                }
            } else {
                conn.rollback()
                logger?.logError(e)
                throw e
            }
        } finally {
            if (numRetries == 0) {
                conn.endTx()
                conn.close()
            }
        }
    }

    override suspend fun <A> transaction(operation: suspend (DBSession) -> A): A {
        try {
            conn.beginTx()
            val result: A = operation.invoke(this)
            conn.commit()
            return result
        } catch (e: Exception) {
            conn.rollback()
            logger?.logError(e)
            throw e
        } finally {
            conn.endTx()
            conn.close()
        }
    }

    override suspend fun <A> transactionOpen(operation: suspend (DBSession) -> A): A {
        try {
            conn.beginTx()
            val result: A = operation.invoke(this)
            conn.commit()
            return result
        } catch (e: Exception) {
            conn.rollback()
            logger?.logError(e)
            throw e
        } finally {
            conn.endTx()
        }
    }

    override suspend fun close() {
        conn.close()
    }

    override fun setQueryLogger(logger: DBLogger): DBSession {
        this.logger = logger
        return this
    }

    override fun getQueryLogger(): DBLogger? {
        return this.logger
    }

    private fun PreparedStatement.setParam(idx: Int, v: Any?) {
        if (v == null) {
            this.setObject(idx, null)
        } else {
            when (v) {
                is String -> this.setString(idx, v)
                is Byte -> this.setByte(idx, v)
                is Boolean -> this.setBoolean(idx, v)
                is Int -> this.setInt(idx, v)
                is Long -> this.setLong(idx, v)
                is Short -> this.setShort(idx, v)
                is Double -> this.setDouble(idx, v)
                is Float -> this.setFloat(idx, v)
                // if is zoned, stored in DB datetime field as the UTC date time,
                // when doing Entity prop type mapping with datetime_utc, it will be auto converted to ZonedDateTime with value in DB consider as UTC value
                is ZonedDateTime -> {
                    val systemZoneDateTime = v.withZoneSameInstant(ZoneId.of("UTC"))
                    val local = systemZoneDateTime.toLocalDateTime()
                    this.setTimestamp(idx, Timestamp(ZonedDateTime.of(local, ZoneId.systemDefault()).toInstant().toEpochMilli()))
                }
                is OffsetDateTime -> this.setTimestamp(idx, Timestamp(Date.from(v.toInstant()).time))
                is Instant -> this.setTimestamp(idx, Timestamp(Date.from(v).time))
                is java.time.LocalDateTime -> this.setTimestamp(idx, Timestamp(LocalDateTime.parse(v.toString()).toDate().time))
                is LocalDate -> this.setDate(idx, java.sql.Date(org.joda.time.LocalDate.parse(v.toString()).toDate().time))
                is LocalTime -> this.setTime(idx, java.sql.Time(org.joda.time.LocalTime.parse(v.toString()).toDateTimeToday().millis))
                is org.joda.time.DateTime -> this.setTimestamp(idx, Timestamp(v.toDate().time))
                is org.joda.time.LocalDateTime -> this.setTimestamp(idx, Timestamp(v.toDate().time))
                is org.joda.time.LocalDate -> this.setDate(idx, java.sql.Date(v.toDate().time))
                is org.joda.time.LocalTime -> this.setTime(idx, java.sql.Time(v.toDateTimeToday().millis))
                is java.util.Date -> this.setTimestamp(idx, Timestamp(v.time))
                is java.sql.Timestamp -> this.setTimestamp(idx, v)
                is java.sql.Time -> this.setTime(idx, v)
                is java.sql.Date -> this.setTimestamp(idx, Timestamp(v.time))
                is java.sql.SQLXML -> this.setSQLXML(idx, v)
                is ByteArray -> this.setBytes(idx, v)
                is InputStream -> this.setBinaryStream(idx, v)
                is BigDecimal -> this.setBigDecimal(idx, v)
                is java.sql.Array -> this.setArray(idx, v)
                is URL -> this.setURL(idx, v)
                else -> this.setObject(idx, v)
            }
        }
    }

    protected fun prepareStatement(sql: String, params: List<Any?>, mode: Int = -1): PreparedStatement {
        logger?.logQuery(sql, params)
        val stmt: PreparedStatement = if (mode > -1) rawConn.prepareStatement(sql, mode) else rawConn.prepareStatement(sql)
        if (!params.isNullOrEmpty()) {
            params.forEachIndexed { index, value ->
                stmt.setParam(index + 1, value)
            }
        }
        return stmt
    }

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        val stmt = prepareStatement(sql, params)
        var affectedRows = 0
        try {
            affectedRows = stmt.executeUpdate()
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            logger?.logUnsupportedSql(err)
            return 0
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeStatement) stmt.close()
            if (closeConn) conn.close()
        }
        return affectedRows
    }

    override suspend fun insert(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val stmt = prepareStatement(sql, params, dbPool.getInsertStatementMode())
        var affectedRows = 0
        try {
            affectedRows = stmt.executeUpdate()
            if (affectedRows == 0) {
                return listOf<Void>()
            }
            if (dbPool.getInsertStatementMode() == Statement.RETURN_GENERATED_KEYS) {
                val keys = arrayListOf<Any>()
                val generatedKeys = stmt.generatedKeys
                if (generatedKeys.next()) {
                    keys.add(generatedKeys.getObject(1))
                }
                return keys
            }
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            // Apache ignite insert will return this due to Auto generated keys are not supported.
            logger?.logUnsupportedSql(err)
            return listOf<Void>()
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeStatement) stmt.close()
            if (closeConn) conn.close()
        }
        return listOf<Void>()
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val stmt = prepareStatement(sql, params)
        val rs = stmt.executeQuery()
        val rows = resultSetToObjects(rs, dataClassHandler)
        rs.close()
        if (closeStatement) stmt.close()
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): ResultSet {
        val stmt = prepareStatement(sql, params)
        val rs = stmt.executeQuery()
        return rs
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        val stmt = prepareStatement(sql, params)
        val rs = stmt.executeQuery()
        val result = rs.toMaps(columns)
        rs.close()
        stmt.close()
        if (closeConn) conn.close()
        return result
    }

    override suspend fun query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        logger?.logQuery(sql)
        val stmt: Statement = rawConn.createStatement()
        val rs = stmt.executeQuery(sql)
        val rows = resultSetToObjects(rs, dataClassHandler)
        rs.close()
        if (closeStatement) stmt.close()
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun query(sql: String): ResultSet {
        logger?.logQuery(sql)
        val stmt: Statement = rawConn.createStatement()
        val rs = stmt.executeQuery(sql)
        return rs
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>>  {
        logger?.logQuery(sql)
        val stmt: Statement = rawConn.createStatement()
        val rs = stmt.executeQuery(sql)
        val result = rs.toMaps(columns)
        rs.close()
        stmt.close()
        if (closeConn) conn.close()
        return result
    }

    private fun resultSetToObjects(rs: ResultSet, dataClassHandler: (dataMap: Map<String, Any?>) -> Any): List<*> {
        val md = rs.metaData
        val columns = md.columnCount
        val rows = arrayListOf<Any>()
        while (rs.next()) {
            val dataMap = HashMap<String, Any?>(columns)
            for (i in 1..columns) {
                dataMap[md.getColumnName(i).toLowerCase()] = rs.getObject(i)
            }
            val user = dataClassHandler(dataMap)
            rows.add(user)
        }
        return rows
    }
}
