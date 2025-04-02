package com.diespy.app.managers.logs

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class LogMessage(
    val id: String = "",
    val username: String = "",
    val log: String = "",
    val timestamp: String = ""
)

class LogManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"

    fun saveLog(username: String, logMessage: String, timestamp: String, party: String) {
        val log = hashMapOf(
            "username" to username,
            "log" to logMessage,
            "timestamp" to timestamp
        )
        db.collection(collection)
            .document(party)
            .collection("logs")
            .add(log)
    }

    suspend fun loadLogs(party: String): List<LogMessage> {
        return try {
            val querySnapshot = db.collection(collection)
                .document(party)
                .collection("logs")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .await()
            querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                data?.let {
                    LogMessage(
                        id = doc.id,
                        username = it["username"] as? String ?: "",
                        log = it["log"] as? String ?: "",
                        timestamp = it["timestamp"] as? String ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteLog(party: String, logId: String) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .document(logId)
            .delete()
            .await()
    }

    suspend fun updateLog(party: String, logId: String, newLog: String) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .document(logId)
            .update("log", newLog)
            .await()
    }

    // In LogManager.kt
    fun subscribeToLogs(party: String, onLogsUpdate: (List<LogMessage>) -> Unit) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val newLogs = snapshots.documents.mapNotNull { doc ->
                        // Convert the document to a LogMessage and update the id
                        doc.toObject(LogMessage::class.java)?.copy(id = doc.id)
                    }
                    onLogsUpdate(newLogs)
                }
            }
    }

}
