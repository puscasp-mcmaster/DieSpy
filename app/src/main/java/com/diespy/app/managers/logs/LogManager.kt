package com.diespy.app.managers.logs

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

//Object for Logs
data class LogMessage(
    val id: String = "",
    val username: String = "",
    val log: String = "",
    val timestamp: String = ""
)

class LogManager() {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"

    //Saving logs to Firestore
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

    //Load logs from snapshot
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

    //Delete log from Firestore
    suspend fun deleteLog(party: String, logId: String) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .document(logId)
            .delete()
            .await()
    }

    //Update log, used for editing rolls
    suspend fun updateLog(party: String, logId: String, newLog: String) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .document(logId)
            .update("log", newLog)
            .await()
    }

    //Real time updates for logs
    fun subscribeToLogs(party: String, onLogsUpdate: (List<LogMessage>) -> Unit) {
        db.collection(collection)
            .document(party)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val newLogs = snapshots.documents.mapNotNull { doc ->
                        doc.toObject(LogMessage::class.java)?.copy(id = doc.id)
                    }
                    onLogsUpdate(newLogs)
                }
            }
    }

}
