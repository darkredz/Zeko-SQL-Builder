package io.zeko.db.sql.connections

interface DBLogger {
    fun getSqlLogLevel(): DBLogLevel
    fun setSqlLogLevel(level: DBLogLevel): DBLogger
    fun getParamsLogLevel(): DBLogLevel
    fun setParamsLogLevel(level: DBLogLevel): DBLogger
    fun setLogLevels(sqlLevel: DBLogLevel, paramsLevel: DBLogLevel): DBLogger
    fun logQuery(sql: String, params: List<Any?>? = null)
    fun logError(err: Exception)
    fun logUnsupportedSql(err: Exception)
    fun logRetry(numRetriesLeft: Int, err: Exception)
}
