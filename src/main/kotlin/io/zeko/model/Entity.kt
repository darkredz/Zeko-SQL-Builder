package io.zeko.model

abstract class Entity {
    protected var map: MutableMap<String, Any?> = mutableMapOf()

    constructor(map: Map<String, Any?>) {
        if (map is MutableMap)
            this.map = map
        else
            this.map = map.toMutableMap()
    }

    constructor(vararg props: Pair<String, Any?>) {
        this.map = mutableMapOf(*props)
    }

    open fun tableName(): String = ""

    open fun dataMap(): MutableMap<String, Any?> = map
}
