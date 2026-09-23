package com.itera.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import com.itera.app.data.AppViewModel
import com.itera.app.ui.IteraApp
import com.itera.app.ui.theme.IteraTheme
import com.itera.app.ui.theme.isDark

/**
 * AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales
 * can switch the per-app language on every Android version.
 */
class MainActivity : AppCompatActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = vm.themeMode.isDark()
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            IteraTheme(dark = dark) {
                IteraApp(vm)
            }
        }
    }
}
