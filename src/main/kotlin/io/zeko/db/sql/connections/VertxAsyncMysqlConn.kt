package io.zeko.db.sql.connections

import io.vertx.sqlclient.Pool

class VertxAsyncMysqlConn(val pool: Pool) : DBConn {

    override suspend fun beginTx() {
    }

    override suspend fun endTx() {
    }

    override suspend fun commit() {
    }

    override suspend fun close() {
    }

    override suspend fun rollback() {
    }

    override fun raw(): Pool {
        return pool
    }
}
