package com.diespy.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.databinding.FragmentChatBinding
import com.diespy.app.managers.chat.ChatManager
import org.json.JSONObject
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.diespy.app.R
import com.diespy.app.managers.chat.ChatMessage
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatManager: ChatManager
    private lateinit var chatAdapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance() // Firestore instance
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

        // Party Screen button
        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_chat_to_party)
        }

        chatManager = ChatManager(requireContext())
        chatAdapter = ChatAdapter(emptyList()) // Start with an empty list

        // Set layout manager with stackFromEnd = true (messages start at bottom)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerView.adapter = chatAdapter

        // Load chat messages from Firestore asynchronously
        lifecycleScope.launch {
            val messages = chatManager.loadMessages()
            chatAdapter.updateMessages(messages)

            // Move chat to latest message
            binding.recyclerView.post {
                binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        // Send message button
        binding.sendButton.setOnClickListener {
            val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
            val username = "User" // Replace with actual username logic
            val message = binding.messageInput.text.toString()

            if (message.isNotBlank()) {
                lifecycleScope.launch {
                    chatManager.saveMessage(username, message, timeStamp)

                    // Reload messages after sending
                    val updatedMessages = chatManager.loadMessages()
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

        // Real-time listener for Firestore updates
        db.collection(collection)
            .document("christian_dev") // Change this to the actual party ID dynamically
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    val newMessages = snapshots.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    chatAdapter.updateMessages(newMessages)

                    // Auto-scroll to the last message when a new one arrives
                    binding.recyclerView.post {
                        binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



class ChatAdapter(private var messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_message_item, parent, false)
        return ChatViewHolder(view).apply {
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = "${message.username}: ${message.timestamp}\n${message.msg}"
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}



