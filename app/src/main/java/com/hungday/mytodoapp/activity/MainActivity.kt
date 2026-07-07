package com.hungday.mytodoapp.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hungday.mytodoapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var mainNavHostFragment: View

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Khởi tạo Splash Screen API
        installSplashScreen()

        // 2. Thiết lập Theme nội dung dựa trên lựa chọn người dùng
        val sharedPref = getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        
        val isPinkTheme = sharedPref.getBoolean("IS_PINK_THEME", false)

        if (isPinkTheme) {
            setTheme(R.style.Theme_MyTodoApp_Pink)
        } else {
            setTheme(R.style.Theme_MyTodoApp)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Components
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAddTask = findViewById(R.id.fabAddTask)
        mainNavHostFragment = findViewById<View>(R.id.mainNavHostFragment)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Khôi phục trạng thái điều hướng
        val isProfileSetup = sharedPref.getBoolean("IS_PROFILE_SETUP", false)
        if (isProfileSetup) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(R.id.homeFragment)
            navController.graph = navGraph
        }

        bottomNavigationView.setupWithNavController(navController)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        fabAddTask.setOnClickListener {
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, false, true)
            navController.navigate(R.id.addTaskFragment, null, builder.build())
        }

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
                R.id.settingFragment -> {
                    navController.navigate(R.id.settingFragment, null, builder.build())
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val params = mainNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams
            val density = resources.displayMetrics.density
            
            when (destination.id) {
                R.id.homeFragment, R.id.taskFragment, R.id.calenderFragment, R.id.settingFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    fabAddTask.visibility = View.VISIBLE
                    params.setMargins(0, 0, 0, (70 * density).toInt())
                }
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    fabAddTask.visibility = View.GONE
                    params.setMargins(0, 0, 0, 0)
                }
            }
            mainNavHostFragment.layoutParams = params
        }
    }
}