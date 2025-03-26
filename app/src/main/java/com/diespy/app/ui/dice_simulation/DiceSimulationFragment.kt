package com.diespy.app.ui.dice_simulation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.diespy.app.R
import com.diespy.app.databinding.FragmentDiceSimulationBinding
import com.diespy.app.managers.dice_sim.DiceSimulationManager

class DiceSimulationFragment : Fragment() {

    private var _binding: FragmentDiceSimulationBinding? = null
    private val binding get() = _binding!!
    private lateinit var diceAdapter: DiceSimulationManager
    private var diceCount = 4

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView with a GridLayoutManager (e.g., 3 columns)
        diceAdapter = DiceSimulationManager(emptyList())
        binding.diceRecyclerView.apply {
            // Assuming you set a span count of 4 in your GridLayoutManager
            val horizontalSpacing = resources.getDimensionPixelSize(R.dimen.dice_horizontal_spacing) // set to 4dp
            val verticalSpacing = resources.getDimensionPixelSize(R.dimen.dice_vertical_spacing)     // set to 2dp

            val spanCount = 4
            binding.diceRecyclerView.addItemDecoration(
                GridSpacingDecoration(spanCount, horizontalSpacing, verticalSpacing)
            )
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = diceAdapter
        }

        binding.rollButton.setOnClickListener {
            val input = binding.diceCountEditText.text.toString().toIntOrNull()
            val count = if (input != null && input > 0) input else diceCount
            diceCount = count
            showRollingDice(count) { counts ->
                displayResults(counts)
            }
        }

        binding.decreaseDiceCountButton.setOnClickListener {
            diceCount = maxOf(1, diceCount - 1)
            binding.diceCountEditText.setText(diceCount.toString())
        }

        binding.increaseDiceCountButton.setOnClickListener {
            diceCount += 1
            binding.diceCountEditText.setText(diceCount.toString())
        }

        binding.diceCountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = binding.diceCountEditText.text.toString().toIntOrNull()
                if (input != null && input > 0) {
                    diceCount = input
                } else {
                    binding.diceCountEditText.setText(diceCount.toString())
                }
            }
        }
    }

    /**
     * Shows spinning dice in the RecyclerView then, after a delay, updates them to show final results.
     */
    private fun showRollingDice(count: Int, onComplete: (IntArray) -> Unit) {
        // Initially fill with spinning dice images
        val initialList = List(count) { R.drawable.dice_spin }
        diceAdapter.updateData(initialList)

        // Delay to simulate spinning animation (800ms)
        Handler(Looper.getMainLooper()).postDelayed({
            // Generate individual dice outcomes (each value between 1 and 6)
            val outcomes = List(count) { (1..6).random() }
            // Convert outcomes to image resource IDs
            val diceImages = outcomes.map { face -> getDiceFaceRes(face) }
            diceAdapter.updateData(diceImages)

            // Aggregate the counts for display
            val counts = IntArray(6)
            outcomes.forEach { face ->
                counts[face - 1]++
            }
            onComplete(counts)
        }, 800)
    }

    /**
     * Displays the aggregated result.
     */
    private fun displayResults(rolls: IntArray) {
        val total = rolls.withIndex().sumOf { (i, count) -> (i + 1) * count }
        binding.simulationResultText.text = "You rolled a total of $total"

        binding.face1.text = "1: ${rolls[0]}"
        binding.face2.text = "2: ${rolls[1]}"
        binding.face3.text = "3: ${rolls[2]}"
        binding.face4.text = "4: ${rolls[3]}"
        binding.face5.text = "5: ${rolls[4]}"
        binding.face6.text = "6: ${rolls[5]}"
    }

    /**
     * Returns the drawable resource ID for the given dice face.
     */
    private fun getDiceFaceRes(face: Int): Int {
        return when (face) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            6 -> R.drawable.dice_6
            else -> R.drawable.dice_spin
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
