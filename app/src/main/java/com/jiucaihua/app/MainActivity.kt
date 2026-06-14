package com.jiucaihua.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jiucaihua.app.presentation.navigation.AppNavHost
import com.jiucaihua.app.presentation.settings.SettingsViewModel
import com.jiucaihua.app.presentation.theme.JiucaihuaTheme
import com.jiucaihua.app.presentation.navigation.NavExtras
import com.jiucaihua.app.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    @Named("appPrefs")
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationNavigation(intent)
        setContent {
            var darkModePref by remember {
                mutableStateOf(
                    if (prefs.contains(SettingsViewModel.KEY_DARK_MODE)) {
                        prefs.getBoolean(SettingsViewModel.KEY_DARK_MODE, false)
                    } else null
                )
            }
            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == SettingsViewModel.KEY_DARK_MODE) {
                        darkModePref = if (sharedPreferences.contains(key)) {
                            sharedPreferences.getBoolean(key, false)
                        } else null
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
            val isDark = darkModePref ?: isSystemInDarkTheme()
            JiucaihuaTheme(darkTheme = isDark) {
                AppNavHost(initialDestination = pendingNavDestination)
                pendingNavDestination = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationNavigation(intent)
    }

    companion object {
        var pendingNavDestination: String? = null

        private fun handleNotificationNavigation(intent: Intent?) {
            if (intent == null) return
            val targetRoute = intent.getStringExtra(NavExtras.EXTRA_TARGET_ROUTE)
            if (targetRoute != null) {
                val code = intent.getStringExtra(NavExtras.EXTRA_TARGET_CODE)
                pendingNavDestination = when (targetRoute) {
                    "detail" -> {
                        if (code != null) Screen.Detail.createRoute(code) else null
                    }
                    Screen.Alerts.route -> Screen.Alerts.route
                    Screen.Market.route -> Screen.Market.route
                    Screen.AiChat.route -> Screen.AiChat.route
                    Screen.Settings.route -> Screen.Settings.route
                    else -> null
                }
            }
        }
    }
}
