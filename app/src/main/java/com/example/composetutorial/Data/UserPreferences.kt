package com.example.composetutorial.Data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(context: Context) {

    private val dataStore = context.userDataStore

    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val PROFILE_IMAGE_URI_KEY = stringPreferencesKey("profile_image_uri")
    }

    val usernameFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: "Lexi"
    }

    val profileImageUriFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PROFILE_IMAGE_URI_KEY] ?: ""
    }

    suspend fun setUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun setProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI_KEY] = uri
        }
    }
}
