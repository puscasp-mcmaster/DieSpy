package com.diespy.app.managers.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

class ChatManager(private val context: Context) {
    private val chatDir = File(context.filesDir, "chat")
    private val chatFile = File(chatDir, "chat_messages.json")

    init {
        if (!chatDir.exists()) {
            chatDir.mkdirs()
        }
        if (!chatFile.exists()) {
            chatFile.writeText("[]")
        }
    }

    fun saveMessage(username: String, message: String) {
        val messages = loadMessages().toMutableList()
        val newMessage = JSONObject().apply {
            put("username", username)
            put("message", message)
        }
        messages.add(newMessage)

        FileWriter(chatFile).use { writer ->
            writer.write(JSONArray(messages).toString())
        }
    }

    fun loadMessages(): List<JSONObject> {
        return try {
            val jsonContent = chatFile.readText()
            val jsonArray = JSONArray(jsonContent)
            List(jsonArray.length()) { i -> jsonArray.getJSONObject(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
