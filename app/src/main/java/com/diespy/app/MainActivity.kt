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

        // View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setStatusBarColor(R.color.primary)
        SharedPrefManager.clearCurrentParty(this)
        applySystemBarInsets()
        FirebaseApp.initializeApp(this)

        setupCustomBottomNav()
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
            val shouldShowCustomNav = destination.id in setOf(
                R.id.partyFragment,
                R.id.chatFragment,
                R.id.membersFragment,
                R.id.logsFragment
            )
            findViewById<View>(R.id.customBottomNav).visibility = if (shouldShowCustomNav) View.VISIBLE else View.GONE
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
