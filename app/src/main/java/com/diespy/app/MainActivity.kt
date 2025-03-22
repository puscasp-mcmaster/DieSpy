package com.diespy.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.diespy.app.databinding.ActivityMainBinding
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force Light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Set up view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setStatusBarColor(R.color.primary)
        SharedPrefManager.clearCurrentParty(this)
        applySystemBarInsets()
        FirebaseApp.initializeApp(this)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect BottomNavView with NavController (auto handles tab switching)
        NavigationUI.setupWithNavController(binding.bottomNavView, navController)

        // Hide bottom nav on certain screens
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
                }
                else -> {
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }

            Log.d("NavDebug", "Currently at destination: ${destination.label}")
        }
    }

    fun setTab(tabId: Int) {
        binding.bottomNavView.selectedItemId = tabId
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setStatusBarColor(color: Int) {
        window.statusBarColor = ContextCompat.getColor(baseContext, color)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }
}
