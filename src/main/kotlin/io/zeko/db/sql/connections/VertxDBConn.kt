package io.zeko.db.sql.connections

import io.vertx.ext.sql.SQLConnection
import io.vertx.kotlin.ext.sql.closeAwait
import io.vertx.kotlin.ext.sql.commitAwait
import io.vertx.kotlin.ext.sql.rollbackAwait
import io.vertx.kotlin.ext.sql.setAutoCommitAwait

class VertxDBConn(val conn: SQLConnection) : DBConn {

    override suspend fun beginTx() {
        conn.setAutoCommitAwait(false)
    }

    override suspend fun endTx() {
        conn.setAutoCommitAwait(true)
    }

    override suspend fun commit() {
        conn.commitAwait()
    }

    override suspend fun close() {
        conn.closeAwait()
    }

    override suspend fun rollback() {
        conn.rollbackAwait()
    }

    override fun raw(): SQLConnection {
        return conn
    }
}
