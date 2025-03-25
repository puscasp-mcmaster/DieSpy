package com.diespy.app.ui.chat

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.databinding.FragmentChatBinding
import com.diespy.app.managers.chat.ChatManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.diespy.app.R
import com.diespy.app.managers.chat.ChatMessage
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import androidx.lifecycle.lifecycleScope
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatManager: ChatManager
    private lateinit var chatAdapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()
    private val collection = "Parties"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""

        chatManager = ChatManager(requireContext())
        chatAdapter = ChatAdapter(requireContext(), emptyList()) // Start with an empty list
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerView.adapter = chatAdapter

        //Load chat messages from Firestore asynchronously
        lifecycleScope.launch {
            val messages = chatManager.loadMessages(currentParty)
            chatAdapter.updateMessages(messages)
        }

        //Send message button
        binding.sendButton.setOnClickListener {
            val timeStamp = Date()
            //Tries to grab username, if there is not one, it defaults to User
            val username = (SharedPrefManager.getCurrentUsername(requireContext()))?: "User"
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                lifecycleScope.launch {
                    chatManager.saveMessage(username, message, timeStamp.toString(), currentParty)

                    // Reload messages after sending
                    val updatedMessages = chatManager.loadMessages(currentParty)
                    chatAdapter.updateMessages(updatedMessages)

                    // Clear input field
                    binding.messageInput.text.clear()

                    // Scroll to the last message
                    binding.recyclerView.post {
                        binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }

        //Real-time listener for Firestore updates
        db.collection(collection)
            .document(currentParty)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val newMessages = snapshots.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    chatAdapter.updateMessages(newMessages)

                    //TODO may not need this maybe have it show a notification icon instead of scroll
                    // Auto-scroll to the last message when a new one arrives
//                    binding.recyclerView.post {
//                        binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
//                    }
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


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


private inline fun String.replaceFirstChar(transform: (Char) -> CharSequence): String {
    return if (isNotEmpty())
        transform(this[0]).toString() + substring(1)
    else
        this
}


