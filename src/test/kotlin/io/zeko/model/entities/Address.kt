package io.zeko.model.entities

import io.zeko.model.Entity

class Address : Entity {
    constructor(map: MutableMap<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)

    var id: Int? by map
    var user_id: Int? by map
    var street1: String? by map
    var street2: String? by map
}
