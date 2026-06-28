package us.smoltech.vidgrab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import us.smoltech.vidgrab.ui.MainScreen
import us.smoltech.vidgrab.ui.theme.VidGrabTheme

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
