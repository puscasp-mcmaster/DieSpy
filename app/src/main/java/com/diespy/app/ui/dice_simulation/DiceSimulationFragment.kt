package com.diespy.app.ui.dice_simulation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.diespy.app.R
import com.diespy.app.ui.utils.showError
import com.diespy.app.databinding.FragmentDiceSimulationBinding
import com.diespy.app.managers.dice_sim.DiceSimulationManager

class DiceSimulationFragment : Fragment() {

    private var _binding: FragmentDiceSimulationBinding? = null
    private val binding get() = _binding!!
    private lateinit var diceAdapter: DiceSimulationManager
    private var diceCount = 8

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diceAdapter = DiceSimulationManager(emptyList())
        binding.diceRecyclerView.apply {
            // Assuming you set a span count of 4 in your GridLayoutManage
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
            if (diceCount < 100) diceCount += 1
            binding.diceCountEditText.setText(diceCount.toString())
        }

        binding.diceCountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = binding.diceCountEditText.text.toString().toIntOrNull()
                if (input != null && input > 0 && input < 101) {
                    binding.countErrorMessage.visibility = View.GONE
                    diceCount = input
                } else {
                    binding.diceCountEditText.setText(diceCount.toString())
                    binding.countErrorMessage.showError("Number has to be between 0 and 100.")
                }
            }
        }

        binding.diceCountEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.diceCountEditText.clearFocus()
                hideKeyboard()
                val input = binding.diceCountEditText.text.toString().toIntOrNull()
                if (input != null && input > 0 && input < 101) {
                    diceCount = input
                } else {
                    binding.diceCountEditText.setText(diceCount.toString())
                    binding.countErrorMessage.showError("Number has to be between 1 and 100.")
                }
                true
            } else {
                false
            }
        }

        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            binding.diceCountEditText.clearFocus()
            false
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

    fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
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
