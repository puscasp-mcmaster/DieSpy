package com.diespy.app.managers.game;
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import java.security.MessageDigest;
import java.io.File
import java.io.FileWriter

import org.json.JSONObject
import android.content.Context
public class AuthenticationManager {


    private fun String.encrypt(): String {
        val bytes = this.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun authenticate(context: Context, username: String, password: String): Int {
        val playerDir = File(context.filesDir, "players")
        val hashedPw = password.encrypt()

        playerDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            val jsonContent = file.readText()
            val jsonObject = JSONObject(jsonContent)

            if (jsonObject.optString("hashedPw") == hashedPw &&
                jsonObject.optString("username") == username
            ) {
                return 1
            }

        }
        return 0
    }

    fun checkUserExists(context: Context, username: String): Int {
        val playerDir = File(context.filesDir, "players")

        playerDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            val jsonContent = file.readText()
            val jsonObject = JSONObject(jsonContent)

            if (jsonObject.optString("username") == username) {
                return 1
            }

        }
        return 0
    }

    fun createAccount(context: Context, username: String, password: String) {
        // Create the "players" directory if it doesn't exist
        val playerDir = File(context.filesDir, "players")
        if (!playerDir.exists()) {
            playerDir.mkdirs()
        }

        // Hash the password (assuming you have an encrypt() function)
        val hashedPw = password.encrypt()

        // Create a JSON object with the username and hashed password
        val jsonObject = JSONObject().apply {
            put("username", username)
            put("hashedPw", hashedPw)
        }

        // Write the JSON object to a file named "username.json"
        val playerFile = File(playerDir, "$username.json")
        FileWriter(playerFile).use { writer ->
            writer.write(jsonObject.toString())
        }
    }
}
