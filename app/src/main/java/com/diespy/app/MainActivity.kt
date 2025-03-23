package com.diespy.app

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.diespy.app.databinding.ActivityMainBinding
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force Light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setStatusBarColor(R.color.primary)
        SharedPrefManager.clearCurrentParty(this)
        applySystemBarInsets()
        FirebaseApp.initializeApp(this)

        setupCustomBottomNav()
        setupCustomTopNav()
    }

    private fun setupCustomBottomNav() {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        findViewById<View>(R.id.nav_members).setOnClickListener {
            if (navController.currentDestination?.id != R.id.membersFragment) {
                navController.navigate(R.id.membersFragment)
            }
        }

        findViewById<View>(R.id.nav_party).setOnClickListener {
            if (navController.currentDestination?.id != R.id.partyFragment) {
                navController.navigate(R.id.partyFragment)
            }
        }

        findViewById<View>(R.id.nav_chat).setOnClickListener {
            if (navController.currentDestination?.id != R.id.chatFragment) {
                navController.navigate(R.id.chatFragment)
            }
        }

        findViewById<View>(R.id.nav_logs).setOnClickListener {
            if (navController.currentDestination?.id != R.id.logsFragment) {
                navController.navigate(R.id.logsFragment)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val shouldShowBottomNav = destination.id in setOf(
                R.id.partyFragment,
                R.id.chatFragment,
                R.id.membersFragment,
                R.id.logsFragment,
                R.id.rollsFragment,
                R.id.diceDetectionFragment
            )
            findViewById<View>(R.id.customBottomNav).visibility = if (shouldShowBottomNav) View.VISIBLE else View.GONE
        }
    }

    private fun setupCustomTopNav() {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        val navBack = findViewById<View>(R.id.nav_back)
        val navSettings = findViewById<View>(R.id.nav_settings)
        val navCamera = findViewById<View>(R.id.nav_camera)
        val customTopNav = findViewById<View>(R.id.customTopNav)

        val backButton = navBack as? android.widget.ImageButton

        navSettings.setOnClickListener {
            if (navController.currentDestination?.id != R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment)
            }
        }

        navCamera.setOnClickListener {
            if (navController.currentDestination?.id != R.id.diceDetectionFragment) {
                navController.navigate(R.id.diceDetectionFragment)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showTopNav = destination.id in setOf(
                R.id.partyFragment,
                R.id.homeFragment,
                R.id.createAccountFragment,
                R.id.joinPartyFragment,
                R.id.createPartyFragment,
                R.id.chatFragment,
                R.id.membersFragment,
                R.id.logsFragment,
                R.id.loginFragment,
                R.id.settingsFragment,
                R.id.profileFragment,
                R.id.rollsFragment,
                R.id.changePasswordFragment,
                R.id.diceDetectionFragment
            )
            customTopNav.visibility = if (showTopNav) View.VISIBLE else View.GONE

            // Back/Home icon logic
            if (destination.id in setOf(
                    R.id.partyFragment,
                    R.id.chatFragment,
                    R.id.logsFragment,
                    R.id.membersFragment
                )) {
                backButton?.setImageResource(R.drawable.icon_home)
                navBack.setOnClickListener {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        navController.navigate(R.id.homeFragment)
                    }
                }
            } else {
                backButton?.setImageResource(R.drawable.icon_arrow_left)
                navBack.setOnClickListener {
                    if (navController.currentDestination?.id == R.id.diceDetectionFragment) {
                        navController.navigate(R.id.partyFragment)
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }

            // Icon visibility rules
            when (destination.id) {
                R.id.createAccountFragment,
                R.id.settingsFragment,
                R.id.profileFragment,
                R.id.diceDetectionFragment,
                R.id.changePasswordFragment,
                R.id.rollsFragment -> {
                    navBack.visibility = View.VISIBLE
                    navSettings.visibility = View.GONE
                    navCamera.visibility = View.GONE
                }
                R.id.loginFragment -> {
                    navBack.visibility = View.GONE
                    navSettings.visibility = View.GONE
                    navCamera.visibility = View.VISIBLE
                }
                R.id.homeFragment -> {
                    navBack.visibility = View.GONE
                    navSettings.visibility = View.VISIBLE
                    navCamera.visibility = View.VISIBLE
                }
                else -> {
                    navBack.visibility = View.VISIBLE
                    navSettings.visibility = View.VISIBLE
                    navCamera.visibility = View.VISIBLE
                }
            }
        }
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