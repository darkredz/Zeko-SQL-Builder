package io.zeko.db.sql.connections

import java.sql.Connection

class HikariDBConn(val conn: Connection) : DBConn {
    override suspend fun beginTx() {
        conn.autoCommit = false
    }

    override suspend fun endTx() {
        conn.autoCommit = true
    }

    override suspend fun commit() {
        conn.commit()
    }

    override suspend fun close() {
        conn.close()
    }

    override suspend fun rollback() {
        conn.rollback()
    }

    override fun raw(): Connection {
        return conn
    }
}
