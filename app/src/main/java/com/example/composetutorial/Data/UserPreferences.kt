package com.example.composetutorial.Data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun getUsername(): String {
        return sharedPreferences.getString("username", "Lexi") ?: "Lexi"
    }

    fun setUsername(username: String) {
        sharedPreferences.edit().putString("username", username).apply()
    }
}