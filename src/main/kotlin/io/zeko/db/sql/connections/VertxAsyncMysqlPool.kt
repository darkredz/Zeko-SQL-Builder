package io.zeko.db.sql.connections

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

class VertxAsyncMysqlPool : DBPool {
    private lateinit var client: Pool
    private var vertx: Vertx
    private var insertStatementMode: Int = -1

    constructor(vertx: Vertx, json: JsonObject) {
        this.vertx = vertx
        init(json)
    }

    fun getVertx(): Vertx = vertx
    fun getClient(): Pool = client

    private fun init(config: JsonObject) {
       val conf = MySQLConnectOptions()
            .setHost(config.getString("host"))
            .setPort(config.getInteger("port"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"))
           .setReconnectAttempts(config.getInteger("reconnectAttempts", 1))
           .setReconnectInterval(config.getLong("reconnectInterval", 1000))

        val poolOptions = PoolOptions().setMaxSize(config.getInteger("poolSize"))
        client = Pool.pool(vertx, conf, poolOptions)
    }

    override suspend fun createConnection(): DBConn {
        return VertxAsyncMysqlConn(client)
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }
}
