package com.diespy.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.diespy.app.databinding.FragmentChatBinding
import com.diespy.app.managers.chat.ChatManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
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
        //TODO remove this once we have party select working
        SharedPrefManager.saveCurrentParty(requireContext(),"cLkDPwOjRwmlXlhIh8s1",)
        val currentParty = SharedPrefManager.getCurrentParty(requireContext()) ?: ""
        //Party Screen button
        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_chat_to_party)
        }

        chatManager = ChatManager(requireContext())
        chatAdapter = ChatAdapter(emptyList()) // Start with an empty list
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
            val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
            //Tries to grab username, if there is not one, it defaults to User
            val username = (SharedPrefManager.getUsername(requireContext()))?: "User"
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                lifecycleScope.launch {
                    chatManager.saveMessage(username, message, timeStamp, currentParty)

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


//prints chat messages on screen
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
        //capitalizes first letter of the Username.
        val message = messages[position]
        holder.messageText.text = "${(message.username).replaceFirstChar { char ->char.titlecase()}}: ${message.timestamp}\n${message.msg}"
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}

private inline fun String.replaceFirstChar(transform: (Char) -> CharSequence): String {
    return if (isNotEmpty())
        transform(this[0]).toString() + substring(1)
    else
        this
}


