package com.wivernz.itera
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.wivernz.itera.core.designsystem.preview.ComponentGallery
import com.wivernz.itera.core.designsystem.preview.ComponentSample
import com.wivernz.itera.core.designsystem.preview.TokenGallery
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.navigation.AppNavHost

/** Synthetic fixtures only; this activity is absent from release builds. */
class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val page = intent.getStringExtra("page") ?: "IteraButton"
        val dark = intent.getBooleanExtra("dark", false)
        val fontScale = intent.getFloatExtra("fontScale", 1f)
        setContent {
            IteraTheme(dark) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    when (page) {
                        "Tokens" -> TokenGallery()
                        "Shell" -> AppNavHost(onboardingCompleted = true)
                        else -> ComponentGallery(ComponentSample.valueOf(page))
                    }
                }
            }
        }
    }
}
