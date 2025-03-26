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

                    //Reload messages after sending
                    val updatedMessages = chatManager.loadMessages(currentParty)
                    chatAdapter.updateMessages(updatedMessages)
                    binding.messageInput.text.clear()

                    //Scroll to the last message
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