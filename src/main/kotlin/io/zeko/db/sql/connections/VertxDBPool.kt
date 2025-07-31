package io.zeko.db.sql.connections

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCConnectOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

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
            connectOptions.setJdbcUrl(config.getString("driverClassName"))
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

        val poolOptions = PoolOptions()
            .setMaxSize(16)
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
