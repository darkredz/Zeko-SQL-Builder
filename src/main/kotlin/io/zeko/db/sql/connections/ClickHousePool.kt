package io.zeko.db.sql.connections

import com.clickhouse.client.api.Client
import com.clickhouse.client.api.ClientConfigProperties
import com.clickhouse.client.api.internal.ServerSettings
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.tracing.TracingPolicy
import java.time.temporal.ChronoUnit

class ClickHousePool : DBPool {
    private lateinit var client: Client
    private var vertx: Vertx
    private var insertStatementMode: Int = -1
    private var config: JsonObject
    constructor(vertx: Vertx, json: JsonObject) {
        this.vertx = vertx
        this.config = json
        init(json)
    }

    fun getVertx(): Vertx = vertx
    fun getClient(): Client = client

    private fun init(config: JsonObject) {
        val tracingPolicy = when (config.getString("tracingPolicy")) {
            "ALWAYS" -> TracingPolicy.ALWAYS
            "PROPAGATE" -> TracingPolicy.PROPAGATE
            "IGNORE" -> TracingPolicy.IGNORE
            else -> TracingPolicy.PROPAGATE
        }

        val timeout = config.getInteger("poolConnectionTimeout", 30).toLong()
        val timeoutUnit = ChronoUnit.valueOf(config.getString("poolConnectionTimeoutUnit", ChronoUnit.SECONDS.name))

        client = Client.Builder()
            .addEndpoint(config.getString("endpoint"))
            .setUsername(config.getString("user"))
            .setPassword(config.getString("password"))
            .setDefaultDatabase(config.getString("database"))
            .enableConnectionPool(config.getBoolean("enableConnectionPool", true))
            .setMaxConnections(config.getInteger("poolSize", ClientConfigProperties.HTTP_MAX_OPEN_CONNECTIONS.defaultValue.toInt()))
            .setConnectTimeout(timeout, timeoutUnit)
            // allow JSON transcoding as a string
            .serverSetting(ServerSettings.INPUT_FORMAT_BINARY_READ_JSON_AS_STRING, "1")
            .serverSetting(ServerSettings.OUTPUT_FORMAT_BINARY_WRITE_JSON_AS_STRING, "1")
            .build()
    }

    override suspend fun createConnection(): DBConn {
        return ClickHouseConn(client)
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
    }

    override fun getConfig(): JsonObject = config
}
