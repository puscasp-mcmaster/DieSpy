package com.diespy.app.managers.profile

import android.content.Context

object SharedPrefManager {

    private const val PREF_NAME = "UserPrefs"

    // Define Keys
    private const val KEY_CURRENT_USER_ID = "currentUserId"
    private const val KEY_CURRENT_USERNAME = "currentUsername"
    private const val KEY_CURRENT_PARTY_ID = "currentPartyId"
    private const val KEY_CURRENT_PARTY_NAME = "currentPartyName"
    private const val CURRENT_PARTY_USER_COUNT = "currentPartyUserCount"
    // Get SharedPreferences Instance
    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)


    // ---------- USER --------------------------------------------------------------

    // Save Logged-in User ID
    fun saveCurrentUserId(context: Context, userId: String) {
        getPreferences(context).edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    // Retrieve Logged-in User ID
    fun getCurrentUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_CURRENT_USER_ID, null)
    }

    // Clear Logged-in User ID
    private fun clearCurrentUserId(context: Context) {
        getPreferences(context).edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    // Save Logged-in Username
    fun saveCurrentUsername(context: Context, username: String) {
        getPreferences(context).edit().putString(KEY_CURRENT_USERNAME, username).apply()
    }

    // Retrieve Logged-in Username
    fun getCurrentUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_CURRENT_USERNAME, null)
    }

    // Clear Logged-in Username
    private fun clearCurrentUsername(context: Context) {
        getPreferences(context).edit().remove(KEY_CURRENT_USERNAME).apply()
    }

    // Clear all Logged-In User Data
    fun clearCurrentUserData(context: Context) {
        clearCurrentUserId(context)
        clearCurrentUsername(context)
    }

    //----- PARTY --------------------------------------------------------------------

    // Save Current Party ID
    fun saveCurrentPartyId(context: Context, party: String) {
        getPreferences(context).edit().putString(KEY_CURRENT_PARTY_ID, party).apply()
    }

    // Retrieve Current Party ID
    fun getCurrentPartyId(context: Context): String? {
        return getPreferences(context).getString(KEY_CURRENT_PARTY_ID, null)
    }

    // Clear Current Party ID
    private fun clearCurrentPartyId(context: Context) {
        getPreferences(context).edit().remove(KEY_CURRENT_PARTY_ID).apply()
    }

    // Save Current Party Name
    fun saveCurrentPartyName(context: Context, party: String) {
        getPreferences(context).edit().putString(KEY_CURRENT_PARTY_NAME, party).apply()
    }

    // Retrieve Current Party Name
    fun getCurrentPartyName(context: Context): String? {
        return getPreferences(context).getString(KEY_CURRENT_PARTY_NAME, null)
    }

    // Clear Current Party Name
    private fun clearCurrentPartyName(context: Context) {
        getPreferences(context).edit().remove(KEY_CURRENT_PARTY_NAME).apply()
    }

    // Save Current Party User Count
    fun saveCurrentPartyUserCount(context: Context, id: String) {
        getPreferences(context).edit().putString(CURRENT_PARTY_USER_COUNT, id).apply()
    }

    // Retrieve Current Party User Count
    fun getCurrentPartyUserCount(context: Context): String? {
        return getPreferences(context).getString(CURRENT_PARTY_USER_COUNT, null)
    }

    // Clear Current Party User Count
    private fun clearCurrentPartyUserCount(context: Context) {
        getPreferences(context).edit().remove(CURRENT_PARTY_USER_COUNT).apply()
    }



    // Clear All Current Party Data
    fun clearCurrentPartyData(context: Context) {
        clearCurrentPartyId(context)
        clearCurrentPartyName(context)
        clearCurrentPartyUserCount(context)
    }

    //----- ALL DATA ----------------------------------------------------------------

    // Clear All Data
    fun clearAllData(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}