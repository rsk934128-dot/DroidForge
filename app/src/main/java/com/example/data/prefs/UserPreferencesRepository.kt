package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "droidforge_prefs")

enum class AuthMethod {
  NONE,
  OAUTH,
  PAT,
}

data class UserPrefs(
  val githubPat: String = "",
  val authMethod: AuthMethod = AuthMethod.NONE,
  val oauthScope: String = "",
  val defaultHandle: String = "rsk934128-dot",
  val activeRepoFullName: String = "",
  val language: AppLanguage = AppLanguage.ENGLISH,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val GITHUB_PAT = stringPreferencesKey("github_pat")
    val AUTH_METHOD = stringPreferencesKey("auth_method")
    val OAUTH_SCOPE = stringPreferencesKey("oauth_scope")
    val DEFAULT_HANDLE = stringPreferencesKey("default_handle")
    val ACTIVE_REPO = stringPreferencesKey("active_repo")
    val LANGUAGE = stringPreferencesKey("app_language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
  }

  val prefsFlow: Flow<UserPrefs> =
    context.dataStore.data.map { prefs ->
      val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code
      val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
      val token = prefs[Keys.GITHUB_PAT] ?: ""
      val storedMethod = prefs[Keys.AUTH_METHOD]
      val authMethod =
        when {
          token.isBlank() -> AuthMethod.NONE
          storedMethod == AuthMethod.OAUTH.name -> AuthMethod.OAUTH
          else -> AuthMethod.PAT
        }
      UserPrefs(
        githubPat = token,
        authMethod = authMethod,
        oauthScope = prefs[Keys.OAUTH_SCOPE] ?: "",
        defaultHandle = prefs[Keys.DEFAULT_HANDLE] ?: "rsk934128-dot",
        activeRepoFullName = prefs[Keys.ACTIVE_REPO] ?: "",
        language = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.ENGLISH,
        themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM,
      )
    }

  suspend fun saveGitHubCredentials(pat: String, defaultHandle: String) {
    val cleanPat = pat.trim()
    context.dataStore.edit { prefs ->
      prefs[Keys.GITHUB_PAT] = cleanPat
      prefs[Keys.AUTH_METHOD] = if (cleanPat.isNotEmpty()) AuthMethod.PAT.name else AuthMethod.NONE.name
      prefs[Keys.OAUTH_SCOPE] = ""
      prefs[Keys.DEFAULT_HANDLE] = defaultHandle.trim().ifEmpty { "rsk934128-dot" }
    }
  }

  suspend fun saveOAuthToken(accessToken: String, scope: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.GITHUB_PAT] = accessToken.trim()
      prefs[Keys.AUTH_METHOD] = AuthMethod.OAUTH.name
      prefs[Keys.OAUTH_SCOPE] = scope.trim()
    }
  }

  suspend fun clearPat() {
    context.dataStore.edit { prefs ->
      prefs[Keys.GITHUB_PAT] = ""
      prefs[Keys.AUTH_METHOD] = AuthMethod.NONE.name
      prefs[Keys.OAUTH_SCOPE] = ""
    }
  }

  suspend fun setActiveRepo(fullName: String) {
    context.dataStore.edit { prefs -> prefs[Keys.ACTIVE_REPO] = fullName }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.code }
  }

  suspend fun setThemeMode(themeMode: ThemeMode) {
    context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = themeMode.name }
  }
}
