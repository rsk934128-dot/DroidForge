package com.example

import android.app.Application
import com.example.data.ai.AIErrorService
import com.example.data.local.AppDatabase
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.GitHubRepository

class AppContainer(application: Application) {
  private val database by lazy { AppDatabase.getInstance(application) }
  private val prefsRepository by lazy { UserPreferencesRepository(application) }
  val gitHubRepository by lazy {
    GitHubRepository(
      appContext = application.applicationContext,
      prefsRepo = prefsRepository,
      db = database,
    )
  }
  val aiErrorService by lazy { AIErrorService() }
}

class DroidForgeApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
