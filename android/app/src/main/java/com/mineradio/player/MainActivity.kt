package com.mineradio.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineradio.player.ui.MainViewModel
import com.mineradio.player.ui.MainViewModelFactory
import com.mineradio.player.ui.screen.PlayerShell
import com.mineradio.player.ui.theme.MineradioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MineradioTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(MineradioApp.get()))
                val state by vm.state.collectAsState()
                PlayerShell(vm = vm, state = state)
            }
        }
    }
}
