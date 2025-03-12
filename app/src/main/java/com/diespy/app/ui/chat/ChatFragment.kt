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
import java.text.SimpleDateFormat
import java.util.Date

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

        //Party Screen button
        binding.toPartyScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_chat_to_party)
        }

        chatManager = ChatManager(requireContext())
        chatAdapter = ChatAdapter(chatManager.loadMessages())
        // Set layout manager with stackFromEnd = true (messages start at bottom)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerView.adapter = chatAdapter

        //Moves chat to latest
        binding.recyclerView.post {
            binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount)
        }
        //Send message button
        binding.sendButton.setOnClickListener {
            val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
            val username = "User" // Replace with actual username logic
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                chatManager.saveMessage(username, message, timeStamp)
                chatAdapter.updateMessages(chatManager.loadMessages())
                binding.messageInput.text.clear()

                //Scroll to last message after sending
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

//Handles displaying chat messages
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
        holder.messageText.setText("${messageObject.getString("username")}: ${messageObject.getString("timestamp")} \n${messageObject.getString("message")}")
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<JSONObject>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
