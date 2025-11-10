package io.zeko.db.sql.connections

import io.vertx.core.json.JsonObject

interface DBPool {
    suspend fun createConnection(): DBConn
    fun getInsertStatementMode(): Int
    fun setInsertStatementMode(mode: Int)
    fun getConfig(): JsonObject
}
