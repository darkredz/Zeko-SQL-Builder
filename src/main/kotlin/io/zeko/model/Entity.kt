package io.zeko.model

abstract class Entity {
    protected var map: MutableMap<String, Any?>

    constructor(map: Map<String, Any?>) {
        if (map is MutableMap)
            this.map = map.withDefault { null }
        else
            this.map = map.toMutableMap().withDefault { null }
    }

    constructor(vararg props: Pair<String, Any?>) {
        this.map = mutableMapOf(*props).withDefault { null }
    }

    open fun tableName(): String = ""

    open fun dataMap(): MutableMap<String, Any?> = map

    override fun toString(): String {
        var str = this.tableName() + " { "
        dataMap().entries.forEach {
            str += "${it.key}-> ${it.value}, "
        }
        return str.removeSuffix(", ") + " }"
    }
}
