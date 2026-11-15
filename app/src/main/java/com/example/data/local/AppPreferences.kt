package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EthioLanguage
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ethiogpt_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val VOICE_AUTO_SPEAK = booleanPreferencesKey("voice_auto_speak")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromCode(prefs[Keys.THEME_MODE])
    }

    val defaultLanguage: Flow<EthioLanguage> = context.dataStore.data.map { prefs ->
        EthioLanguage.fromCode(prefs[Keys.DEFAULT_LANGUAGE])
    }

    val voiceAutoSpeak: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.VOICE_AUTO_SPEAK] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.code
        }
    }

    suspend fun setDefaultLanguage(language: EthioLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_LANGUAGE] = language.code
        }
    }

    suspend fun setVoiceAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOICE_AUTO_SPEAK] = enabled
        }
    }
}
