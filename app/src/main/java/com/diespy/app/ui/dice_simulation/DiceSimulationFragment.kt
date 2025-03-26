package com.diespy.app.ui.dice_simulation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.diespy.app.databinding.FragmentDiceSimulationBinding

class DiceSimulationFragment : Fragment() {

    private var _binding: FragmentDiceSimulationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var diceCount = 10

        binding.rollButton.setOnClickListener {
            val input = binding.diceCountEditText.text.toString().toIntOrNull()
            val count = if (input != null && input > 0) input else diceCount
            diceCount = count
            val result = simulateDiceRoll(count)
            displayResults(result)
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

    private fun simulateDiceRoll(count: Int): IntArray {
        val result = IntArray(6)
        val random = java.util.Random()
        repeat(count) {
            val face = random.nextInt(6)
            result[face] += 1
        }
        return result
    }


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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
