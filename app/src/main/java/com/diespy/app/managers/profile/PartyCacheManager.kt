package com.diespy.app.managers.profile

object PartyCacheManager {
    var userIds: List<String> = emptyList()
    var usernames: Map<String, String> = emptyMap()
    var turnIndex: Int = 0
    var joinPw: String = ""

    fun clear() {
        userIds = emptyList()
        usernames = emptyMap()
        turnIndex = 0
        joinPw = ""
    }
}