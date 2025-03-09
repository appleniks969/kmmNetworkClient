package com.kmm.networkclient

import android.content.Context

/**
 * Singleton to provide Android Context throughout the application.
 * This is required for Chucker to function properly.
 */
object AndroidContextProvider {
    private var applicationContext: Context? = null
    
    /**
     * Initialize the provider with the application context.
     * This should be called once when the application starts.
     * 
     * @param context The application context
     */
    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }
    
    /**
     * Get the application context.
     * 
     * @return The application context or null if not initialized
     */
    fun getApplicationContext(): Context? {
        return applicationContext
    }
    
    /**
     * Check if the context provider is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return applicationContext != null
    }
} 