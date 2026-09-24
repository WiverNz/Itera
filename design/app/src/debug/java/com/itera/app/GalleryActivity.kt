package com.itera.app
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.itera.app.ui.preview.ComponentGallery
import com.itera.app.ui.preview.ComponentSample
import com.itera.app.ui.preview.TokenGallery
import com.itera.app.ui.theme.IteraTheme
import com.itera.app.ui.PrototypeShellGallery

/** Synthetic fixtures only; this activity is absent from release builds. */
class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra("page") ?: "IteraButton"
        val dark = intent.getBooleanExtra("dark", false)
        enableEdgeToEdge(
            statusBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        val fontScale = intent.getFloatExtra("fontScale", 1f)
        setContent {
            IteraTheme(dark) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    when (page) {
                        "Tokens" -> TokenGallery()
                        "Shell" -> PrototypeShellGallery()
                        else -> ComponentGallery(ComponentSample.valueOf(page))
                    }
                }
            }
        }
    }
}
