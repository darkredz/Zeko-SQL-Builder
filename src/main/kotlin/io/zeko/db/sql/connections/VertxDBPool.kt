package io.zeko.db.sql.connections

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.tracing.TracingPolicy
import io.vertx.jdbcclient.JDBCConnectOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import java.util.concurrent.TimeUnit

class VertxDBPool : DBPool {
    private lateinit var pool: Pool
    private var vertx: Vertx
    private var insertStatementMode: Int = -1

    constructor(vertx: Vertx, json: JsonObject) {
        this.vertx = vertx
        init(json)
    }

    private fun init(config: JsonObject) {
        val connectOptions = JDBCConnectOptions()
        if (config.containsKey("jdbcUrl")) {
            connectOptions.setJdbcUrl(config.getString("jdbcUrl"))
        }
        if (config.containsKey("database")) {
            connectOptions.setDatabase(config.getString("database"))
        }
        if (config.containsKey("user")) {
            connectOptions.setUser(config.getString("user"))
        }
        if (config.containsKey("password")) {
            connectOptions.setPassword(config.getString("password"))
        }

        val tracingPolicy = when (config.getString("tracingPolicy")) {
            "ALWAYS" -> TracingPolicy.ALWAYS
            "PROPAGATE" -> TracingPolicy.PROPAGATE
            "IGNORE" -> TracingPolicy.IGNORE
            else -> TracingPolicy.PROPAGATE
        }

        connectOptions.setTracingPolicy(tracingPolicy)

        val timeoutUnit = TimeUnit.valueOf(config.getString("poolConnectionTimeoutUnit", PoolOptions.DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT.name))
        val poolOptions = PoolOptions()
            .setMaxSize(config.getInteger("poolSize", PoolOptions.DEFAULT_MAX_SIZE))
            .setConnectionTimeout(config.getInteger("poolConnectionTimeout", PoolOptions.DEFAULT_CONNECTION_TIMEOUT))
            .setConnectionTimeoutUnit(timeoutUnit)

        pool = JDBCPool.pool(vertx, connectOptions, poolOptions)
    }

    override suspend fun createConnection(): DBConn {
        return VertxDBConn(pool.connection.coAwait())
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }
}
