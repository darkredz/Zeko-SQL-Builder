package io.zeko.db.sql.connections

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.kotlin.ext.sql.getConnectionAwait

class VertxDBPool : DBPool {
    private lateinit var client: JDBCClient
    private var vertx: Vertx
    private var insertStatementMode: Int = -1

    constructor(vertx: Vertx, json: JsonObject) {
        this.vertx = vertx
        init(json)
    }

    private fun init(config: JsonObject) {
        client = JDBCClient.createShared(vertx, config)
    }

    override suspend fun createConnection(): DBConn {
        return VertxDBConn(client.getConnectionAwait())
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }
}
