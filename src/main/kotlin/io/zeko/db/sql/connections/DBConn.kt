package io.zeko.db.sql.connections

interface DBConn {
    suspend fun beginTx()
    suspend fun endTx()
    suspend fun commit()
    suspend fun close()
    suspend fun rollback()
    fun raw(): Any
}
