package com.diespy.app.managers.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    // USERS COLLECTION
    fun getUser(userId: String, callback: (Map<String, Any>?) -> Unit) {
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                callback(document.data)
            }
            .addOnFailureListener { callback(null) }
    }

    fun saveUser(userId: String, userData: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("Users").document(userId)
            .set(userData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    // PARTIES COLLECTION
    fun getParty(partyId: String, callback: (Map<String, Any>?) -> Unit) {
        db.collection("Parties").document(partyId)
            .get()
            .addOnSuccessListener { document ->
                callback(document.data)
            }
            .addOnFailureListener { callback(null) }
    }

    fun saveParty(partyId: String, partyData: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("Parties").document(partyId)
            .set(partyData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

}
