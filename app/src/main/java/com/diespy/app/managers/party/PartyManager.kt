package com.diespy.app.managers.party

import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.logs.LogMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PartyManager {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"
    private val fireStoreManager = FireStoreManager()

    // Retrieves party members from the party document's "members" field.
    // Ensure your Firestore party document contains a "members" array.
    suspend fun getPartyMembersForTurn(party: String): List<String> {
        var members = fireStoreManager.getUsernamesForParty(party)
        if (members.size == 1) {
            members = listOf(members[0], members[0])
        }
        return members
    }

    // Subscribes to the logs and returns the latest log via a callback.
// In PartyManager.kt
    fun subscribeToLatestLog(party: String, onLatestLogUpdate: (LogMessage?) -> Unit) {
        db.collection("Parties")
            .document(party)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val logs = snapshots.documents.mapNotNull { doc ->
                        doc.toObject(LogMessage::class.java)?.copy(id = doc.id)
                    }
                    onLatestLogUpdate(logs.lastOrNull())
                }
            }
    }

    fun subscribeToTurnOrder(
        party: String,
        members: List<String>,
        onUpdate: (currentTurn: String, nextTurn: String) -> Unit
    ) {
        db.collection(collection).document(party)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val turnIndex = snapshot.getLong("turnIndex")?.toInt() ?: 0
                    if (members.isNotEmpty()) {
                        val currentTurn = members[turnIndex % members.size]
                        val nextTurn = members[(turnIndex + 1) % members.size]
                        onUpdate(currentTurn, nextTurn)
                    }
                }
            }
    }

    suspend fun updateTurnOrder(party: String) {
        val partyRef = db.collection(collection).document(party)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(partyRef)
            val members = snapshot.get("userIds") as? List<String> ?: emptyList()
            if (members.isNotEmpty()) {
                val currentTurnIndex = snapshot.getLong("turnIndex")?.toInt() ?: 0
                // Compute the new turn index (wrap around if needed)
                val newTurnIndex = (currentTurnIndex + 1) % members.size
                transaction.update(partyRef, "turnIndex", newTurnIndex)
            }
            null
        }.await()
    }

}
