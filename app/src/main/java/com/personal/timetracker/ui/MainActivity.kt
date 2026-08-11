package com.personal.timetracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.personal.timetracker.App
import com.personal.timetracker.R
import com.personal.timetracker.databinding.ActivityMainBinding
import com.personal.timetracker.ui.attendance.AttendanceFragment
import com.personal.timetracker.ui.calendar.CalendarFragment
import com.personal.timetracker.ui.dashboard.DashboardFragment
import com.personal.timetracker.ui.reports.ReportsFragment
import com.personal.timetracker.ui.settings.SettingsFragment
import com.personal.timetracker.ui.tasks.TasksFragment
import com.personal.timetracker.util.BiometricHelper
import com.personal.timetracker.util.GeoHelper
import com.personal.timetracker.util.NotifHelper
import com.personal.timetracker.util.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Implemented by fragments that paint their own colors, so MainActivity can refresh them
 *  in place instead of requiring the user to navigate away and back. */
interface Themable {
    fun refreshTheme(primary: Int, dark: Boolean)
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var unlocked = false
    private var firstFragmentOpened = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            try { GeoHelper.requestAndCheck(this) } catch (_: Exception) {}
            requestBackgroundLocationIfNeeded()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: periodic background worker checks permission itself before use */ }

    var primaryColor: Int = 0xFF1565C0.toInt()
        private set
    var isDark: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Night mode must be set BEFORE super/onCreate content for proper theme
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val darkPref = prefs.getBoolean("dark", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkPref) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        try {
            NotifHelper.ensureChannel(this)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            lifecycleScope.launch {
                try {
                    val settings = withContext(Dispatchers.IO) {
                        (application as App).repository.getSettings()
                    }
                    primaryColor = settings.themeColor
                    isDark = settings.isDarkMode

                    // sync prefs
                    prefs.edit().putBoolean("dark", settings.isDarkMode).apply()
                    if (settings.isDarkMode != darkPref) {
                        AppCompatDelegate.setDefaultNightMode(
                            if (settings.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO
                        )
                        recreate()
                        return@launch
                    }

                    applyChrome(settings.themeColor, settings.isDarkMode)

                    // Only now that isDark/primaryColor are the real values do we open the
                    // first fragment — this is what fixes dark mode not applying until the
                    // user manually switches tabs once.
                    if (savedInstanceState == null && !firstFragmentOpened) {
                        firstFragmentOpened = true
                        open(DashboardFragment())
                    }

                    try { NotifHelper.scheduleGeoBackgroundCheck(this@MainActivity) } catch (_: Exception) {}

                    if (settings.biometricEnabled && !unlocked) {
                        binding.root.visibility = View.INVISIBLE
                        if (BiometricHelper.canAuthenticate(this@MainActivity)) {
                            BiometricHelper.prompt(
                                this@MainActivity,
                                onSuccess = {
                                    unlocked = true
                                    binding.root.visibility = View.VISIBLE
                                },
                                onFail = {
                                    Toast.makeText(this@MainActivity, "احراز هویت ناموفق", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            )
                        } else {
                            binding.root.visibility = View.VISIBLE
                            unlocked = true
                        }
                    } else {
                        unlocked = true
                    }
                } catch (e: Exception) {
                    Log.e("PTT", "init", e)
                    unlocked = true
                    if (savedInstanceState == null && !firstFragmentOpened) {
                        firstFragmentOpened = true
                        applyChrome(primaryColor, isDark)
                        open(DashboardFragment())
                    }
                }
            }

            binding.bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> open(DashboardFragment())
                    R.id.nav_attendance -> open(AttendanceFragment())
                    R.id.nav_tasks -> open(TasksFragment())
                    R.id.nav_reports -> open(ReportsFragment())
                    R.id.nav_settings -> open(SettingsFragment())
                }
                true
            }

            requestRuntimePermissions()
            try { GeoHelper.requestAndCheck(this) } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("PTT", "MainActivity", e)
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun applyChrome(primary: Int, dark: Boolean) {
        primaryColor = primary
        isDark = dark
        window.statusBarColor = if (dark) Color.parseColor("#121212") else primary
        window.navigationBarColor = ThemeHelper.surfaceCard(dark)
        binding.root.setBackgroundColor(ThemeHelper.surface(dark))
        ThemeHelper.applyBottomNav(binding.bottomNav, primary, dark)
        // Refresh the currently visible fragment in place if it supports it, so theme/color
        // changes made from Settings apply immediately without needing to switch tabs.
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        (current as? Themable)?.refreshTheme(primary, dark)
    }

    /** Call after saving theme settings */
    fun applyThemeMode(dark: Boolean) {
        getSharedPreferences("theme_prefs", MODE_PRIVATE)
            .edit().putBoolean("dark", dark).apply()
        val mode = if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
            recreate()
        } else {
            applyChrome(primaryColor, dark)
        }
    }

    fun applyThemeColor(color: Int) {
        primaryColor = color
        applyChrome(color, isDark)
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    /**
     * On Android 10+ foreground and background location are separate permissions.
     * Requesting background location is only meaningful once foreground access is granted,
     * and is what allows the geofence-based auto check-in / notifications to keep working
     * while the app is closed instead of only while it's open in the foreground.
     */
    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return
        val bgGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!bgGranted) {
            try {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } catch (_: Exception) { }
        }
    }

    fun openCalendar() {
        open(CalendarFragment())
    }

    private fun open(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
    }
}
