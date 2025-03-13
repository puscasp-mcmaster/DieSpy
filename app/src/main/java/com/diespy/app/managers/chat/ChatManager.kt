package com.diespy.app.managers.chat

import android.content.Context
import com.diespy.app.managers.firestore.FireStoreManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

data class ChatMessage(
    val username: String = "",
    val msg: String = "",
    val timestamp: String = ""
)

class ChatManager(private val context: Context) {
    private val fireStoreManager = FireStoreManager()
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private val collection = "Parties"


    private val chatDir = File(context.filesDir, "chat")
    private val chatFile = File(chatDir, "chat_messages.json")

    init {
        if (!chatDir.exists()) {
            chatDir.mkdirs()
        }
        if (!chatFile.exists()) {
            chatFile.writeText("[]")
        }
    }

    fun saveMessage(username: String, message: String, timestamp: String) {
        //L6WRTNjIOXw7DTRuEwTt is the default party
        //Create chat object
        val chat = hashMapOf(
            "username" to username,
            "msg" to message,
            "timestamp" to timestamp
        )

        db.collection(collection).document("christian_dev").collection("chat").add(chat)

//        //JSON way of adding messages
//        val messages = loadMessages().toMutableList()
//        val newMessage = JSONObject().apply {
//            put("username", username)
//            put("message", message)
//            put("timestamp", timestamp)
//        }
//        messages.add(newMessage)
//
//        FileWriter(chatFile).use { writer ->
//            writer.write(JSONArray(messages).toString())
//        }
    }

//    fun loadMessages(): List<JSONObject> {
//        return try {
//            val jsonContent = chatFile.readText()
//            val jsonArray = JSONArray(jsonContent)
//            List(jsonArray.length()) { i -> jsonArray.getJSONObject(i) }
//        } catch (e: Exception) {
//            emptyList()
//        }
//    }

    suspend fun loadMessages(): List<ChatMessage> {
        return try {
            val querySnapshot = db.collection(collection)
                .document("christian_dev") // Replace with dynamic party ID if needed
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java) // Directly convert to ChatMessage
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
