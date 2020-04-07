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

class VertxDBSession : DBSession {
    var conn: DBConn
    var dbPool: DBPool
    var rawConn: SQLConnection

    constructor(dbPool: DBPool, conn: DBConn) {
        this.dbPool = dbPool
        this.conn = conn
        rawConn = conn.raw() as SQLConnection
    }

    override fun pool(): DBPool = dbPool

    override fun connection(): DBConn = conn

    override fun rawConnection(): SQLConnection = rawConn

    override suspend fun <A> once(operation: suspend (DBSession) -> A): A {
        try {
            val result: A = operation.invoke(this)
            return result
        } catch (e: Exception) {
            throw e
        } finally {
            conn.close()
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
            if (numRetries > 0) {
                conn.rollback()
                conn.endTx()
                if (delayTry > 0) {
                    delay(delayTry)
                }
                transaction(numRetries - 1, delayTry, operation)
            } else {
                conn.rollback()
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
            throw e
        } finally {
            conn.endTx()
        }
    }

    override suspend fun close() {
        conn.close()
    }

    override suspend fun update(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): Int {
        var updateRes: UpdateResult?
        var affectedRows = 0
        try {
            updateRes = rawConn.updateWithParamsAwait(sql, JsonArray(params))
            affectedRows = updateRes.updated
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            return affectedRows
        } finally {
            if (closeConn) conn.close()
        }
        return affectedRows
    }

    override suspend fun insert(sql: String, params: List<Any?>, closeStatement: Boolean, closeConn: Boolean): List<*> {
        var updateRes: UpdateResult? = null
        try {
            updateRes = rawConn.updateWithParamsAwait(sql, JsonArray(params))
            val affectedRows = updateRes.updated
            if (affectedRows == 0) {
                return listOf<Void>()
            }
            return updateRes.keys.toList()
        } catch (err: java.sql.SQLFeatureNotSupportedException) {
            // Apache ignite insert will return this due to Auto generated keys are not supported.
            if (updateRes != null ) {
                return updateRes?.keys.toList()
            }
        } finally {
            if (closeConn) conn.close()
        }
        return listOf<Void>()
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val res = rawConn.queryWithParamsAwait(sql, JsonArray(params))
        val rows = res.rows.map { jObj ->
            val rowMap = jObj.map.mapKeys { it.key.toLowerCase() }
            dataClassHandler(rowMap)
        }
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>): ResultSet {
        return rawConn.queryWithParamsAwait(sql, JsonArray(params))
    }

    override suspend fun queryPrepared(sql: String, params: List<Any?>, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        val res = rawConn.queryWithParamsAwait(sql, JsonArray(params))
        val rs = res.toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }

    override suspend fun query(sql: String, dataClassHandler: (dataMap: Map<String, Any?>) -> Any, closeStatement: Boolean, closeConn: Boolean): List<*> {
        val res = rawConn.queryAwait(sql)
        val rows = res.rows.map { jObj ->
            val rowMap = jObj.map.mapKeys { it.key.toLowerCase() }
            dataClassHandler(rowMap)
        }
        if (closeConn) conn.close()
        return rows
    }

    override suspend fun query(sql: String): ResultSet {
        return rawConn.queryAwait(sql)
    }

    override suspend fun query(sql: String, columns: List<String>, closeConn: Boolean): List<LinkedHashMap<String, Any?>> {
        val res = rawConn.queryAwait(sql)
        val rs = res.toMaps(columns)
        if (closeConn) conn.close()
        return rs
    }
}
