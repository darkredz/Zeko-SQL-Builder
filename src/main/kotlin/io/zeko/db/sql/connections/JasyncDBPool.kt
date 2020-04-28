package io.zeko.db.sql.connections

import com.github.jasync.sql.db.*
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.util.size
import io.vertx.core.json.JsonObject
import java.nio.charset.Charset

class JasyncDBPool : DBPool {
    private lateinit var client: ConnectionPool<*>
    private var insertStatementMode: Int = -1

    constructor(json: JsonObject) {
        init(json)
    }

    constructor(client: ConnectionPool<*>) {
        this.client = client
    }

    constructor(database: String, config: Configuration, poolConfig: ConnectionPoolConfiguration) {
        init(database, config, poolConfig)
    }

    constructor(database: String, config: ConnectionPoolConfigurationBuilder) {
        init(database, config)
    }

    private fun getConfigFromURL(url: String): Pair<String, ConnectionPoolConfigurationBuilder> {
        //eg. "jdbc:mysql://localhost:3306/zeko_test?user=root&password=123456"
        val config = hashMapOf<String, String>()
        val noJdbc = url.substring(5)
        val parts = noJdbc.split("/")
        val databaseType = parts[0].substring(0, parts[0].size - 1)
        val hostParts = parts[2].split(":")
        config["host"] = hostParts[0]
        config["port"] = hostParts[1]
        val uriParts = parts[3].split("?")
        config["database"] = uriParts[0]

        if (uriParts.size > 1) {
            val query = uriParts[1]
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                val key = if (idx > 0) pair.substring(0, idx) else pair
                val value = if (idx > 0 && pair.length > idx + 1) pair.substring(idx + 1) else ""
                config[key] = value
            }
        }
        var charset: Charset = if (config.containsKey("characterEncoding")) Charset.forName(config["characterEncoding"]) else Charset.forName("utf-8")

        val dbConfig = ConnectionPoolConfigurationBuilder().apply {
            host = config["host"]!!
            port = config["port"]!!.toInt()
            database = config["database"]!!
            username = config["user"]!!
            charset = charset
            if (config.containsKey("password"))
                password = config["password"]!!
            if (config.containsKey("queryTimeout"))
                queryTimeout = config["queryTimeout"].toString().toLong()
            if (config.containsKey("maxIdleTime"))
                maxIdleTime = config["queryTimeout"].toString().toLong()
            if (config.containsKey("maxConnectionTtl"))
                maxConnectionTtl = config["maxConnectionTtl"].toString().toLong()
            if (config.containsKey("maxActiveConnections"))
                maxActiveConnections = config["maxActiveConnections"].toString().toInt()
            if (config.containsKey("maxPendingQueries"))
                maxPendingQueries = config["maxPendingQueries"].toString().toInt()
            if (config.containsKey("maximumMessageSize"))
                maximumMessageSize = config["maximumMessageSize"].toString().toInt()
        }
        return (databaseType to dbConfig)
    }

    private fun init(json: JsonObject) {
        var url = json.getString("jdbcUrl")
        if (url == null) {
            url = json.getString("url")
        }
        val (dbType, config) = getConfigFromURL(url)

        if (json.containsKey("maxActiveConnections")) {
            config.maxActiveConnections = json.getInteger("maxActiveConnections")
        } else if (json.containsKey("max_pool_size")) {
            config.maxActiveConnections = json.getInteger("max_pool_size")
        } else {
            config.maxActiveConnections = 15
        }

        if (json.containsKey("maxIdleTime")) {
            config.maxIdleTime = json.getLong("maxIdleTime")
        } else if (json.containsKey("max_idle_time")) {
            config.maxIdleTime = json.getLong("max_idle_time")
        }

        if (json.containsKey("maxPendingQueries")) {
            config.maxPendingQueries = json.getInteger("maxPendingQueries")
        } else if (json.containsKey("max_statements")) {
            config.maxPendingQueries = json.getInteger("max_statements")
        }

        if (json.containsKey("maxConnectionTtl")) {
            config.maxConnectionTtl = json.getLong("maxConnectionTtl")
        }
        if (json.containsKey("connectionCreateTimeout")) {
            config.connectionCreateTimeout = json.getLong("connectionCreateTimeout")
        }
        if (json.containsKey("connectionTestTimeout")) {
            config.connectionTestTimeout = json.getLong("connectionTestTimeout")
        }
        if (json.containsKey("maximumMessageSize")) {
            config.maximumMessageSize = json.getInteger("maximumMessageSize")
        }
        if (json.containsKey("queryTimeout")) {
            config.queryTimeout = json.getLong("queryTimeout")
        }

        if (dbType == "mysql") {
            client = MySQLConnectionBuilder.createConnectionPool(config)
        } else {
            client = PostgreSQLConnectionBuilder.createConnectionPool(url)
        }
    }

    private fun init(database: String, config: Configuration, poolConfig: ConnectionPoolConfiguration) {
        if (database == "mysql") {
            client = ConnectionPool(factory = MySQLConnectionFactory(config), configuration = poolConfig)
        } else {
            client = ConnectionPool(factory = PostgreSQLConnectionFactory(config), configuration = poolConfig)
        }
    }

    private fun init(database: String, config: ConnectionPoolConfigurationBuilder) {
        if (database == "mysql") {
            client = MySQLConnectionBuilder.createConnectionPool(config)
        } else {
            client = PostgreSQLConnectionBuilder.createConnectionPool(config)
        }
    }

    override suspend fun createConnection(): DBConn {
        return JasyncDBConn(client)
    }

    override fun getInsertStatementMode(): Int = insertStatementMode

    override fun setInsertStatementMode(mode: Int) {
        insertStatementMode = mode
    }
}
