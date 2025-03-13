package com.diespy.app.managers.party

import com.diespy.app.managers.firestore.FireStoreManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Party(
    val partyId: String = "",
    val name: String = "",
    val joinPw: String = "",
    val userIds: List<String> = emptyList()
)

class PartyManager {

    private val fireStoreManager = FireStoreManager()
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private val partiesCollection = "Parties"

    /**
     * Creates a new party with the given details and stores it in Firestore.
     * Returns the generated partyId if successful, null otherwise.
     */
    suspend fun createParty(name: String, joinPw: String, userId: String): String? {
        return try {
            // Generate a unique party ID (Firestore auto-generated ID)
            val newPartyRef = db.collection(partiesCollection).document()

            // Create a party object
            val party = Party(
                partyId = newPartyRef.id,
                name = name,
                joinPw = joinPw,
                userIds = listOf(userId)
            )

            // Save to Firestore
            newPartyRef.set(party).await()
            newPartyRef.id // Return partyId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
