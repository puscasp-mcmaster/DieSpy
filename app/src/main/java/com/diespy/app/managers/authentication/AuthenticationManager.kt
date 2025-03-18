package com.diespy.app.managers.authentication

import com.diespy.app.managers.firestore.FireStoreManager
import java.security.MessageDigest

data class User(
    val username: String = "",
    val hashedPw: String = ""
)

class AuthenticationManager {

    private val fireStoreManager = FireStoreManager()
    private val usersCollection = "Users"

    private fun String.encrypt(): String {
        val bytes = this.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

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

    suspend fun checkUserExists(username: String): Int {
        return if (fireStoreManager.documentExists(usersCollection, "username", username)) 1 else 0
    }

    suspend fun createAccount(username: String, password: String): Boolean {
        return try {
            if (checkUserExists(username) == 1) {
                return false // User already exists
            }

            val hashedPw = password.encrypt()
            val user = hashMapOf(
                "username" to username,
                "hashedPw" to hashedPw
            )

            val documentId = fireStoreManager.createDocument(usersCollection, user)
            documentId != null // Return true if user was created successfully
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
