package com.doer.shijiben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.navigation.ShijibenNavHost
import com.doer.shijiben.ui.theme.ShijibenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ShijibenApplication
        setContent {
            ShijibenTheme {
                val vm: EventViewModel = viewModel(factory = EventViewModel.factory(app.repository))
                val navController = rememberNavController()
                ShijibenNavHost(navController = navController, viewModel = vm)
            }
        }
    }
}
