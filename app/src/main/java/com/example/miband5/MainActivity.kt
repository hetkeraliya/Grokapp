package com.example.miband5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.miband5.ui.dashboard.DashboardScreen
import com.example.miband5.ui.theme.MiBand5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiBand5Theme {
                DashboardScreen()
            }
        }
    }
}
