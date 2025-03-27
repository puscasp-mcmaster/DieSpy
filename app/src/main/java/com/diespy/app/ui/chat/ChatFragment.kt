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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatManager: ChatManager
    private lateinit var chatAdapter: ChatAdapter

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
            val timestamp = Timestamp.now()

            //Tries to grab username, if there is not one, it defaults to User
            val username = (SharedPrefManager.getCurrentUsername(requireContext()))?: "User"
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                lifecycleScope.launch {
                    chatManager.saveMessage(username, message, timestamp, currentParty)

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

        chatManager.subscribeToChatMessages(currentParty) { newMessages ->
            _binding?.let { binding ->
                chatAdapter.updateMessages(newMessages)
                val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val lastVisibleItem = it.findLastVisibleItemPosition()
                    // Check if the user is near the bottom (within 2 items)
                    if (lastVisibleItem >= chatAdapter.itemCount - 3) {
                        binding.recyclerView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                            override fun onLayoutChange(
                                v: View, left: Int, top: Int, right: Int, bottom: Int,
                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                            ) {
                                binding.recyclerView.removeOnLayoutChangeListener(this)
                                binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}