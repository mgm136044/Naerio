package app.naerio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.naerio.app.AppViewModel
import app.naerio.app.Screen
import app.naerio.ui.NaerilgotApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val s = vm.ui.collectAsStateWithLifecycle().value
            BackHandler(enabled = s.screen != Screen.DESTINATION) { vm.back() }
            NaerilgotApp(vm)
        }
    }
}
