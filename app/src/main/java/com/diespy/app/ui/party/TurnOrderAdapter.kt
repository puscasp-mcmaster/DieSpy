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

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.playerName)
        val endTurnButton: Button = view.findViewById(R.id.endTurnButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_turn_order_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val name = players[position].replaceFirstChar { it.uppercaseChar() }
        holder.usernameText.text = name

        if (position == 0) {
            // Highlight the current turn holder
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_accent))
            holder.endTurnButton.visibility = View.VISIBLE
            holder.endTurnButton.setOnClickListener { onEndTurnClicked() }
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
            holder.endTurnButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = players.size

    fun updatePlayers(newPlayers: List<String>) {
        players = newPlayers
        notifyDataSetChanged()
    }
}
