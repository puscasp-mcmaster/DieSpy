package com.diespy.app.managers.profile

object PartyCacheManager {
    var partyId: String = ""
    var userIds: List<String> = emptyList()
    var usernames: Map<String, String> = emptyMap()
    var turnIndex: Int = 0
    var joinPw: String = ""
    var currentUserId: String? = null

    fun clear() {
        partyId = ""
        userIds = emptyList()
        usernames = emptyMap()
        turnIndex = 0
        joinPw = ""
        currentUserId = null
    }
}