package io.zeko.model.entities

import io.zeko.model.Entity

class UserRole : Entity {
    constructor(map: MutableMap<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)

    var id: Int? by map
    var role_name: String? by map
    var user_id: Int? by map
}
