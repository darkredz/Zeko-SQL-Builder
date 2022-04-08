package io.zeko.model

import io.zeko.db.sql.utilities.toCamelCase
import java.time.*
import java.time.format.DateTimeFormatter

abstract class Entity {
    protected var map: MutableMap<String, Any?>

    constructor(map: Map<String, Any?>) {
        val camelCaseMap = mutableMapOf<String, Any?>()
        val mappings = propTypeMapping()
        for ((k, v) in map) {
            if (v == null) continue
            val prop = k.toCamelCase()

            if (mappings != null) {
                camelCaseMap[prop] = mapPropValue(mappings, prop, v)
            } else {
                camelCaseMap[prop] = v
            }
        }
        this.map = camelCaseMap.withDefault { null }
    }

    constructor(vararg props: Pair<String, Any?>) {
        this.map = mutableMapOf(*props).withDefault { null }
    }

    open fun ignoreFields(): List<String> = listOf()

    open fun copyDataMap(map: Map<String, Any?>) {
        this.map = map.toMutableMap()
    }

    open fun copyDataMap(entity: Entity) {
        this.map = entity.dataMap().toMutableMap()
    }

    open fun tableName(): String = ""

    open fun dataMap(): MutableMap<String, Any?> = map

    open fun propTypeMapping(): Map<String, Type>? = null

    open fun mapPropValue(mappings: Map<String, Type>, prop: String, value: Any): Any {
        if (mappings.isNotEmpty()) {
            if (!mappings.containsKey(prop)) return value
            val convertType = mappings[prop]
            if (convertType != null) {
                return convertValueToType(value, convertType)
            }
        }
        return value
    }

    open fun convertValueToType(value: Any, type: Type): Any {
        val converted = when (type) {
            //tiny(1) hikari returns booleam, jasync returns byte
            Type.BOOL -> when (value) {
                is Boolean -> value
                is Byte -> value.toInt() > 0
                else -> false
            }
            Type.INT -> when (value) {
                is Int -> value
                is Byte -> value.toInt()
                is Long -> value.toInt()
                else -> value
            }
            Type.LONG -> when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Byte -> value.toLong()
                else -> value
            }
            Type.DOUBLE -> when (value) {
                is Double -> value
                is Float -> value.toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                is Byte -> value.toLong()
                else -> value
            }
            Type.FLOAT -> when (value) {
                is Float -> value
                is Double -> value.toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                is Byte -> value.toLong()
                else -> value
            }
            Type.DATETIME -> {
                if (value !is String) {
                    val dateStr = value.toString()
                    var pattern: DateTimeFormatter
                    // Jasync returns 2020-01-12T23:10:32.000
                    // Hikari returns 2020-01-12 23:10:32.0
                    if (dateStr.indexOf("T") > 0) {
                        pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    } else {
                        pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
                    }
                    LocalDateTime.parse(dateStr, pattern)
                } else {
                    //Vertx JDBC client returns date time field as String and already converted to UTC
                    val pattern = if (value.indexOf("Z") == value.length - 1 && value.indexOf(".") == value.length - 5) {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")
                    } else {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                    }
                    val systemZoneDateTime = ZonedDateTime.parse(value, pattern).withZoneSameInstant(ZoneId.systemDefault())
                    systemZoneDateTime.toLocalDateTime()
                }
            }
            Type.DATE -> {
                if (value is java.util.Date) {
                    value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    LocalDate.parse(value.toString())
                }
            }
            Type.ZONEDATETIME_UTC -> {
                convertZoneDateTime(value)
            }
            Type.ZONEDATETIME_SYS -> {
                convertZoneDateTime(value, true)
            }
            Type.DATETIME_UTC -> {
                convertZoneDateTime(value).toInstant()
            }
            Type.DATETIME_SYS -> {
                convertZoneDateTime(value, true).toInstant()
            }
            Type.DATE_UTC -> {
                LocalDate.parse(value.toString()).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            else -> value
        }
        return converted
    }

    fun repeatMs(dateStr: String) = "S".repeat(dateStr.split(".").last().takeWhile { !it.isLetter() }.length)

    fun convertZoneDateTime(value: Any, useSystem: Boolean = false): ZonedDateTime {
        if (value !is String) {
            val dateStr = value.toString()
            var patternStr = ""
            if (dateStr.indexOf("T") > 0) {
                patternStr = if (dateStr.indexOf(".") > 0)
                    "yyyy-MM-dd'T'HH:mm:ss.${repeatMs(dateStr)}XXX"
                else
                    "yyyy-MM-dd'T'HH:mm:ssXXX"
            } else {
                // Apache ignite returns "2020-05-06 17:15:03.322Z" for timestamp columns
                patternStr = if (dateStr.indexOf(".") > 0)
                    "yyyy-MM-dd HH:mm:ss.${repeatMs(dateStr)}XXX"
                else
                    "yyyy-MM-dd HH:mm:ssXXX"
            }
            val pattern = DateTimeFormatter.ofPattern(patternStr)
            return ZonedDateTime.parse(dateStr.removeSuffix("Z") + "Z", pattern)
        }

        //Vertx JDBC client returns date time field as String and already converted to UTC
        val pattern = if (value.indexOf("Z") == value.length - 1 && value.indexOf(".") > 0) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.${repeatMs(value)}z")
        } else {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        }

        val systemZoneDateTime = ZonedDateTime.parse(value, pattern).withZoneSameInstant(ZoneId.systemDefault())
        if (useSystem) return systemZoneDateTime
        return systemZoneDateTime.withZoneSameInstant(ZoneId.of("UTC"))
    }

    open fun toParams(valueHandler: ((String, Any?) -> Any?)? = null): List<Any?> {
        val entries = dataMap().entries
        val params = arrayListOf<Any?>()
        val ignores = ignoreFields()

        entries.forEach { prop ->
            if (!(ignores.isNotEmpty() && ignores.indexOf(prop.key) > -1)) {
                if (valueHandler != null) {
                    params.add(valueHandler(prop.key, prop.value))
                } else {
                    when (prop.value) {
                        is Enum<*> -> params.add((prop.value as Enum<*>).name)
                        else -> params.add(prop.value)
                    }
                }
            }
        }
        return params
    }

    override fun toString(): String {
        var str = this.tableName() + " { "
        dataMap().entries.forEach {
            str += "${it.key}-> ${it.value}, "
        }
        return str.removeSuffix(", ") + " }"
    }
}
