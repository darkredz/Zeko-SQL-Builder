package io.zeko.model.entities

import io.zeko.model.Entity

class User : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)
    //example to use tableName() to specify the table name in the sql statement
    override fun tableName(): String = "users"

    var id: Int? by map
    var name: String? by map
    var age: Int? by map
    var roles: List<UserRole>? by map
    var addresses: List<Address>? by map
}
