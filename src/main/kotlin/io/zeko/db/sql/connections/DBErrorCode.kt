package io.zeko.db.sql.connections

enum class DBErrorCode(val value: Int) {
    CONN_CLOSED(0),
    CONN_EXCEPTION(1),
    CONN_TIMEOUT(2),
    UNKNOWN_HOST(3);

    companion object {
        fun from(value: Short) = DBErrorCode.values().first { it.value == value.toInt() }

        fun from(value: Int) = DBErrorCode.values().first { it.value == value }

        fun from(value: String) = when (value.uppercase()) {
            "CONN_CLOSED" -> CONN_CLOSED
            "CONN_EXCEPTION" -> CONN_EXCEPTION
            "CONN_TIMEOUT" -> CONN_TIMEOUT
            "CONN_UNKNOWN_HOST" -> UNKNOWN_HOST
            else -> throw Exception("Invalid DB Error Code")
        }
    }
}