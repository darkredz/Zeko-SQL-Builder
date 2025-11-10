package io.zeko.db.sql.connections

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.tracing.TracingPolicy
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import java.util.concurrent.TimeUnit

class VertxAsyncMysqlPool : DBPool {
    private lateinit var client: Pool
    private var vertx: Vertx
    private var insertStatementMode: Int = -1
    private var config: JsonObject
    constructor(vertx: Vertx, json: JsonObject) {
        this.vertx = vertx
        this.config = json
        init(json)
    }

    fun getVertx(): Vertx = vertx
    fun getClient(): Pool = client

    private fun init(config: JsonObject) {
        val tracingPolicy = when (config.getString("tracingPolicy")) {
            "ALWAYS" -> TracingPolicy.ALWAYS
            "PROPAGATE" -> TracingPolicy.PROPAGATE
            "IGNORE" -> TracingPolicy.IGNORE
            else -> TracingPolicy.PROPAGATE
        }
        val conf = MySQLConnectOptions()
            .setHost(config.getString("host"))
            .setPort(config.getInteger("port"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"))
           .setReconnectAttempts(config.getInteger("reconnectAttempts", 1))
           .setReconnectInterval(config.getLong("reconnectInterval", 1000))
           .setTracingPolicy(tracingPolicy)

        val timeoutUnit = TimeUnit.valueOf(config.getString("poolConnectionTimeoutUnit", PoolOptions.DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT.name))

        val poolOptions = PoolOptions()
            .setMaxSize(config.getInteger("poolSize", PoolOptions.DEFAULT_MAX_SIZE))
            .setConnectionTimeout(config.getInteger("poolConnectionTimeout", PoolOptions.DEFAULT_CONNECTION_TIMEOUT))
            .setConnectionTimeoutUnit(timeoutUnit)
        client = Pool.pool(vertx, conf, poolOptions)
    }

    override suspend fun createConnection(): DBConn {
        return VertxAsyncMysqlConn(client)
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }

    override fun getConfig(): JsonObject = config
}
