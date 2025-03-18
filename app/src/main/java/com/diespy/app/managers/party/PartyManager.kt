package com.diespy.app.managers.party

import com.diespy.app.managers.firestore.FireStoreManager
import kotlin.random.Random

data class Party(
    val name: String = "",
    val joinPw: String = "",
    val userIds: List<String> = emptyList()
)

class PartyManager {

    private val fireStoreManager = FireStoreManager()
    private val partiesCollection = "Parties"

    /**
     * Creates a new party with the given details using FireStoreManager.
     * Returns the Firestore-generated party document ID if successful, null otherwise.
     */
    suspend fun createParty(name: String, userId: String): String? {
        return try {
            // Create a Party object
            val partyData = mapOf(
                "name" to name,
                "joinPw" to generateRandomPassword(),
                "userIds" to listOf(userId)
            )

            // Use FireStoreManager to create a document and return the generated document ID
            fireStoreManager.createDocument(partiesCollection, partyData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateRandomPassword(length: Int = 6): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { characters.random() }
            .joinToString("")
    }

}
