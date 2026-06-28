package com.example.vidgrab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vidgrab.ui.MainScreen
import com.example.vidgrab.ui.theme.VidGrabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VidGrabTheme {
                MainScreen()
            }
        }
    }
}
