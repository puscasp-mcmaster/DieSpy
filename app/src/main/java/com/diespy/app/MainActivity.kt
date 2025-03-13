package com.diespy.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.diespy.app.databinding.ActivityMainBinding

import com.google.firebase.firestore.FirebaseFirestore

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

        // Apply padding adjustments to prevent system bars overlapping content
        applySystemBarInsets()

        // Initialize Navigation Component
        setupNavigation()

        // Step 1: Initialize Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Step 2: Create a sample data object to store in Firestore
        val testData = hashMapOf("testField" to "Hello, Firestore!")

        // Step 3: Add testData to Firestore in a collection named "testCollection"
        db.collection("testCollection").add(testData)
            .addOnSuccessListener { println("Firestore write successful!") }  // ✅ Success callback
            .addOnFailureListener { e -> println("Firestore write failed: $e") }  // ❌ Failure callback
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
    }

    /**
     * Sets the status bar color dynamically.
     */
    private fun setStatusBarColor(color: Int) {
        window?.statusBarColor = ContextCompat.getColor(baseContext, color)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }
}
