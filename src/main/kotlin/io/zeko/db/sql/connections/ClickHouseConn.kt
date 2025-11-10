package io.zeko.db.sql.connections

import com.clickhouse.client.api.Client

class ClickHouseConn(val pool: Client) : DBConn {

    override suspend fun beginTx() {
    }

    override suspend fun endTx() {
    }

    override suspend fun commit() {
    }

    override suspend fun close() {
        return pool.close()
    }

    override suspend fun rollback() {
    }

    override fun raw(): Client {
        return pool
    }
}
