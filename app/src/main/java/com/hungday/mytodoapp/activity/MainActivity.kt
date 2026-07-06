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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hungday.mytodoapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var mainNavHostFragment: View

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAddTask = findViewById(R.id.fabAddTask)
        mainNavHostFragment = findViewById(R.id.mainNavHostFragment)
        
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

        // Fix bottom gap by handling insets manually
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, insets ->
            v.setPadding(0, 0, 0, 0) // Force zero padding to touch bottom
            insets
        }

        // Xử lý sự kiện click cho FAB để mở màn hình Add Task
        fabAddTask.setOnClickListener {
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, false, true)
            navController.navigate(R.id.addTaskFragment, null, builder.build())
        }

        // Custom listener để xử lý điều hướng cho các tab khác
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

        // Visibility handling cho cả BottomNav và FAB
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.taskFragment, R.id.calenderFragment, R.id.settingFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    fabAddTask.visibility = View.VISIBLE
                    // Thêm padding cho nội dung khi có BottomNav (70dp = 70 * mật độ màn hình)
                    val density = resources.displayMetrics.density
                    mainNavHostFragment.setPadding(0, 0, 0, (70 * density).toInt())
                }
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    fabAddTask.visibility = View.GONE
                    // Gỡ bỏ padding khi không có BottomNav
                    mainNavHostFragment.setPadding(0, 0, 0, 0)
                }
            }
        }
    }
}
