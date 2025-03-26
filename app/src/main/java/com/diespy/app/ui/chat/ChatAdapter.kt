package com.diespy.app.ui.chat

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R
import com.diespy.app.managers.chat.ChatMessage
import com.diespy.app.managers.profile.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.Date

class ChatAdapter(private val context: Context, private var messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    companion object {
        private const val VIEW_TYPE_LEFT = 0
        private const val VIEW_TYPE_RIGHT = 1
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val currentUsername = SharedPrefManager.getCurrentUsername(context)
        return if (message.username == currentUsername) VIEW_TYPE_RIGHT else VIEW_TYPE_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_RIGHT) {
            R.layout.chat_message_right
        } else {
            R.layout.chat_message_left
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // Format the header (username + timestamp) and message
        val date = message.timeStamp?.toDate() ?: Date()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm").format(date)
        val header = "${message.username.replaceFirstChar { it.titlecase() }}: $formattedTime\n"
        val fullText = header + message.msg

        val spannable = SpannableString(fullText).apply {
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, header.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        holder.messageText.text = spannable
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }
}