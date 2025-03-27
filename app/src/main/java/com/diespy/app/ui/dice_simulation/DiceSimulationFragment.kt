package com.diespy.app.ui.dice_simulation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.diespy.app.R
import com.diespy.app.ui.utils.showError
import com.diespy.app.databinding.FragmentDiceSimulationBinding
import com.diespy.app.managers.dice_sim.DiceSimulationManager
import com.diespy.app.managers.logs.LogManager
import com.diespy.app.managers.profile.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.Date

class DiceSimulationFragment : Fragment() {

    private var _binding: FragmentDiceSimulationBinding? = null
    private val binding get() = _binding!!
    private lateinit var diceAdapter: DiceSimulationManager
    private var diceCount = 8
    private var rollHandler: Handler? = null
    private var rollRunnable: Runnable? = null
    private var lastRollCounts: IntArray? = null
    private lateinit var logManager: LogManager
    private var logged = false
    private var activeToast: Toast? = null
    private var rolling = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiceSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logManager = LogManager(requireContext())

        diceAdapter = DiceSimulationManager(emptyList())
        binding.diceRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = diceAdapter
        }

        binding.rollButton.setOnClickListener {
            if (!rolling) {
                rolling = true
                val input = binding.diceCountEditText.text.toString().toIntOrNull()
                val count = if (input != null && input > 0) input else diceCount
                diceCount = count
                showRollingDice(count) { counts ->
                    results(counts)
                    rolling = false
                }
                logged = false
            }else{
                activeToast?.cancel()
                activeToast = Toast.makeText(
                    requireContext(),
                    "Dice roll in progress.",
                    Toast.LENGTH_SHORT
                )
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

        binding.logRollButton.setOnClickListener {
            if (!logged) {
                val currentParty = SharedPrefManager.getCurrentPartyId(requireContext()) ?: ""
                val username = SharedPrefManager.getCurrentUsername(requireContext()) ?: "User"
                val counts = lastRollCounts

                if (currentParty.isEmpty()) {
                    activeToast?.cancel()
                    activeToast = Toast.makeText(
                        requireContext(),
                        "No party found. Join one to log rolls.",
                        Toast.LENGTH_SHORT
                    )
                    activeToast?.show()
                    return@setOnClickListener
                }

                if (counts == null) {
                    activeToast?.cancel()
                    activeToast = Toast.makeText(
                        requireContext(),
                        "Roll first before logging.",
                        Toast.LENGTH_SHORT
                    )
                    activeToast?.show()
                    return@setOnClickListener
                }

                val log = "1: ${counts[0]}      4: ${counts[3]}\n" +
                        "2: ${counts[1]}      5: ${counts[4]}\n" +
                        "3: ${counts[2]}      6: ${counts[5]}"

                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                logManager.saveLog(username, log, timestamp, currentParty)
                activeToast?.cancel()
                activeToast = Toast.makeText(requireContext(), "Roll has been logged!", Toast.LENGTH_SHORT)
                activeToast?.show()
                logged = true
            } else {
                activeToast?.cancel()
                activeToast = Toast.makeText(requireContext(), "Roll has already been logged.", Toast.LENGTH_SHORT)
                activeToast?.show()
            }
        }

        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            binding.diceCountEditText.clearFocus()
            false
        }
    }

    private fun showRollingDice(count: Int, onComplete: (IntArray) -> Unit) {
        val initialList = List(count) { R.drawable.dice_spin }
        diceAdapter.updateData(initialList)

        rollHandler = Handler(Looper.getMainLooper())
        rollRunnable = Runnable {
            if (!isAdded) return@Runnable

            val outcomes = List(count) { (1..6).random() }
            val diceImages = outcomes.map { face -> getDiceFaceRes(face) }
            diceAdapter.updateData(diceImages)

            val counts = IntArray(6)
            outcomes.forEach { face -> counts[face - 1]++ }
            onComplete(counts)
        }

        rollHandler?.postDelayed(rollRunnable!!, 800)
    }

    fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
    }

    private fun results(rolls: IntArray) {
        val total = rolls.withIndex().sumOf { (i, count) -> (i + 1) * count }
        lastRollCounts = rolls
        binding.simulationResultText.text = "You rolled a total of $total"
        binding.face1.text = "1: ${rolls[0]}"
        binding.face2.text = "2: ${rolls[1]}"
        binding.face3.text = "3: ${rolls[2]}"
        binding.face4.text = "4: ${rolls[3]}"
        binding.face5.text = "5: ${rolls[4]}"
        binding.face6.text = "6: ${rolls[5]}"
    }

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
        rollHandler?.removeCallbacks(rollRunnable ?: return)
        rollRunnable = null
        rollHandler = null
        activeToast?.cancel()
        activeToast = null
        _binding = null
        super.onDestroyView()
    }
}
