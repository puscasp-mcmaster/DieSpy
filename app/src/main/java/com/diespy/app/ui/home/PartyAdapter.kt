package com.diespy.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.diespy.app.R

data class PartyItem(val id: String, val name: String, val userCount: Int, val ipAddress : String? = null, val port : String? = null)

class PartyAdapter(
    private val parties: List<PartyItem>,
    private val onItemClick: (party: PartyItem) -> Unit
) : RecyclerView.Adapter<PartyAdapter.PartyViewHolder>() {

    class PartyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val partyName: TextView = itemView.findViewById(R.id.partyName)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
        val memberIcon: ImageView = itemView.findViewById(R.id.memberIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_party, parent, false)
        return PartyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartyViewHolder, position: Int) {
        val party = parties[position]
        holder.partyName.text = party.name
        holder.memberCount.text = "${party.userCount}"
        holder.itemView.setOnClickListener { onItemClick(party) }
    }

    override fun getItemCount(): Int = parties.size
}
