package com.diespy.app.managers.chat

import android.content.Context
import com.diespy.app.managers.firestore.FireStoreManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.type.Date
import kotlinx.coroutines.tasks.await
import java.io.File

data class ChatMessage(
    val username: String = "",
    val msg: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class ChatManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private val collection = "Parties"

    fun saveMessage(username: String, message: String, timestamp: Timestamp, party: String) {
        val chat = hashMapOf(
            "username" to username,
            "msg" to message,
            "timestamp" to timestamp
        )

        db.collection(collection).document(party).collection("chat").add(chat)
    }

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

