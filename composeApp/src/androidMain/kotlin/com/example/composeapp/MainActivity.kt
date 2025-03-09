package com.example.composeapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var networkClient: SimpleNetworkClient
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the network client with context for Chucker
        networkClient = SimpleNetworkClient(
            baseUrl = "https://jsonplaceholder.typicode.com",
            context = this
        )
        
        // Create a simple TextView to display results
        val textView = TextView(this).apply {
            text = "Loading data..."
            textSize = 20f
            setPadding(30, 30, 30, 30)
        }
        
        setContentView(textView)
        
        // Create an exception handler for coroutines
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("MainActivity", "Error in coroutine", throwable)
            runOnUiThread {
                textView.text = "Error: ${throwable.message}"
            }
        }
        
        // Use our SimpleNetworkClient to fetch data
        mainScope.launch(exceptionHandler) {
            try {
                val result = networkClient.get("posts/1")
                textView.text = "Successfully fetched data:\n\n$result"
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching data", e)
                textView.text = "Error: ${e.message}"
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing coroutines when the activity is destroyed
        mainScope.launch {
            networkClient.close()
        }
    }
} 