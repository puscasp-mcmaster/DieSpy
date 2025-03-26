package com.diespy.app.ui.party

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

    companion object {
        private const val VIEW_TYPE_ACTIVE = 0
        private const val VIEW_TYPE_INACTIVE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == currentTurnIndex) VIEW_TYPE_ACTIVE else VIEW_TYPE_INACTIVE
    }

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.playerName)
        val endTurnButton: Button? = view.findViewById(R.id.endTurnButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_ACTIVE) {
            R.layout.party_member_active
        } else {
            R.layout.party_member_inactive
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val name = players[position].replaceFirstChar { it.uppercaseChar() }
        holder.usernameText.text = name

        if (position == currentTurnIndex) {
            holder.endTurnButton?.visibility = View.VISIBLE
            holder.endTurnButton?.setOnClickListener { onEndTurnClicked() }
        } else {
            holder.endTurnButton?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = players.size

    fun updatePlayers(newPlayers: List<String>) {
        players = newPlayers
        notifyDataSetChanged()
    }
}
