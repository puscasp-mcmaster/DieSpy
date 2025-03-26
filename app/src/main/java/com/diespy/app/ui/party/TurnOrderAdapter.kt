package com.diespy.app.ui.party

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R

class TurnOrderAdapter(
    private var players: List<String>,
    private val onEndTurnClicked: () -> Unit
) : RecyclerView.Adapter<TurnOrderAdapter.PlayerViewHolder>() {

    var currentTurnIndex = 0
        private set

    fun setCurrentTurnIndex(index: Int) {
        currentTurnIndex = index
        notifyDataSetChanged()
    }

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.playerName)
        val endTurnButton: Button = view.findViewById(R.id.endTurnButton)
        val container: View = view.findViewById(R.id.playerContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.party_member_drag_and_drop, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val name = players[position].replaceFirstChar { it.uppercaseChar() }
        holder.usernameText.text = name

        val isCurrentTurn = position == currentTurnIndex

        // Highlight background based on current turn
        val ctx = holder.itemView.context
        val bubbleBackground = if (isCurrentTurn)
            ContextCompat.getDrawable(ctx, R.drawable.chat_bubble_background) // active
        else
            ContextCompat.getDrawable(ctx, R.drawable.chat_bubble_background_gray) // inactive

        holder.usernameText.background = bubbleBackground


        // Show/hide end turn button
        holder.endTurnButton.visibility = if (isCurrentTurn) View.VISIBLE else View.GONE
        holder.endTurnButton.setOnClickListener {
            if (isCurrentTurn) onEndTurnClicked()
        }
    }

    override fun getItemCount(): Int = players.size

    fun updatePlayers(newPlayers: List<String>) {
        players = newPlayers
        notifyDataSetChanged()
    }
}
