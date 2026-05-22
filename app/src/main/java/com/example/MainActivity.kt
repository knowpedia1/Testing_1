package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.CodeEditorScreen
import com.example.ui.SignInScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ChatViewModel = viewModel(
          factory = ChatViewModelFactory(applicationContext)
        )
        
        val showSignIn by viewModel.showSignIn.collectAsState()
        val showCodeEditor by viewModel.showCodeEditor.collectAsState()

        when {
            showSignIn -> {
                SignInScreen(
                    onSignInSuccess = { viewModel.showSignIn.value = false }
                )
            }
            showCodeEditor -> {
                CodeEditorScreen(
                    onClose = { viewModel.showCodeEditor.value = false }
                )
            }
            else -> {
                ChatScreen(viewModel = viewModel)
            }
        }
      }
    }
  }
}

