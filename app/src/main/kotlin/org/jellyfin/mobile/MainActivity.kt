package org.jellyfin.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jellyfin.mobile.presentation.main.MainScaffold
import org.jellyfin.mobile.ui.theme.JellyfinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            JellyfinTheme {
                MainScaffold()
            }
        }
    }
}
