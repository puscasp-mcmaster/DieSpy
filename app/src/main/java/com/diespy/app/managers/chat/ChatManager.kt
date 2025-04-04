package com.diespy.app.managers.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

//Object for messages
data class ChatMessage(
    val username: String = "",
    val msg: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class ChatManager {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"

    //Saving to Firestore
    fun saveMessage(username: String, message: String, timestamp: Timestamp, party: String) {
        val chat = hashMapOf(
            "username" to username,
            "msg" to message,
            "timestamp" to timestamp
        )

        db.collection(collection).document(party).collection("chat").add(chat)
    }

    //Loading from Firestore
    suspend fun loadMessages(party: String): List<ChatMessage> {
        return try {
            val querySnapshot = db.collection(collection)
                .document(party)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java) // Directly convert to ChatMessage
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    //Continuously checking for new messages
    fun subscribeToChatMessages(party: String, onMessagesUpdate: (List<ChatMessage>) -> Unit) {
        db.collection(collection)
            .document(party)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val newMessages = snapshots.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    onMessagesUpdate(newMessages)
                }
            }
    }

}

