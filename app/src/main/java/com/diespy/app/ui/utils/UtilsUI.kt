package com.diespy.app.ui.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.diespy.app.R

fun TextView.showError(message: String) {
    text = message
    visibility = View.VISIBLE
}

fun TextView.clearError() {
    text = ""
    visibility = View.GONE
}

fun diceParse(breakdown: String): IntArray {
    val regex = Regex("""(\d+)s?:\s*(\d+)""")
    val countsMap = mutableMapOf<Int, Int>()
    regex.findAll(breakdown).forEach { result ->
        val (faceStr, countStr) = result.destructured
        countsMap[faceStr.toInt()] = countStr.toInt()
    }
    return IntArray(6) { index ->
        val face = index + 1
        (countsMap[face] ?: 0) + (countsMap[face + 6] ?: 0)
    }
}

//Dialog box used by dice screen and logs
@SuppressLint("SetTextI18n")
fun showEditRollDialog(
    context: Context,
    rolls: IntArray,
    onSave: (updatedRolls: IntArray) -> Unit
) {
    val inflater = LayoutInflater.from(context)
    val dialogView = inflater.inflate(R.layout.dialog_edit_log_quantity, null)

    val faceViews = listOf(
        Triple(
            dialogView.findViewById<TextView>(R.id.face1_count),
            dialogView.findViewById<Button>(R.id.face1_minus),
            dialogView.findViewById<Button>(R.id.face1_plus)
        ),
        Triple(
            dialogView.findViewById<TextView>(R.id.face2_count),
            dialogView.findViewById<Button>(R.id.face2_minus),
            dialogView.findViewById<Button>(R.id.face2_plus)
        ),
        Triple(
            dialogView.findViewById<TextView>(R.id.face3_count),
            dialogView.findViewById<Button>(R.id.face3_minus),
            dialogView.findViewById<Button>(R.id.face3_plus)
        ),
        Triple(
            dialogView.findViewById<TextView>(R.id.face4_count),
            dialogView.findViewById<Button>(R.id.face4_minus),
            dialogView.findViewById<Button>(R.id.face4_plus)
        ),
        Triple(
            dialogView.findViewById<TextView>(R.id.face5_count),
            dialogView.findViewById<Button>(R.id.face5_minus),
            dialogView.findViewById<Button>(R.id.face5_plus)
        ),
        Triple(
            dialogView.findViewById<TextView>(R.id.face6_count),
            dialogView.findViewById<Button>(R.id.face6_minus),
            dialogView.findViewById<Button>(R.id.face6_plus)
        )
    )

    //Populate and set up button logic
    faceViews.forEachIndexed { index, (countView, minusBtn, plusBtn) ->
        countView.text = rolls[index].toString()

        minusBtn.setOnClickListener {
            val current = countView.text.toString().toIntOrNull() ?: 0
            if (current > 0) countView.text = (current - 1).toString()
        }

        plusBtn.setOnClickListener {
            val current = countView.text.toString().toIntOrNull() ?: 0
            countView.text = (current + 1).toString()
        }
    }

    val customTitle = inflater.inflate(R.layout.custom_dialog_title, null)

    val dialog = AlertDialog.Builder(context)
        .setCustomTitle(customTitle)
        .setView(dialogView)
        .setPositiveButton("Save") { _, _ ->
            val updatedRolls = faceViews.map { (countView, _, _) ->
                countView.text.toString().toIntOrNull() ?: 0
            }.toIntArray()

            onSave(updatedRolls)
        }
        .setNegativeButton("Cancel", null)
        .create()

    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    dialog.show()

    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        ?.setTextColor(ContextCompat.getColor(context, R.color.primary_accent))
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        ?.setTextColor(ContextCompat.getColor(context, R.color.secondary_accent))
}
