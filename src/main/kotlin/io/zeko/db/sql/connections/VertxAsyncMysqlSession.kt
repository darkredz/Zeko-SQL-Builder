package io.zeko.db.sql.connections

import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import io.zeko.model.declarations.toDataObject
import io.zeko.model.declarations.toMaps
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*

open class VertxAsyncMysqlSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: SqlClient
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as VertxAsyncMysqlPool).getClient()
    }

    constructor(dbPool: DBPool, conn: DBConn, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as VertxAsyncMysqlPool).getClient()
        this.throwOnDuplicate = throwOnDuplicate
    }

    constructor(dbPool: DBPool, conn: DBConn, rawConn: SqlClient, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        this.rawConn = rawConn
        this.throwOnDuplicate = throwOnDuplicate
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): SqlClient = rawConn

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

    override suspend fun <A> transaction(operation: suspend (DBSession) -> A): A {
        try {
            val res = (rawConn as Pool).withTransaction {
                val dbSess = VertxAsyncMysqlSession(dbPool, conn, it, throwOnDuplicate)

                var futIn: A? = null
                val vertx = (dbPool as VertxAsyncMysqlPool).getVertx()
                GlobalScope.launch(vertx.dispatcher()) {
                    val r = operation.invoke(dbSess)
                    futIn = r
                }
                Future.succeededFuture(futIn!!)
            }.result()
            return res
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
        TODO("Not yet implemented")
    }

    override suspend fun <A> transactionOpen(operation: suspend (DBSession) -> A): A {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
//        conn.close()
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
        var updateRes: RowSet<Row>? = null

        try {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            if (params.isNotEmpty()) {
                updateRes = stmt.execute(Tuple.from(convertParams(params).list)).result()
            } else {
                updateRes = stmt.execute().result()
            }
            return updateRes.rowCount()
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeConn) conn.close()
        }
    }

    override suspend fun insert(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): List<*> {
        var updateRes: RowSet<Row>? = null

        try {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            updateRes = stmt.execute(Tuple.from(convertParams(params).list)).result()
            val lastInsertId = updateRes.property(MySQLClient.LAST_INSERTED_ID)
            return listOf(lastInsertId)
        } catch (err: Exception) {
            throwDuplicateException(err)
            throw err
        } finally {
            if (closeConn) conn.close()
        }
    }

    override suspend fun <T> queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        logger?.logQuery(sql, params)

        val stmt = rawConn.preparedQuery(sql)
        val res = stmt.execute(Tuple.from(convertParams(params).list))
        val rows = res.result().toDataObject(dataClassHandler)
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): RowSet<Row> {
        logger?.logQuery(sql, params)
        val stmt = rawConn.preparedQuery(sql)
        return stmt.execute(Tuple.from(convertParams(params).list)).result()
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        logger?.logQuery(sql, params)
        val stmt = rawConn.preparedQuery(sql)
        val res = stmt.execute(Tuple.from(convertParams(params).list)).result()
        val rs = res.toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }

    override suspend fun <T> query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        logger?.logQuery(sql)
        val stmt = rawConn.query(sql)
        val res = stmt.execute()
        val rows = res.result().toDataObject(dataClassHandler)

        if (closeConn) conn.close()
        return rows
    }

    override suspend fun query(sql: String): RowSet<Row> {
        logger?.logQuery(sql)
        val stmt = rawConn.query(sql)
        return stmt.execute().result()
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        logger?.logQuery(sql)
        val stmt = rawConn.query(sql)
        val res = stmt.execute()
        val rs = res.result().toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }
}
