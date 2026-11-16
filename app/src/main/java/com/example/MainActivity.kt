package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DroidForgeApp
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PipelineViewModel

class MainActivity : ComponentActivity() {
  private val pendingOAuthCallback = mutableStateOf<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    captureOAuthIntent(intent)
    val container = (application as DroidForgeApplication).container
    setContent {
      val mainViewModel: MainViewModel =
        viewModel(factory = MainViewModel.Factory(container.gitHubRepository))
      val pipelineViewModel: PipelineViewModel =
        viewModel(factory = PipelineViewModel.Factory(container.gitHubRepository))

      val callbackUri by pendingOAuthCallback
      LaunchedEffect(callbackUri) {
        callbackUri?.let { uri ->
          mainViewModel.handleOAuthCallback(uri, "Signed in to GitHub via OAuth2")
          pendingOAuthCallback.value = null
        }
      }

      DroidForgeApp(
        mainViewModel = mainViewModel,
        pipelineViewModel = pipelineViewModel,
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    captureOAuthIntent(intent)
  }

  private fun captureOAuthIntent(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme == "droidforge" && data.host == "oauth") {
      pendingOAuthCallback.value = data
    }
  }
}
