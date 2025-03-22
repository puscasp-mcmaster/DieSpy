package com.diespy.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.diespy.app.databinding.ActivityMainBinding
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.firebase.FirebaseApp

/**
 * MainActivity serves as the entry point of the application.
 * It manages the navigation host and applies UI enhancements like edge-to-edge display.
 */
class MainActivity : AppCompatActivity() {

    // View binding for accessing views in the layout file
    private lateinit var binding: ActivityMainBinding

    // Navigation controller for managing app navigation
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Forces Light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge layout display
        enableEdgeToEdge()
        setStatusBarColor(R.color.primary)
        SharedPrefManager.clearCurrentParty(this)
        // Apply padding adjustments to prevent system bars overlapping content
        applySystemBarInsets()

        // Initialize Navigation Component
        setupNavigation()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }

    /**
     * Configures padding adjustments to prevent system bars (status & navigation bar) from overlapping content.
     */
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Initializes the Navigation Component by binding the NavController to the NavHostFragment.
     */
    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Handle bottom nav item selection
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.partyFragment -> R.id.partyFragment
                R.id.chatFragment -> R.id.chatFragment
                R.id.logsFragment -> R.id.logsFragment
                R.id.membersFragment -> R.id.membersFragment
                else -> null
            }

            destinationId?.let {
                if (navController.currentDestination?.id != it) {
                    navController.popBackStack(it, false)
                    navController.navigate(it)
                }
                true
            } ?: false
        }

        // Show/hide bottom nav and handle item highlighting
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.createAccountFragment,
                R.id.homeFragment,
                R.id.createPartyFragment,
                R.id.joinPartyFragment,
                R.id.settingsFragment,
                R.id.profileFragment -> {
                    binding.bottomNavView.visibility = View.GONE
                    binding.bottomNavView.menu.setGroupCheckable(0, false, true) // Deselect items
                }
                else -> {
                    binding.bottomNavView.visibility = View.VISIBLE
                    binding.bottomNavView.menu.setGroupCheckable(0, true, true)

                    // Update selected item in nav bar
                    val menuItemId = when (destination.id) {
                        R.id.partyFragment -> R.id.partyFragment
                        R.id.chatFragment -> R.id.chatFragment
                        R.id.logsFragment -> R.id.logsFragment
                        R.id.membersFragment -> R.id.membersFragment
                        else -> null
                    }
                    menuItemId?.let {
                        binding.bottomNavView.selectedItemId = it
                    }
                }
            }

            Log.d("NavDebug", "Currently at destination: ${destination.label}")
        }
    }


    /**
     * Sets the status bar color dynamically.
     */
    private fun setStatusBarColor(color: Int) {
        window?.statusBarColor = ContextCompat.getColor(baseContext, color)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }
}
