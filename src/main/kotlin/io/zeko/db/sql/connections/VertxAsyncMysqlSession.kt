package io.zeko.db.sql.connections

import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import io.zeko.db.sql.utilities.convertParams
import io.zeko.model.declarations.toDataObject
import io.zeko.model.declarations.toMaps
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ConnectException

open class VertxAsyncMysqlSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: SqlClient
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true
    protected var connErrorHandler: (suspend (Throwable, DBSession) -> Boolean)? = null

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

    // TODO: Add set conn error handler to interface class
    fun setConnErrorHandler(handler: suspend (Throwable, DBSession) -> Boolean): DBSession {
        this.connErrorHandler = handler
        return this
    }

    fun setRawConnection(rawConn: SqlClient): DBSession {
        this.rawConn = rawConn
        return this
    }

    fun reinit(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as VertxAsyncMysqlPool).getClient()
    }

    private fun checkIsConnError (err: Throwable): Boolean {
        return err is ConnectException || err.message!!.contains("CLOSED")
    }

    private suspend fun <T : Any> executeQuery(
        operation: suspend () -> T
    ): T {
        try {
            return operation()
        } catch (err: Throwable) {
            if (checkIsConnError(err) && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, this)
                if (toRetry == true) {
                    return operation()
                } else {
                    throw err
                }
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

    override suspend fun <A> transaction(operation: suspend (DBSession) -> A): A {
        try {
            return (rawConnection() as Pool).withTransaction { client ->
                Future.future {
                    GlobalScope.launch((dbPool as VertxAsyncMysqlPool).getVertx().dispatcher()) {
                        try {
                            val sess = VertxAsyncMysqlSession(
                                dbPool,
                                VertxAsyncMysqlConn((dbPool as VertxAsyncMysqlPool).getClient()),
                                client as SqlClient,
                                throwOnDuplicate
                            )
                            if (logger != null) sess.setQueryLogger(logger!!)
                            val rs = operation.invoke(sess)
                            it.complete(rs)
                        } catch (err: Throwable) {
                            it.fail(err)
                        }
                    }
                }
            }.coAwait()
        } catch (err: Throwable) {
            if (checkIsConnError(err) && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, this)
                if (toRetry == true) {
                    return transaction(operation)
                } else {
                    throw err
                }
            }
            throw err
        }
    }

    override suspend fun <A> transaction(numRetries: Int, delayTry: Long, operation: suspend (DBSession) -> A) {
        try {
            (rawConnection() as Pool).withTransaction { client ->
                Future.future {
                    GlobalScope.launch((dbPool as VertxAsyncMysqlPool).getVertx().dispatcher()) {
                        try {
                            val sess = VertxAsyncMysqlSession(dbPool, VertxAsyncMysqlConn((dbPool as VertxAsyncMysqlPool).getClient()), client as SqlClient, throwOnDuplicate)
                            if (logger != null) sess.setQueryLogger(logger!!)
                            val rs = operation.invoke(sess)
                            it.complete(rs)
                        } catch (err: Exception) {
                            it.fail(err)
                        }
                    }
                }
            }.coAwait()
        } catch (e: java.lang.Exception) {
            if (e !is DuplicateKeyException) {
                if (numRetries > 0) {
                    if (delayTry > 0) {
                        delay(delayTry)
                    }
                    logger?.logRetry(numRetries, e)
                    transaction(numRetries - 1, delayTry, operation)
                } else {
                    logger?.logError(e)
                    throw e
                }
            } else {
                throw e
            }
        } finally {
            if (numRetries == 0) {
                //end tx
            }
        }
    }

    override suspend fun <A> transactionOpen(operation: suspend (DBSession) -> A): A {
        try {
            return (rawConnection() as Pool).withTransaction { client ->
                Future.future {
                    GlobalScope.launch((dbPool as VertxAsyncMysqlPool).getVertx().dispatcher()) {
                        try {
                            val sess = VertxAsyncMysqlSession(dbPool, VertxAsyncMysqlConn((dbPool as VertxAsyncMysqlPool).getClient()), client as SqlClient, throwOnDuplicate)
                            if (logger != null) sess.setQueryLogger(logger!!)
                            val rs = operation.invoke(sess)
                            it.complete(rs)
                        } catch (err: Throwable) {
                            it.fail(err)
                        }
                    }
                }
            }.coAwait()
        } catch (err: Throwable) {
            if (checkIsConnError(err) && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, this)
                if (toRetry == true) {
                    return transaction(operation)
                } else {
                    throw err
                }
            }
            throw err
        }
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

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        var updateRes: RowSet<Row>? = null

        try {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            if (params.isNotEmpty()) {
                updateRes = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            } else {
                updateRes = stmt.execute().coAwait()
            }
            return updateRes.rowCount()
        } catch (err: Exception) {
            throwDuplicateException(err)
            if (checkIsConnError(err) && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, this)
                if (toRetry == true) {
                    return update(sql, params, closeStatement, closeConn)
                } else {
                    throw err
                }
            }
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
            updateRes = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            val lastInsertId = updateRes.property(MySQLClient.LAST_INSERTED_ID)
            return listOf(lastInsertId)
        } catch (err: Exception) {
            throwDuplicateException(err)
            if (checkIsConnError(err) && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, this)
                if (toRetry == true) {
                    return insert(sql, params, closeStatement, closeConn)
                } else {
                    throw err
                }
            }
            throw err
        } finally {
            if (closeConn) conn.close()
        }
    }

    override suspend fun <T> queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            val res = stmt.execute(Tuple.from(convertParams(params).list))
            val rows = res.coAwait().toDataObject(dataClassHandler)
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): RowSet<Row> {
        return executeQuery {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery {
            logger?.logQuery(sql, params)
            val stmt = rawConn.preparedQuery(sql)
            val res = stmt.execute(Tuple.from(convertParams(params).list)).coAwait()
            val rs = res.toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }

    override suspend fun <T> query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery {
            logger?.logQuery(sql)
            val stmt = rawConn.query(sql)
            val res = stmt.execute()
            val rows = res.coAwait().toDataObject(dataClassHandler)
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun query(sql: String): RowSet<Row> {
        return executeQuery {
            logger?.logQuery(sql)
            val stmt = rawConn.query(sql)
            stmt.execute().coAwait()
        }
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery {
            logger?.logQuery(sql)
            val stmt = rawConn.query(sql)
            val rs = stmt.execute().coAwait().toMaps(columns)
            if (closeConn) conn.close()
            rs
        }
    }
}
