package com.diespy.app.managers.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreManager {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Saves data to Firestore
     * paramters:   firestore collection name as string
     *              unique firestore document as string
     *              HashMap<String, Any> to store
     * returns true if saved correctly, and false if error
     */
    suspend fun saveData(collection: String, documentId: String, data: Map<String, Any>): Boolean {
        return try {
            db.collection(collection).document(documentId).set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Queries Firestore for a document where a field matches a given value.
     * paramters:   firestore collection name as string
     *              firestore document field name as string
     *              value as string
     */
    suspend fun queryDocument(collection: String, field: String, value: String): Map<String, Any>? {
        return try {
            val querySnapshot = db.collection(collection)
                .whereEqualTo(field, value)
                .limit(1) // Get only the first match
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                document.data
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks firestore if a document exists where a field matches a given value.
     * paramters:   firestore collection name as string
     *              firestore document field name as string
     *              value as string
     * returns true if found, false if not
     */
    suspend fun documentExists(collection: String, fieldName: String, value: String): Boolean {
        return try {
            val querySnapshot = db.collection(collection)
                .whereEqualTo(fieldName, value)
                .limit(1)
                .get()
                .await()

            !querySnapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
