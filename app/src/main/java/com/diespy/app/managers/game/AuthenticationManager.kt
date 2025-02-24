package com.diespy.app.managers.game;
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import java.security.MessageDigest;
import java.io.File
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
}
