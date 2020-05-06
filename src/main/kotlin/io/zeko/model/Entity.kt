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
                    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                    val systemZoneDateTime = ZonedDateTime.parse(value, pattern).withZoneSameInstant(ZoneId.systemDefault())
                    return systemZoneDateTime.toLocalDateTime()
                }
            }
            Type.DATE -> {
                LocalDate.parse(value.toString())
            }
            Type.ZONEDATETIME_UTC -> {
                if (value !is String) {
                    val dateStr = value.toString()
                    var pattern: DateTimeFormatter
                    if (dateStr.indexOf("T") > 0) {
                        pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    } else {
                        // Apache ignite returns "2020-05-06 17:15:03.322Z" for timestamp columns
                        if (dateStr.length > 21 && dateStr[19] + "" == "." && dateStr[21] + "" != "Z") {
                            pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXXX")
                        } else {
                            pattern = if (dateStr.indexOf(".") > 0)
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SXXX")
                            else
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
                        }
                    }
                    ZonedDateTime.parse(dateStr.removeSuffix("Z") + "Z", pattern)
                } else {
                    //Vertx JDBC client returns date time field as String and already converted to UTC
                    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                    val systemZoneDateTime = ZonedDateTime.parse(value, pattern).withZoneSameInstant(ZoneId.systemDefault())
                    val local = systemZoneDateTime.toLocalDateTime()
                    ZonedDateTime.of(local, ZoneId.of("UTC"))
                }
            }
            Type.DATETIME_UTC -> {
                if (value !is String) {
                    val dateStr = value.toString()
                    var pattern: DateTimeFormatter
                    if (dateStr.indexOf("T") > 0) {
                        pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    } else {
                        pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
                    }
                    LocalDateTime.parse(dateStr, pattern).atZone(ZoneOffset.UTC).toInstant()
                } else {
                    //Vertx JDBC client returns date time field as String and already converted to UTC
                    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                    val systemZoneDateTime = ZonedDateTime.parse(value, pattern).withZoneSameInstant(ZoneId.systemDefault())
                    val local = systemZoneDateTime.toLocalDateTime()
                    ZonedDateTime.of(local, ZoneId.of("UTC")).toInstant()
                }
            }
            Type.DATE_UTC -> {
                LocalDate.parse(value.toString()).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            else -> value
        }
        return converted
    }

    open fun toParams(valueHandler: ((String, Any?) -> Any?)? = null): List<Any?> {
        val entries = dataMap().entries
        val params = arrayListOf<Any?>()
        entries.forEach { prop ->
            if (valueHandler != null) {
                params.add(valueHandler(prop.key, prop.value))
            } else {
                when (prop.value) {
                    is Enum<*> -> params.add((prop.value as Enum<*>).name)
                    else -> params.add(prop.value)
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
