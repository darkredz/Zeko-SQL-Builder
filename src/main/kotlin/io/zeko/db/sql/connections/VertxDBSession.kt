package io.zeko.db.sql.connections

import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import io.zeko.db.sql.utilities.convertParams
import io.zeko.model.declarations.toDataObject
import io.zeko.model.declarations.toMaps
import kotlinx.coroutines.delay
import java.net.ConnectException

open class VertxDBSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: SqlConnection
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true
    protected var connErrorHandler: ((Throwable) -> Unit)? = null

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as SqlConnection
    }

    constructor(dbPool: DBPool, conn: DBConn, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as SqlConnection
        this.throwOnDuplicate = throwOnDuplicate
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): SqlConnection = rawConn

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

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        var updateRes: RowSet<Row>? = null
        var affectedRows = 0
        try {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            if (params.isNotEmpty()) {
                updateRes = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            } else {
                updateRes = stmt.execute().coAwait()
            }
            affectedRows = updateRes.rowCount()
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
        var updateRes: RowSet<Row>? = null
        try {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            updateRes = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            val lastInsertId = updateRes.property(JDBCPool.GENERATED_KEYS)
            return listOf(lastInsertId)
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            // Apache ignite insert will return this due to Auto generated keys are not supported.
            logger?.logUnsupportedSql(err)
            if (updateRes != null ) {
                return listOf(updateRes.property(JDBCPool.GENERATED_KEYS))
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
            val stmt = rawConn.preparedQuery(sql)
            val res = stmt.execute(Tuple.from(convertParams(params).list))
            val rows = res.coAwait().toDataObject(dataClassHandler)
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): RowSet<Row> {
        return executeQuery(sql, params) {
            val stmt = rawConn.preparedQuery(sql)
            stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery(sql, params) {
            val stmt = rawConn.preparedQuery(sql)
            val res = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            val rs = res.toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }

    override suspend fun <T> query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery(sql, listOf()) {
            val stmt = rawConn.query(sql)
            val res = stmt.execute()
            val rows = res.coAwait().toDataObject(dataClassHandler)
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun query(sql: String): RowSet<Row> {
        return executeQuery(sql, listOf()) {
            val stmt = rawConn.query(sql)
            stmt.execute().coAwait()
        }
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery(sql, listOf()) {
            val stmt = rawConn.query(sql)
            val rs = stmt.execute().coAwait().toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }
}
