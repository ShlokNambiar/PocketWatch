package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.PocketWatchApp
import com.example.viewmodel.PocketWatchViewModel
import com.example.viewmodel.PocketWatchViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database holding dynamic personal ledgers
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = TransactionRepository(database.transactionDao())
    val factory = PocketWatchViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[PocketWatchViewModel::class.java]

    setContent {
      PocketWatchApp(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}
