package com.truong.fridgezero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "fridgezero_prefs"
)

class AppConfig(context: Context) {

  private val dataStore = context.applicationContext.appPrefsDataStore

  val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_ONBOARDING_DONE] ?: false
  }

  val expiryRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_EXPIRY_REMINDERS] ?: false
  }

  val notificationPromptShown: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_NOTIFICATION_PROMPTED] ?: false
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_ONBOARDING_DONE] = completed
    }
  }

  suspend fun setExpiryRemindersEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_EXPIRY_REMINDERS] = enabled
    }
  }

  suspend fun setNotificationPromptShown(shown: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_NOTIFICATION_PROMPTED] = shown
    }
  }

  companion object {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("has_completed_onboarding")
    private val KEY_EXPIRY_REMINDERS = booleanPreferencesKey("expiry_reminders_enabled")
    private val KEY_NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompt_shown")
  }
}
