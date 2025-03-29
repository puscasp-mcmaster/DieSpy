package com.diespy.app.managers.party

import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.logs.LogMessage
import com.diespy.app.managers.profile.PartyCacheManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PartyManager {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"

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
                    val rawTurnIndex = snapshot.getLong("turnIndex")?.toInt() ?: 0
                    val turnIndex = if (rawTurnIndex < 0) 0 else rawTurnIndex
                    PartyCacheManager.turnIndex = turnIndex
                    if (members.isNotEmpty()) {
                        val currentTurn = members[turnIndex % members.size]
                        val nextTurn = members[(turnIndex + 1) % members.size]
                        onUpdate(currentTurn, nextTurn)
                    }
                }
            }
    }

    fun subscribeToPartyMembers(
        partyId: String,
        onUpdate: (List<String>) -> Unit
    ): ListenerRegistration {
        val firestore = FirebaseFirestore.getInstance()
        val partyDocRef = firestore.collection("Parties").document(partyId)
        return partyDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Optionally log or handle error
                return@addSnapshotListener
            }
            snapshot?.let { doc ->
                doc.get("userIds")?.let { rawList ->
                    @Suppress("UNCHECKED_CAST")
                    val updatedUserIds = rawList as List<String>
                    onUpdate(updatedUserIds)
                    PartyCacheManager.userIds = updatedUserIds
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
