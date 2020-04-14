package io.zeko.db.sql.connections

import com.github.jasync.sql.db.pool.ConnectionPool

class JasyncDBConn(val conn: ConnectionPool<*>) : DBConn {
    override suspend fun beginTx() {
        conn.sendQuery("BEGIN")
    }

    override suspend fun close() {
        conn.disconnect()
    }

    override suspend fun commit() {
        conn.sendQuery("COMMIT")
    }

    override suspend fun endTx() {
    }

    override fun raw(): Any {
        return conn
    }

    override suspend fun rollback() {
        conn.sendQuery("ROLLBACK")
    }
}
