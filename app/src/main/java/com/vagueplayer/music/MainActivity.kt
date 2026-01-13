package com.vagueplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vagueplayer.music.ui.theme.VaguePlayerTheme
import com.vagueplayer.music.ui.screens.MainScreen

// Permission Imports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable Edge-to-Edge (Immersive UI)
        enableEdgeToEdge()

        setContent {
            VaguePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    PermissionRequestWrapper {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequestWrapper(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(checkPermissions(context))
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { 
            hasPermissions = checkPermissions(context)
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            launcher.launch(permissions)
        }
    }

    if (hasPermissions) {
        content()
    } else {
        // Simple Placeholder
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text("需要权限以播放音乐")
        }
    }
}

fun checkPermissions(context: android.content.Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}