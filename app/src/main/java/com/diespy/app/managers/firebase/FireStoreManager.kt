package com.diespy.app.managers.firestore

import com.diespy.app.ui.home.PartyItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreManager {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Creates a new document in Firestore within a given collection and returns the generated document ID.
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - data: A HashMap<String, Any> containing the document data.
     * returns: The generated document ID as a string if successful, null if error occurs.
     */
    suspend fun createDocument(collection: String, data: Map<String, Any>): String? {
        return try {
            val documentReference = db.collection(collection).add(data).await()
            documentReference.id // Return the generated document ID
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates document to Firestore using a specific document ID and collection name.
     * fields not in data map will remain the same
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - documentId: The specific Firestore document ID as a string.
     *   - data: A HashMap<String, Any> containing the updated document data.
     * returns: True if saved correctly, false if error occurs.
     */
    suspend fun updateDocument(collection: String, documentId: String, data: Map<String, Any>): Boolean {
        return try {
            db.collection(collection).document(documentId).update(data).await()
            true
        } catch (e: Exception) {
            if (e.message?.contains("NOT_FOUND") == true) {
                println("Error: Document with ID $documentId does not exist.")
            }
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes a document from Firestore.
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - documentId: The Firestore document ID to delete.
     * returns: True if deleted successfully, false if error occurs.
     */
    suspend fun deleteDocument(collection: String, documentId: String): Boolean {
        return try {
            db.collection(collection).document(documentId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Queries Firestore for a document where a field matches a given value.
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - field: The Firestore document field name as a string.
     *   - value: The field value to search for.
     * returns: The found document data as a Map<String, Any>, or null if not found.
     */
    suspend fun queryDocument(collection: String, field: String, value: String): Map<String, Any>? {
        return try {
            val querySnapshot = db.collection(collection)
                .whereEqualTo(field, value)
                .limit(1) // Get only the first match
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].data
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch a Firestore document by its document ID.
     * parameters:
     *   - collection: The Firestore collection name.
     *   - documentId: The document ID to retrieve.
     * returns: A Map of the document fields, or null if not found.
     */
    suspend fun getDocumentById(collection: String, documentId: String): Map<String, Any>? {
        return try {
            val documentSnapshot = db.collection(collection)
                .document(documentId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                documentSnapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Checks Firestore if a document exists where a field matches a given value.
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - fieldName: The Firestore document field name as a string.
     *   - value: The field value to search for.
     * returns: True if found, false if not.
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


    /**
     * Query Firestore for documentId
     * ONLY WORKS FOR UNIQUE FIELDS
     * parameters:
     *   - collection: The Firestore collection name as a string.
     *   - fieldName: The Firestore document field name as a string.
     *   - value: The field value to search for.
     * returns: documentID if found, null if not found
     */
    suspend fun getDocumentIdByField(collection: String, field: String, value: String): String? {
        return try {
            val querySnapshot = db.collection(collection)
                .whereEqualTo(field, value)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].id // Return the document ID
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Query Firestore for parties of a user
     * parameters:
     *   - userId: The document id for users
     * returns: list of partys with associated name, member count, and id
     */
    suspend fun getAllPartiesForUser(userId: String): List<PartyItem> {
        return try {
            val querySnapshot = FirebaseFirestore.getInstance()
                .collection("Parties")
                .whereArrayContains("userIds", userId)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val userIds = doc.get("userIds") as? List<*> ?: emptyList<Any>()
                val userCount = userIds.size

                PartyItem(id, name, userCount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Query Firestore for users of a party
     * parameters:
     *   - partyId: The document id for party
     * returns: list of usernames in the party
     */
    suspend fun getUsernamesForParty(partyId: String): List<String> {
        return try {
            val partySnapshot = FirebaseFirestore.getInstance()
                .collection("Parties")
                .document(partyId)
                .get()
                .await()

            val userIds = partySnapshot.get("userIds") as? List<String> ?: return emptyList()

            val usernames = mutableListOf<String>()
            for (userId in userIds) {
                val userSnapshot = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .get()
                    .await()

                userSnapshot.getString("username")?.let { usernames.add(it) }
            }

            usernames
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }




}
