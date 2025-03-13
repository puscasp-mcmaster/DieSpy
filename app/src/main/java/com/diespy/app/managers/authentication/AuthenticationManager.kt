package com.diespy.app.managers.authentication

import com.diespy.app.managers.firestore.FireStoreManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

data class User(
    val userId: String = "",
    val username: String = "",
    val hashedPw: String = ""
)

class AuthenticationManager {

    private val fireStoreManager = FireStoreManager()
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private val usersCollection = "Users"

    // Encrypt the password using SHA-256
    private fun String.encrypt(): String {
        val bytes = this.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Authenticate user login by checking Firestore for a matching username and password.
     * returns 1 if authentication is successful or 0 if authentication fails
     */
    suspend fun authenticate(username: String, password: String): Int {
        return try {
            val userData = fireStoreManager.queryDocument(usersCollection, "username", username)
            if (userData != null) {
                val storedHashedPw = userData["hashedPw"] as? String
                if (storedHashedPw == password.encrypt()) {
                    return 1 // Login successful
                }
            }
            0 // Login failed
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Checks if a user with the given username already exists in Firestore.
     * returns 1 if the user exists or 0 if the user does not exist
     */
    suspend fun checkUserExists(username: String): Int {
        return if (fireStoreManager.documentExists(usersCollection, "username", username)) 1 else 0
    }

    /**
     * Creates a new account by adding a user to Firestore.
     */
    suspend fun createAccount(username: String, password: String): Boolean {
        return try {
            // Check if user already exists
            if (checkUserExists(username) == 1) {
                return false // User already exists
            }

            val hashedPw = password.encrypt()

            // Create user object
            val user = hashMapOf(
                "username" to username,
                "hashedPw" to hashedPw
            )

            // Add user to Firestore and get unique ID
            val documentReference = db.collection(usersCollection).add(user).await()
            val generatedUserId = documentReference.id // Firestore-generated unique ID

            // Update the user document with the Firestore-generated user ID
            db.collection(usersCollection).document(generatedUserId).update("userId", generatedUserId).await()

            true // Account successfully created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
