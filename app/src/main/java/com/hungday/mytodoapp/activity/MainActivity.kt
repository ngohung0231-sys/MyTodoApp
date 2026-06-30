package com.hungday.mytodoapp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hungday.mytodoapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Handle start destination based on profile setup
        val sharedPref = getSharedPreferences("MyTodoPrefs", android.content.Context.MODE_PRIVATE)
        val isProfileSetup = sharedPref.getBoolean("IS_PROFILE_SETUP", false)

        if (isProfileSetup) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(R.id.homeFragment)
            navController.graph = navGraph
        }

        // Setup with NavController
        bottomNavigationView.setupWithNavController(navController)

        // Custom listener to fix the "Home button unresponsiveness" issue
        bottomNavigationView.setOnItemSelectedListener { item ->
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, false, true)

            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment, null, builder.build())
                    true
                }
                R.id.taskFragment -> {
                    navController.navigate(R.id.taskFragment, null, builder.build())
                    true
                }
                R.id.calenderFragment -> {
                    navController.navigate(R.id.calenderFragment, null, builder.build())
                    true
                }
                R.id.addTaskFragment -> {
                    navController.navigate(R.id.addTaskFragment, null, builder.build())
                    true
                }
                else -> false
            }
        }

        // Visibility handling
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.taskFragment, R.id.calenderFragment, R.id.addTaskFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
                else -> {
                    bottomNavigationView.visibility = View.GONE
                }
            }
        }
    }
}