package com.example.voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val THEME_KEY = intPreferencesKey("theme_key")
        val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode_key")
    }

    val themeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: 0 // Default theme 0
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE_KEY] ?: false
    }

    suspend fun setTheme(themeIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeIndex
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE_KEY] = isDark
        }
    }
}
