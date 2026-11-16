package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gk_transport_prefs")

class LanguagePreferences(private val context: Context) {
  private val languageKey = stringPreferencesKey("app_language")

  val languageFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[languageKey] ?: "en" }

  suspend fun setLanguage(langCode: String) {
    context.dataStore.edit { prefs -> prefs[languageKey] = langCode }
  }
}
