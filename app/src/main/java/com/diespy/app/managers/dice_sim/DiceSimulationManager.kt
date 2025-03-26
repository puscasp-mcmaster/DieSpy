package com.diespy.app.managers.dice_sim

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.diespy.app.R

class DiceSimulationManager(private var diceList: List<Int>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<DiceSimulationManager.DiceViewHolder>() {

    inner class DiceViewHolder(val imageView: ImageView) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiceViewHolder {
        val imageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dice, parent, false) as ImageView
        return DiceViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: DiceViewHolder, position: Int) {
        holder.imageView.setImageResource(diceList[position])
    }

    override fun getItemCount(): Int = diceList.size

    fun updateData(newData: List<Int>) {
        diceList = newData
        notifyDataSetChanged()
    }
}
