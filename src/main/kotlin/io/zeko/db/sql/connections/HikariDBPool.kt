package io.zeko.db.sql.connections

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.json.JsonObject

class HikariDBPool : DBPool {
    private lateinit var ds: HikariDataSource
    private var insertStatementMode: Int = -1

    constructor(json: JsonObject) {
        init(json)
    }

    constructor(config: HikariConfig) {
        init(config)
    }

    private fun init(config: JsonObject) {
        val hkConfig = HikariConfig()
        if (config.containsKey("driverClassName")) {
            hkConfig.driverClassName = config.getString("driverClassName")
        }
        if (config.containsKey("url")) {
            hkConfig.jdbcUrl = config.getString("url")
        } else {
            hkConfig.jdbcUrl = config.getString("jdbcUrl")
        }
        if (config.containsKey("username")) {
            hkConfig.username = config.getString("username")
        }
        if (config.containsKey("password")) {
            hkConfig.password = config.getString("password")
        }
        hkConfig.maximumPoolSize = if (config.containsKey("max_pool_size")) config.getInteger("max_pool_size") else 15
        hkConfig.minimumIdle = if (config.containsKey("initial_pool_size")) config.getInteger("initial_pool_size") else 3

        if (config.containsKey("max_idle_time")) {
            hkConfig.maxLifetime = config.getLong("max_idle_time")
        }
        if (config.containsKey("aliveBypassWindowMs")) {
            hkConfig.connectionTimeout = config.getLong("aliveBypassWindowMs")
        }

        ds = HikariDataSource(hkConfig)
    }

    fun init(config: HikariConfig) {
        ds = HikariDataSource(config)
    }

    override suspend fun createConnection(): DBConn {
        return HikariDBConn(ds.connection)
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }
}

