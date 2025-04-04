package com.diespy.app.managers.create_party

import com.diespy.app.managers.firestore.FireStoreManager

//Object for a party
data class Party(
    val name: String = "",
    val joinPw: String = "",
    val userIds: List<String> = emptyList()
)

class CreatePartyManager {

    private val fireStoreManager = FireStoreManager()
    private val partiesCollection = "Parties"

    suspend fun createParty(name: String, userId: String): String? {
        return try {
            val partyData = mapOf(
                "name" to name,
                "joinPw" to generateRandomPassword(),
                "userIds" to listOf(userId)
            )

            //FireStoreManager to create a party and return the generated document ID for later
            fireStoreManager.createDocument(partiesCollection, partyData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //Generation of join password 
    private fun generateRandomPassword(length: Int = 6): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { characters.random() }
            .joinToString("")
    }
}
