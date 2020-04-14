package io.zeko.db.sql.connections

import com.github.jasync.sql.db.SuspendingConnection

class JasyncSuspendingDBConn(val conn: SuspendingConnection) : DBConn {
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
