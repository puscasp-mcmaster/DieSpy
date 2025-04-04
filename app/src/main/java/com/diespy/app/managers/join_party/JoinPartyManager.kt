package com.diespy.app.managers.join_party

import android.content.Context
import com.diespy.app.managers.firestore.FireStoreManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.diespy.app.ui.home.PartyItem
import com.google.firebase.firestore.FieldValue

class JoinPartyManager {

    private val firestoreManager = FireStoreManager()

    suspend fun verifyParty(partyName: String): PartyItem? {
        return try {
            val partyData = firestoreManager.queryDocument("Parties", "name", partyName)
            val memberCount = (partyData?.get("userIds") as? List<*>)?.size ?: 0
            PartyItem("e", partyName, memberCount)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinPartyByField(context: Context, field: String, value: String): Result<String> {
        val partyId = firestoreManager.getDocumentIdByField("Parties", field, value)
            ?: return Result.failure(Exception("No party found with that $field."))

        val userId = SharedPrefManager.getCurrentUserId(context)
            ?: return Result.failure(Exception("User not found."))

        val partyData = firestoreManager.queryDocument("Parties", field, value)
        val userIds = partyData?.get("userIds") as? List<*>
        val partyName = partyData?.get("name") as? String

        if (userIds != null && userIds.contains(userId)) {
            return Result.failure(Exception("You're already in this party!"))
        }

        val success = firestoreManager.updateDocument("Parties", partyId, mapOf(
            "userIds" to FieldValue.arrayUnion(userId)
        ))

        return if (success) {
            SharedPrefManager.saveCurrentPartyId(context, partyId)
            if (partyName != null) {
                SharedPrefManager.saveCurrentPartyName(context, partyName)
                Result.success("Party joined!")
            } else {
                Result.failure(Exception("Party name not found. Something went wrong."))
            }
        } else {
            Result.failure(Exception("Failed to join the party. Please try again."))
        }
    }
}
