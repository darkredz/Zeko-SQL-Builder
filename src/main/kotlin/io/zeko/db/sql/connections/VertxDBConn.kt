package io.zeko.db.sql.connections

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlConnection

class VertxDBConn(val conn: SqlConnection) : DBConn {

    override suspend fun beginTx() {
        conn.begin().coAwait()
    }

    override suspend fun endTx() {
    }

    override suspend fun commit() {
        conn.transaction().commit().coAwait()
    }

    override suspend fun close() {
        conn.close().coAwait()
    }

    override suspend fun rollback() {
        conn.transaction().rollback()
    }

    override fun raw(): SqlConnection {
        return conn
    }
}
