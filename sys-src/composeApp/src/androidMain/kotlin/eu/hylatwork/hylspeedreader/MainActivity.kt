package eu.hylatwork.hylspeedreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import eu.hylatwork.hylspeedreader.data.Platform

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Platform.init(this)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
