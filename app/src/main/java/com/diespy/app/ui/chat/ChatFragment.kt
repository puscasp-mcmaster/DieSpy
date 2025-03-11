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
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.diespy.app.R

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
        binding.recyclerView.post {
            binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_chat_to_party)
        }

        chatManager = ChatManager(requireContext())

        chatAdapter = ChatAdapter(chatManager.loadMessages())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = chatAdapter

        binding.sendButton.setOnClickListener {
            val username = "User" // Replace with actual username logic
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                chatManager.saveMessage(username, message)
                chatAdapter.updateMessages(chatManager.loadMessages())
                binding.messageInput.text.clear()
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

class ChatAdapter(private var messages: List<JSONObject>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
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
        val messageObject = messages[position]
        holder.messageText.setText("${messageObject.getString("username")}: ${messageObject.getString("message")}")
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<JSONObject>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
