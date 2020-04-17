package io.zeko.db.sql.connections

import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.mysql.MySQLQueryResult
import io.zeko.model.declarations.toMaps
import kotlinx.coroutines.delay
import java.lang.Exception
import java.time.*
import java.util.LinkedHashMap

class JasyncDBSession : DBSession {
    var conn: DBConn
    var dbPool: DBPool
    var rawConn: Any

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw()
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): ConnectionPool<*> = rawConn as ConnectionPool<*>

    override suspend fun close() {
        conn.close()
    }

    fun suspendingConn(): SuspendingConnection {
        val raw = conn.raw()
        if (raw is ConnectionPool<*>) {
            return raw.asSuspending
        }
        return raw as SuspendingConnection
    }

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        var updateRes: QueryResult?
        var affectedRows = 0
        try {
            updateRes = suspendingConn().sendPreparedStatement(sql, convertParams(params))
            affectedRows = updateRes.rowsAffected.toInt()
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            return affectedRows
        } finally {
            if (closeConn) conn.close()
        }
        return affectedRows
    }

    private fun convertParams(params: List<Any?>): List<Any?> {
        if (!params.isNullOrEmpty()) {
            val converted = arrayListOf<Any?>()
            //Jasync accepts only Joda date time instead of java.time
            params.forEach { value ->
                val v = when (value) {
                    is LocalDate -> org.joda.time.LocalDate(value.year, value.monthValue, value.dayOfMonth)
                    is LocalDateTime -> org.joda.time.LocalDateTime(value.year, value.monthValue, value.dayOfMonth, value.hour, value.minute, value.second, value.nano / 1000000)
                    is LocalTime -> org.joda.time.LocalTime(value.hour, value.minute, value.second, value.nano / 1000000)
                    is Instant -> org.joda.time.Instant(value.toEpochMilli())
                    // if is zoned, stored in DB datetime field as the UTC date time,
                    // when doing Entity prop type mapping with datetime_utc, it will be auto converted to ZonedDateTime with value in DB consider as UTC value
                    is ZonedDateTime -> org.joda.time.LocalDateTime.parse(value.toInstant().toString().removeSuffix("Z"))
                    else -> value
                }
                converted.add(v)
            }
            return converted
        }
        return params
    }

    override suspend fun insert(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): List<*> {
        var updateRes: QueryResult? = null
        try {
            updateRes = suspendingConn().sendPreparedStatement(sql, convertParams(params))
            val affectedRows = updateRes.rowsAffected.toInt()
            println("affectedRows $affectedRows")
            if (affectedRows == 0) {
                return listOf<Void>()
            }
            if (updateRes is MySQLQueryResult) {
                return listOf(updateRes.lastInsertId)
            }
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            // Apache ignite insert will return this due to Auto generated keys are not supported.
            if (updateRes != null ) {
                if (updateRes is MySQLQueryResult) {
                    return listOf(updateRes.lastInsertId)
                }
            }
        } finally {
            if (closeConn) conn.close()
        }
        return listOf<Void>()
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val res = suspendingConn().sendPreparedStatement(sql, convertParams(params))
        val rows = resultSetToObjects(res.rows, dataClassHandler)
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): QueryResult {
        return suspendingConn().sendPreparedStatement(sql, convertParams(params))
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        val res = suspendingConn().sendPreparedStatement(sql, convertParams(params))
        val rs = res.rows.toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }

    override suspend fun query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val res = suspendingConn().sendQuery(sql)
        val rows = resultSetToObjects(res.rows, dataClassHandler)
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun query(sql: String): QueryResult {
        return suspendingConn().sendQuery(sql)
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        val res = suspendingConn().sendQuery(sql)
        val rs = res.rows.toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }

    private fun resultSetToObjects(rs: ResultSet, dataClassHandler: (dataMap: Map<String, Any?>) -> Any): List<*> {
        val rows = arrayListOf<Any>()
        val columns = rs.columnNames()
        rs.forEach {
            val dataMap = mutableMapOf<String, Any?>()
            for (i in 0 until columns.size) {
                val colName = columns[i].toLowerCase()
                dataMap[colName] = it[colName]
            }
            val row = dataClassHandler(dataMap)
            rows.add(row)
        }
        return rows
    }

    override suspend fun <A> once(operation: suspend (DBSession) -> A): A {
        try {
            val result: A = operation.invoke(this)
            return result
        } catch (e: Exception) {
            throw e
        } finally {
            // conn.close()
        }
    }

    override suspend fun <A> retry(numRetries: Int, delayTry: Long, operation: suspend (DBSession) -> A) {
        try {
            operation.invoke(this)
        } catch (e: Exception) {
            if (numRetries > 0) {
                if (delayTry > 0) {
                    delay(delayTry)
                }
                retry(numRetries - 1, delayTry, operation)
            } else {
                throw e
            }
        } finally {
            if (numRetries == 0) {
                // conn.close()
            }
        }
    }

    override suspend fun <A> transaction(numRetries: Int, delayTry: Long, operation: suspend (DBSession) -> A) {
        val conn = suspendingConn()
        try {
            conn.inTransaction {
                operation.invoke(JasyncDBSession(JasyncDBPool(rawConnection()), JasyncSuspendingDBConn(it)))
            }
        } catch (e: Exception) {
            if (numRetries > 0) {
                if (delayTry > 0) {
                    delay(delayTry)
                }
                transaction(numRetries - 1, delayTry, operation)
            } else {
                throw e
            }
        } finally {
            if (numRetries == 0) {
                //end tx
            }
        }
    }

    override suspend fun <A> transaction(operation: suspend (DBSession) -> A): A {
        val conn = suspendingConn()
        return conn.inTransaction {
            operation.invoke(JasyncDBSession(JasyncDBPool(rawConnection()), JasyncSuspendingDBConn(it)))
        }
    }

    override suspend fun <A> transactionOpen(operation: suspend (DBSession) -> A): A {
        val conn = suspendingConn()
        return conn.inTransaction {
            operation.invoke(JasyncDBSession(JasyncDBPool(rawConnection()), JasyncSuspendingDBConn(it)))
        }
    }

}
