package io.zeko.db.sql.utilities

import io.vertx.core.json.JsonArray
import io.vertx.sqlclient.Tuple
import io.zeko.model.Entity
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*


fun String.toSnakeCase(): String {
    var text: String = ""
    this.forEachIndexed { index, c ->
        if (c.isUpperCase()) {
            if (index > 0) text += "_"
            text += c.toLowerCase()
        } else {
            text += c
        }
    }
    return text
}

fun String.toCamelCase(): String {
    if (this.isEmpty()) {
        return ""
    }
    var camelCase = this.substring(0, 1).toLowerCase()

    if (this.length > 1) {
        var wordStart = false;

        for (i in 1..(this.length - 1)) {
            var currChar = this[i]
            if (currChar == '_') {
                wordStart = true
            } else {
                if (wordStart) {
                    camelCase += currChar.toUpperCase()
                } else {
                    camelCase += currChar.toLowerCase()
                }
                wordStart = false
            }
        }
    }
    return camelCase
}

fun tuple(param: List<*>): Tuple {
    return Tuple.from(convertParams(param).list)
}

fun tuple(param: MutableMap<String, Any?>): Tuple {
    return Tuple.from(convertParams(param.values.toList()).list)
}

fun tuple(entity: Entity): Tuple {
    return Tuple.from(convertParams(entity.dataMap().values.toList()).list)
}

fun convertTuple(params: List<Any?>): Tuple {
    return Tuple.from(convertParams(params).list)
}

fun convertParams(params: List<Any?>): JsonArray {
    if (!params.isNullOrEmpty()) {
        val converted = arrayListOf<Any?>()
        //Vertx accepts Timestamp for date/time field
        params.forEach { value ->
            val v = when (value) {
                is LocalDate -> Date.valueOf(value)
                is LocalDateTime -> Timestamp.valueOf(value)
                is LocalTime -> Time.valueOf(value)
                is Instant -> Timestamp.valueOf(value.atZone(ZoneId.systemDefault()).toLocalDateTime())
                // if is zoned, stored in DB datetime field as the UTC date time,
                // when doing Entity prop type mapping with datetime_utc, it will be auto converted to ZonedDateTime with value in DB consider as UTC value
                is ZonedDateTime -> {
                    val systemZoneDateTime = value.withZoneSameInstant(ZoneId.of("UTC"))
                    val local = systemZoneDateTime.toLocalDateTime()
                    Timestamp(ZonedDateTime.of(local, ZoneId.systemDefault()).toInstant().toEpochMilli())
                }
                else -> value
            }
            converted.add(v)
        }
        return JsonArray(converted)
    }
    return JsonArray(params)
}
