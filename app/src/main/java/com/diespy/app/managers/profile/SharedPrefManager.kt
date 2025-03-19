package com.diespy.app.managers.profile

import android.content.Context

object SharedPrefManager {

    private const val PREF_NAME = "UserPrefs"

    // Define Keys
    private const val KEY_LOGGED_IN_USER_ID = "loggedInUserId"
    private const val KEY_USERNAME = "username"
    private const val KEY_PARTY = "party"

    // Get SharedPreferences Instance
    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Save Logged-in User ID
    fun saveLoggedInUserId(context: Context, userId: String) {
        getPreferences(context).edit().putString(KEY_LOGGED_IN_USER_ID, userId).apply()
    }

    // Retrieve Logged-in User ID
    fun getLoggedInUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_LOGGED_IN_USER_ID, null)
    }

    // Save Username
    fun saveUsername(context: Context, username: String) {
        getPreferences(context).edit().putString(KEY_USERNAME, username).apply()
    }

    // Retrieve Username
    fun getUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USERNAME, null)
    }

    // Save Party
    fun saveCurrentParty(context: Context, party: String) {
        getPreferences(context).edit().putString(KEY_PARTY, party).apply()
    }

    // Retrieve Party
    fun getCurrentParty(context: Context): String? {
        return getPreferences(context).getString(KEY_PARTY, null)
    }

    // Clear All User Data (Logout)
    fun clearUserData(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}