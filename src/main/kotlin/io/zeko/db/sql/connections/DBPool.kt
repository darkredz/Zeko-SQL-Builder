package io.zeko.db.sql.connections

interface DBPool {
    suspend fun createConnection(): DBConn
    fun getInsertStatementMode(): Int
    fun setInsertStatementMode(mode: Int)
}
