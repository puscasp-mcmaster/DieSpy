package com.diespy.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.diespy.app.databinding.ActivityMainBinding
import com.diespy.app.managers.profile.PartyCacheManager
import com.diespy.app.managers.profile.SharedPrefManager
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Force Light mode so we can use our own theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setStatusBarColor(R.color.primary)
        SharedPrefManager.clearCurrentPartyData(this)
        applySystemBarInsets()
        FirebaseApp.initializeApp(this)

        setupCustomBottomNav()
        setupCustomTopNav()
    }

    private fun setupCustomBottomNav() {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        val navMembers = findViewById<ImageButton>(R.id.nav_members)
        val navLogs = findViewById<ImageButton>(R.id.nav_logs)
        val navParty = findViewById<ImageButton>(R.id.nav_party)
        val navChat = findViewById<ImageButton>(R.id.nav_chat)

        val allIcons = listOf(navMembers, navLogs, navParty, navChat)

        fun highlightBottomIcon(active: ImageButton) {
            allIcons.forEach {
                it.setColorFilter(ContextCompat.getColor(this, R.color.nav_inactive))
            }
            active.setColorFilter(ContextCompat.getColor(this, R.color.nav_active))
        }

        navMembers.setOnClickListener {
            if (navController.currentDestination?.id != R.id.membersFragment) {
                navController.navigate(R.id.membersFragment)
            }
        }

        navLogs.setOnClickListener {
            if (navController.currentDestination?.id != R.id.logsFragment) {
                navController.navigate(R.id.logsFragment)
            }
        }

        navParty.setOnClickListener {
            if (navController.currentDestination?.id != R.id.partyFragment) {
                navController.navigate(R.id.partyFragment)
            }
        }

        navChat.setOnClickListener {
            if (navController.currentDestination?.id != R.id.chatFragment) {
                navController.navigate(R.id.chatFragment)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentPartyId = SharedPrefManager.getCurrentPartyId(this)
            val shouldShow = destination.id in setOf(
                R.id.partyFragment, R.id.chatFragment, R.id.membersFragment, R.id.logsFragment,
                R.id.diceDetectionFragment, R.id.simulationFragment
            ) && currentPartyId != null
            findViewById<View>(R.id.customBottomNav).visibility = if (shouldShow) View.VISIBLE else View.GONE

            when (destination.id) {
                R.id.membersFragment -> highlightBottomIcon(navMembers)
                R.id.logsFragment -> highlightBottomIcon(navLogs)
                R.id.partyFragment -> highlightBottomIcon(navParty)
                R.id.chatFragment -> highlightBottomIcon(navChat)
                else -> allIcons.forEach {
                    it.setColorFilter(ContextCompat.getColor(this, R.color.nav_inactive))
                }
            }
        }
    }

    private fun setupCustomTopNav() {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        val navBack = findViewById<View>(R.id.nav_back)
        val navProfile = findViewById<View>(R.id.nav_profile)
        val navCamera = findViewById<View>(R.id.nav_camera)
        val customTopNav = findViewById<View>(R.id.customTopNav)

        val backButton = navBack as? ImageButton
        val profileButton = navProfile as? ImageButton
        val cameraButton = navCamera as? ImageButton

        navProfile.setOnClickListener {
            if (navController.currentDestination?.id != R.id.profileFragment) {
                navController.navigate(R.id.profileFragment)
            }
        }

        navCamera.setOnClickListener {
            if (navController.currentDestination?.id != R.id.diceDetectionFragment) {
                navController.navigate(R.id.diceDetectionFragment)
            }
        }

        fun setEnabled(view: View, enabled: Boolean) {
            view.isEnabled = enabled
            view.alpha = if (enabled) 1f else 0.3f
        }

        fun highlightTopIcon(active: ImageButton?) {
            listOf(backButton, profileButton, cameraButton).forEach {
                it?.setColorFilter(ContextCompat.getColor(this, R.color.nav_inactive))
            }
            active?.setColorFilter(ContextCompat.getColor(this, R.color.nav_active))
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showTopNav = destination.id in setOf(
                R.id.partyFragment, R.id.homeFragment, R.id.createAccountFragment, R.id.joinPartyFragment,
                R.id.createPartyFragment, R.id.chatFragment, R.id.membersFragment, R.id.logsFragment,
                R.id.loginFragment, R.id.settingsFragment, R.id.profileFragment,
                R.id.changePasswordFragment, R.id.diceDetectionFragment, R.id.simulationFragment
            )
            customTopNav.visibility = if (showTopNav) View.VISIBLE else View.GONE

            if (destination.id in setOf(R.id.partyFragment, R.id.chatFragment, R.id.logsFragment, R.id.membersFragment)) {
                backButton?.setImageResource(R.drawable.icon_home)
                navBack.setOnClickListener {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        showLeavePartyConfirmation(navController)
                    }
                }
            } else {
                backButton?.setImageResource(R.drawable.icon_arrow_left)
                navBack.setOnClickListener {
                    if (navController.currentDestination?.id == R.id.diceDetectionFragment) {
                        val userId = SharedPrefManager.getCurrentUserId(this)
                        val partyId = SharedPrefManager.getCurrentPartyId(this)

                        when {
                            userId == null -> navController.navigate(R.id.loginFragment)
                            partyId == null -> navController.navigate(R.id.homeFragment)
                            else -> navController.navigate(R.id.partyFragment)
                        }
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }

            //Enable/disable icons for nav bar
            when (destination.id) {
                R.id.createAccountFragment, R.id.settingsFragment, R.id.profileFragment,
                R.id.diceDetectionFragment, R.id.changePasswordFragment -> {
                    setEnabled(navBack, true)
                    setEnabled(navProfile, false)
                    setEnabled(navCamera, false)
                }
                R.id.loginFragment -> {
                    setEnabled(navBack, false)
                    setEnabled(navProfile, false)
                    setEnabled(navCamera, true)
                }
                R.id.homeFragment -> {
                    setEnabled(navBack, false)
                    setEnabled(navProfile, true)
                    setEnabled(navCamera, true)
                }
                R.id.createPartyFragment, R.id.joinPartyFragment -> {
                    setEnabled(navBack, true)
                    setEnabled(navProfile, true)
                    setEnabled(navCamera, false)
                }
                else -> {
                    setEnabled(navBack, true)
                    setEnabled(navProfile, true)
                    setEnabled(navCamera, true)
                }
            }

            // Highlight active icon
            when (destination.id) {
                R.id.profileFragment -> highlightTopIcon(profileButton)
                R.id.diceDetectionFragment -> highlightTopIcon(cameraButton)
                else -> highlightTopIcon(null)
            }
        }
    }

    //Deals with leaving party and deleting cache if so
    fun showLeavePartyConfirmation(navController: androidx.navigation.NavController) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_leave_party_screen, null)

        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.homeButton).setOnClickListener {
            SharedPrefManager.clearCurrentPartyData(this)
            alertDialog.dismiss()
            PartyCacheManager.clear()
            navController.navigate(R.id.homeFragment)
        }
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        alertDialog.window?.setDimAmount(0.8f) // 0 = no dim, 1 = full black
        alertDialog.show()
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
