package io.zeko.db.sql.connections

import io.zeko.model.declarations.toMaps
import io.vertx.core.json.JsonArray
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.ext.sql.queryAwait
import io.vertx.kotlin.ext.sql.queryWithParamsAwait
import io.vertx.kotlin.ext.sql.updateWithParamsAwait
import kotlinx.coroutines.delay
import java.lang.Exception
import java.util.LinkedHashMap
import io.vertx.ext.sql.ResultSet
import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.net.ConnectException

open class VertxDBSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: SQLConnection
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true
    protected var connErrorHandler: ((Throwable) -> Unit)? = null

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as SQLConnection
    }

    constructor(dbPool: DBPool, conn: DBConn, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as SQLConnection
        this.throwOnDuplicate = throwOnDuplicate
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): SQLConnection = rawConn

    protected fun throwDuplicateException(err: Exception) {
        if (this.throwOnDuplicate) {
            throwDuplicate(err)
        }
    }

    private suspend fun <T> executeQuery(
        sql: String,
        params: List<Any?>,
        operation: suspend () -> T
    ): T {
        try {
            logger?.logQuery(sql, params)
            return operation()
        } catch (err: Exception) {
            if (err is ConnectException && connErrorHandler != null) {
                connErrorHandler?.invoke(err)
            }
            throw err
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
            operation.invoke(this)
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

    // TODO: Add set conn error handler to interface class
    fun setConnErrorHandler(handler: (Throwable) -> Unit): DBSession {
        this.connErrorHandler = handler
        return this
    }

    override fun setQueryLogger(logger: DBLogger): DBSession {
        this.logger = logger
        return this
    }

    override fun getQueryLogger(): DBLogger? {
        return this.logger
    }

    private fun convertParams(params: List<Any?>): JsonArray {
        if (!params.isNullOrEmpty()) {
            val converted = arrayListOf<Any?>()
            //Vertx accepts Timestamp for date/time field
            params.forEach { value ->
                val v = when (value) {
                    is LocalDate -> Date.valueOf(value)
                    is LocalDateTime -> Timestamp.valueOf(value)
                    is LocalTime -> Time.valueOf(value)
                    is Instant -> Timestamp.valueOf(value.atZone(ZoneId.systemDefault()).toLocalDateTime())
                    // if is zoned, stored in DB datetime field as the UTC date time,
                    // when doing Entity prop type mapping with datetime_utc, it will be auto converted to ZonedDateTime with value in DB consider as UTC value
                    is ZonedDateTime -> {
                        val systemZoneDateTime = value.withZoneSameInstant(ZoneId.of("UTC"))
                        val local = systemZoneDateTime.toLocalDateTime()
                        Timestamp(ZonedDateTime.of(local, ZoneId.systemDefault()).toInstant().toEpochMilli())
                    }
                    else -> value
                }
                converted.add(v)
            }
            return JsonArray(converted)
        }
        return JsonArray(params)
    }

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        var updateRes: UpdateResult?
        var affectedRows = 0
        try {
            logger?.logQuery(sql, params)
            updateRes = rawConn.updateWithParamsAwait(sql, convertParams(params))
            affectedRows = updateRes.updated
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            return affectedRows
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeConn) conn.close()
        }
        return affectedRows
    }

    override suspend fun insert(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): List<*> {
        var updateRes: UpdateResult? = null
        try {
            logger?.logQuery(sql, params)
            updateRes = rawConn.updateWithParamsAwait(sql, convertParams(params))
            val affectedRows = updateRes.updated
            if (affectedRows == 0) {
                return listOf<Void>()
            }
            return updateRes.keys.toList()
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            // Apache ignite insert will return this due to Auto generated keys are not supported.
            logger?.logUnsupportedSql(err)
            if (updateRes != null ) {
                return updateRes.keys.toList()
            }
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeConn) conn.close()
        }
        return listOf<Void>()
    }



    override suspend fun <T> queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery(sql, params) {
            val res = rawConn.queryWithParamsAwait(sql, convertParams(params))
            val rows = res.rows.map { jObj ->
                val rowMap = jObj.map.mapKeys { it.key.toLowerCase() }
                dataClassHandler(rowMap)
            }
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): ResultSet {
        return executeQuery(sql, params) {
            rawConn.queryWithParamsAwait(sql, convertParams(params))
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery(sql, params) {
            val res = rawConn.queryWithParamsAwait(sql, convertParams(params))
            val rs = res.toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }

    override suspend fun <T> query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery(sql, listOf()) {
            val res = rawConn.queryAwait(sql)
            val rows = res.rows.map { jObj ->
                val rowMap = jObj.map.mapKeys { it.key.toLowerCase() }
                dataClassHandler(rowMap)
            }
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun query(sql: String): ResultSet {
        return executeQuery(sql, listOf()) {
            rawConn.queryAwait(sql)
        }
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery(sql, listOf()) {
            val res = rawConn.queryAwait(sql)
            val rs = res.toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }
}
