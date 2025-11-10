package io.zeko.db.sql.connections

import com.clickhouse.client.api.Client
import com.clickhouse.client.api.metrics.ServerMetrics
import com.clickhouse.client.api.query.QueryResponse
import io.zeko.db.sql.exceptions.DuplicateKeyException
import io.zeko.db.sql.exceptions.throwDuplicate
import io.zeko.model.Entity
import io.zeko.model.ResultSetHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await

open class ClickHouseSession : DBSession {
    protected var conn: DBConn
    protected var dbPool: DBPool
    protected var rawConn: Client
    protected var logger: DBLogger? = null
    protected var throwOnDuplicate = true
    protected var connErrorHandler: (suspend (Throwable, DBErrorCode, DBSession) -> Boolean)? = null

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as ClickHousePool).getClient()
    }

    constructor(dbPool: DBPool, conn: DBConn, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as ClickHousePool).getClient()
        this.throwOnDuplicate = throwOnDuplicate
    }

    constructor(dbPool: DBPool, conn: DBConn, rawConn: Client, throwOnDuplicate: Boolean) {
        this.dbPool = dbPool
        this.conn = conn
        this.rawConn = rawConn
        this.throwOnDuplicate = throwOnDuplicate
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): Client = rawConn

    protected fun throwDuplicateException(err: Exception) {
        if (this.throwOnDuplicate) {
            throwDuplicate(err)
        }
    }

    fun setRawConnection(rawConn: Client): DBSession {
        this.rawConn = rawConn
        return this
    }

    override fun reinit(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = (dbPool as ClickHousePool).getClient()
    }

    override fun setConnErrorHandler(handler: suspend (Throwable, DBErrorCode, DBSession) -> Boolean): DBSession {
        this.connErrorHandler = handler
        return this
    }

    override fun checkIsConnError (err: Throwable): DBErrorCode? {
        // TODO: Implement specific error code checks for ClickHouse
        return null
    }

    private suspend fun <T : Any> executeQuery(
        operation: suspend () -> T
    ): T {
        try {
            return operation()
        } catch (err: Throwable) {
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
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
            return operation.invoke(this)
        } catch (err: Throwable) {
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
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
            operation.invoke(this)
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
            return operation.invoke(this)
        } catch (err: Throwable) {
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
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
        conn.close()
    }

    override fun setQueryLogger(logger: DBLogger): DBSession {
        this.logger = logger
        return this
    }

    override fun getQueryLogger(): DBLogger? {
        return this.logger
    }

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        try {
            logger?.logQuery(sql, params)
            val queryParams: MutableMap<String, Any> = HashMap()
            params.forEachIndexed { index, any ->
                queryParams["param$index"] = any as Any
            }
            rawConn.query(sql, queryParams).await().close()
            // Clickhouse update does not return affected rows
            return 1
        } catch (err: Exception) {
            throwDuplicateException(err)
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
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
        try {
            logger?.logQuery(sql, params)
            val queryParams: MutableMap<String, Any> = HashMap()
            params.forEachIndexed { index, any ->
                queryParams["param$index"] = any as Any
            }
            val res = rawConn.query(sql, queryParams).await()
            val insertedRows = res.metrics.getMetric(ServerMetrics.NUM_ROWS_WRITTEN).getLong()
            res.close()
            return listOf(insertedRows)
        } catch (err: Exception) {
            throwDuplicateException(err)
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
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

    override suspend fun insert(tableName: String, records: List<Entity>, closeConn: Boolean): List<*> {
        try {
            val res = rawConn.insert(tableName, records).await()
            val insertedRows = res.writtenRows
            res.close()
            return listOf(insertedRows)
        } catch (err: Exception) {
            throwDuplicateException(err)
            val errorCode = checkIsConnError(err)
            if (errorCode != null && connErrorHandler != null) {
                val toRetry = connErrorHandler?.invoke(err, errorCode, this)
                if (toRetry == true) {
                    return insert(tableName, records, closeConn)
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
            val queryParams: MutableMap<String, Any> = HashMap()
            params.forEachIndexed { index, any ->
                queryParams["param$index"] = any as Any
            }
            val res = rawConn.query(sql, queryParams).await()
            val reader = rawConn.newBinaryFormatReader(res)
            val rows = mutableListOf<T>()
            while (reader.hasNext()) {
                rows.add(dataClassHandler(reader.next()))
            }
            res.close()
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): QueryResponse {
        return executeQuery {
            logger?.logQuery(sql, params)
            val queryParams: MutableMap<String, Any> = HashMap()
            params.forEachIndexed { index, any ->
                queryParams["param$index"] = any as Any
            }

            rawConn.query(sql, queryParams).await()
            // client need to close response manually
        }
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery {
            logger?.logQuery(sql, params)
            val queryParams: MutableMap<String, Any> = HashMap()
            params.forEachIndexed { index, any ->
                queryParams["param$index"] = any as Any
            }
            val res = rawConn.query(sql, queryParams).await()

            val result = mutableListOf<LinkedHashMap<String, Any?>>()
            val reader = rawConn.newBinaryFormatReader(res)
            while (reader.hasNext()) {
                val row = reader.next()
                val values = columns.map { col -> row[col] }
                result.add(ResultSetHelper.convertRowToMap(values, columns))
            }
            res.close()
            if (closeConn) conn.close()
            result
        }
    }

    override suspend fun <T> query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> T, closeStatement: Boolean, closeConn: Boolean): List<T> {
        return executeQuery {
            logger?.logQuery(sql)
            val res = rawConn.query(sql).await()
            val reader = rawConn.newBinaryFormatReader(res)
            val rows = mutableListOf<T>()
            while (reader.hasNext()) {
                rows.add(dataClassHandler(reader.next()))
            }
            res.close()
            if (closeConn) conn.close()
            rows
        }
    }

    override suspend fun query(sql: String): QueryResponse {
        return executeQuery {
            logger?.logQuery(sql)
            rawConn.query(sql).await()
            // client need to close response manually
        }
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        return executeQuery {
            logger?.logQuery(sql)
            val res = rawConn.query(sql).await()
            val reader = rawConn.newBinaryFormatReader(res)
            val result = mutableListOf<LinkedHashMap<String, Any?>>()
            while (reader.hasNext()) {
                val row = reader.next()
                val values = columns.map { col -> row[col] }
                result.add(ResultSetHelper.convertRowToMap(values, columns))
            }
            res.close()
            if (closeConn) conn.close()
            result
        }
    }
}
