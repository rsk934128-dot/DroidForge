package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkedRepoEntity
import com.example.data.network.GitHubRepoDto
import com.example.data.network.GitHubUserDto
import com.example.data.network.RateLimitCoreDto
import com.example.data.prefs.UserPrefs
import com.example.data.repository.GitHubLookupTarget
import com.example.data.repository.GitHubRepository
import com.example.domain.GradleBuildTask
import com.example.domain.PortalAccent
import com.example.domain.VercelPortalConfig
import com.example.domain.VercelPortalGenerator
import com.example.domain.WorkflowConfig
import com.example.domain.WorkflowTriggerMode
import com.example.domain.WorkflowYamlGenerator
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExploreReposState {
  data object Idle : ExploreReposState
  data object Loading : ExploreReposState
  data class Success(val repos: List<GitHubRepoDto>, val queryLabel: String) : ExploreReposState
  data class Error(val message: String) : ExploreReposState
}

data class SettingsVerificationState(
  val isVerifying: Boolean = false,
  val isOAuthInProgress: Boolean = false,
  val isOAuthConfigured: Boolean = false,
  val verifiedUser: GitHubUserDto? = null,
  val rateLimit: RateLimitCoreDto? = null,
  val errorMessage: String? = null,
)

data class YamlStudioUiState(
  val config: WorkflowConfig = WorkflowConfig(),
  val generatedYaml: String = WorkflowYamlGenerator.generate(WorkflowConfig()),
  val isPushing: Boolean = false,
)

data class VercelPortalUiState(
  val config: VercelPortalConfig = VercelPortalConfig(),
  val generatedHtml: String = VercelPortalGenerator.generateIndexHtml(VercelPortalConfig()),
  val generatedVercelJson: String = VercelPortalGenerator.generateVercelJson(),
  val isPushing: Boolean = false,
)

class MainViewModel(private val repo: GitHubRepository) : ViewModel() {

  val prefs: StateFlow<UserPrefs> =
    repo.prefsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPrefs(),
    )

  val bookmarkedRepos: StateFlow<List<BookmarkedRepoEntity>> =
    repo.bookmarkedReposFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  private val _exploreState = MutableStateFlow<ExploreReposState>(ExploreReposState.Idle)
  val exploreState: StateFlow<ExploreReposState> = _exploreState.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _settingsState =
    MutableStateFlow(SettingsVerificationState(isOAuthConfigured = repo.isOAuthConfigured()))
  val settingsState: StateFlow<SettingsVerificationState> = _settingsState.asStateFlow()

  private val _yamlState = MutableStateFlow(YamlStudioUiState())
  val yamlState: StateFlow<YamlStudioUiState> = _yamlState.asStateFlow()

  private val _vercelState = MutableStateFlow(VercelPortalUiState())
  val vercelState: StateFlow<VercelPortalUiState> = _vercelState.asStateFlow()

  private val _events = MutableSharedFlow<String>()
  val events = _events.asSharedFlow()

  init {
    viewModelScope.launch {
      val initialPrefs = repo.prefsFlow.first()
      repo.updateTokenCache(initialPrefs.githubPat)
      _searchQuery.value = initialPrefs.defaultHandle
      if (initialPrefs.activeRepoFullName.isNotBlank()) {
        val repoName = initialPrefs.activeRepoFullName.substringAfter('/', initialPrefs.activeRepoFullName)
        updateVercelConfig(
          _vercelState.value.config.copy(
            appName = repoName,
            repoFullName = initialPrefs.activeRepoFullName,
          )
        )
      }
      if (initialPrefs.defaultHandle.isNotBlank()) {
        lookupGitHub(initialPrefs.defaultHandle)
      }
      refreshRateLimit()
    }
  }

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun lookupGitHub(rawInput: String = _searchQuery.value) {
    val target = repo.parseTarget(rawInput)
    if (target == null) {
      _exploreState.value = ExploreReposState.Error("Enter a GitHub username or owner/repo URL.")
      return
    }
    _searchQuery.value = rawInput.trim()
    viewModelScope.launch {
      _exploreState.value = ExploreReposState.Loading
      when (target) {
        is GitHubLookupTarget.UserRepos -> {
          repo
            .fetchUserRepos(target.username)
            .onSuccess { list ->
              _exploreState.value = ExploreReposState.Success(list, "@${target.username}")
              refreshRateLimit()
            }
            .onFailure { err ->
              _exploreState.value =
                ExploreReposState.Error(err.message ?: "Unable to fetch repositories.")
            }
        }
        is GitHubLookupTarget.SingleRepo -> {
          repo
            .fetchRepo(target.owner, target.repo)
            .onSuccess { single ->
              _exploreState.value =
                ExploreReposState.Success(listOf(single), "${target.owner}/${target.repo}")
              refreshRateLimit()
            }
            .onFailure { err ->
              _exploreState.value =
                ExploreReposState.Error(err.message ?: "Repository not found.")
            }
        }
      }
    }
  }

  fun loadMyRepos() {
    viewModelScope.launch {
      _exploreState.value = ExploreReposState.Loading
      repo
        .fetchMyRepos()
        .onSuccess { list ->
          _exploreState.value = ExploreReposState.Success(list, "My Repos")
          refreshRateLimit()
        }
        .onFailure { err ->
          _exploreState.value =
            ExploreReposState.Error(err.message ?: "Unable to fetch your repositories.")
        }
    }
  }

  fun toggleBookmark(gitHubRepo: GitHubRepoDto, addedMsg: String, removedMsg: String) {
    viewModelScope.launch {
      val added = repo.toggleBookmark(gitHubRepo)
      _events.emit(if (added) addedMsg else removedMsg)
    }
  }

  fun removeBookmark(fullName: String, removedMsg: String) {
    viewModelScope.launch {
      repo.removeBookmark(fullName)
      _events.emit(removedMsg)
    }
  }

  fun selectActiveRepo(gitHubRepo: GitHubRepoDto) {
    viewModelScope.launch {
      repo.bookmarkRepo(gitHubRepo)
      repo.prefsRepo.setActiveRepo(gitHubRepo.fullName)
      updateYamlConfig(_yamlState.value.config.copy(branch = gitHubRepo.defaultBranch))
      updateVercelConfig(
        _vercelState.value.config.copy(
          appName = gitHubRepo.name,
          repoFullName = gitHubRepo.fullName,
          branch = gitHubRepo.defaultBranch,
        )
      )
    }
  }

  fun selectActiveRepoByName(fullName: String, defaultBranch: String = "main") {
    viewModelScope.launch {
      repo.prefsRepo.setActiveRepo(fullName)
      updateYamlConfig(_yamlState.value.config.copy(branch = defaultBranch))
      val repoName = fullName.substringAfter('/', fullName)
      updateVercelConfig(
        _vercelState.value.config.copy(
          appName = repoName,
          repoFullName = fullName,
          branch = defaultBranch,
        )
      )
    }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { repo.prefsRepo.setLanguage(language) }
  }

  fun setThemeMode(themeMode: ThemeMode) {
    viewModelScope.launch { repo.prefsRepo.setThemeMode(themeMode) }
  }

  fun saveGitHubSettings(pat: String, defaultHandle: String, savedMessage: String) {
    viewModelScope.launch {
      repo.updateTokenCache(pat)
      repo.prefsRepo.saveGitHubCredentials(pat, defaultHandle)
      _events.emit(savedMessage)
      verifyConnection()
    }
  }

  fun startGitHubOAuth(onOpenBrowser: (Uri) -> Unit) {
    repo
      .createOAuthAuthorizeUri()
      .onSuccess { authUri ->
        _settingsState.update { it.copy(isOAuthInProgress = true, errorMessage = null) }
        onOpenBrowser(authUri)
      }
      .onFailure { err ->
        val msg = err.message ?: "Unable to start GitHub OAuth2 flow."
        _settingsState.update { it.copy(isOAuthInProgress = false, errorMessage = msg) }
        viewModelScope.launch { _events.emit(msg) }
      }
  }

  fun handleOAuthCallback(callbackUri: Uri, successMessage: String) {
    if (!repo.isOAuthCallbackUri(callbackUri)) return
    viewModelScope.launch {
      _settingsState.update { it.copy(isOAuthInProgress = true, errorMessage = null) }
      repo
        .completeOAuthCallback(callbackUri)
        .onSuccess {
          _settingsState.update { it.copy(isOAuthInProgress = false) }
          _events.emit(successMessage)
          verifyConnection()
          loadMyRepos()
        }
        .onFailure { err ->
          val msg = err.message ?: "GitHub OAuth2 login failed."
          _settingsState.update { it.copy(isOAuthInProgress = false, errorMessage = msg) }
          _events.emit(msg)
        }
    }
  }

  fun clearPat(clearedMessage: String) {
    viewModelScope.launch {
      repo.updateTokenCache("")
      repo.prefsRepo.clearPat()
      _settingsState.update { it.copy(verifiedUser = null, errorMessage = null) }
      _events.emit(clearedMessage)
      refreshRateLimit()
    }
  }

  fun verifyConnection() {
    viewModelScope.launch {
      _settingsState.update { it.copy(isVerifying = true, errorMessage = null) }
      val pat = prefs.value.githubPat
      if (pat.isNotBlank()) {
        repo
          .fetchAuthenticatedUser()
          .onSuccess { user ->
            _settingsState.update { it.copy(isVerifying = false, verifiedUser = user) }
          }
          .onFailure { err ->
            _settingsState.update {
              it.copy(isVerifying = false, verifiedUser = null, errorMessage = err.message)
            }
          }
      } else {
        _settingsState.update { it.copy(isVerifying = false, verifiedUser = null) }
      }
      refreshRateLimit()
    }
  }

  fun refreshRateLimit() {
    viewModelScope.launch {
      repo.fetchRateLimit().onSuccess { limit ->
        _settingsState.update { it.copy(rateLimit = limit) }
      }
    }
  }

  fun updateGradleTask(task: GradleBuildTask) {
    updateYamlConfig(_yamlState.value.config.copy(task = task))
  }

  fun updateJdkVersion(jdk: String) {
    updateYamlConfig(_yamlState.value.config.copy(jdkVersion = jdk))
  }

  fun updateTargetBranch(branch: String) {
    updateYamlConfig(_yamlState.value.config.copy(branch = branch))
  }

  fun updateTriggerMode(mode: WorkflowTriggerMode) {
    updateYamlConfig(_yamlState.value.config.copy(triggerMode = mode))
  }

  fun updatePublishRelease(enabled: Boolean) {
    updateYamlConfig(_yamlState.value.config.copy(publishRelease = enabled))
  }

  private fun updateYamlConfig(newConfig: WorkflowConfig) {
    _yamlState.update {
      it.copy(config = newConfig, generatedYaml = WorkflowYamlGenerator.generate(newConfig))
    }
  }

  fun pushYamlToActiveRepo(successMessage: String) {
    val active = prefs.value.activeRepoFullName
    val parts = active.split('/')
    if (parts.size != 2) {
      viewModelScope.launch { _events.emit("Select a repository first in the Repos tab.") }
      return
    }
    viewModelScope.launch {
      _yamlState.update { it.copy(isPushing = true) }
      val branch = _yamlState.value.config.branch.trim().ifEmpty { "main" }
      repo
        .pushWorkflowYaml(parts[0], parts[1], branch, _yamlState.value.generatedYaml)
        .onSuccess {
          _yamlState.update { it.copy(isPushing = false) }
          _events.emit(successMessage)
        }
        .onFailure { err ->
          _yamlState.update { it.copy(isPushing = false) }
          _events.emit(err.message ?: "Failed to push workflow YAML.")
        }
    }
  }

  fun updatePortalAppName(appName: String) {
    updateVercelConfig(_vercelState.value.config.copy(appName = appName))
  }

  fun updatePortalTagline(tagline: String) {
    updateVercelConfig(_vercelState.value.config.copy(tagline = tagline))
  }

  fun updatePortalAccent(accent: PortalAccent) {
    updateVercelConfig(_vercelState.value.config.copy(accent = accent))
  }

  fun updatePortalSubfolder(useSubfolder: Boolean) {
    updateVercelConfig(_vercelState.value.config.copy(useSubfolder = useSubfolder))
  }

  private fun updateVercelConfig(newConfig: VercelPortalConfig) {
    _vercelState.update {
      it.copy(
        config = newConfig,
        generatedHtml = VercelPortalGenerator.generateIndexHtml(newConfig),
      )
    }
  }

  fun pushVercelPortalToActiveRepo(successMessage: String) {
    val active = prefs.value.activeRepoFullName
    val parts = active.split('/')
    if (parts.size != 2) {
      viewModelScope.launch { _events.emit("Select a repository first in the Repos tab.") }
      return
    }
    viewModelScope.launch {
      _vercelState.update { it.copy(isPushing = true) }
      val cfg = _vercelState.value.config
      val branch = cfg.branch.trim().ifEmpty { _yamlState.value.config.branch.ifEmpty { "main" } }
      val prefix = if (cfg.useSubfolder) "vercel-site/" else ""
      val htmlRes =
        repo.pushTextFile(
          owner = parts[0],
          repo = parts[1],
          branch = branch,
          filePath = "${prefix}index.html",
          content = _vercelState.value.generatedHtml,
          commitMessage = "chore: add Vercel APK portal index.html via DroidForge",
        )
      if (htmlRes.isFailure) {
        _vercelState.update { it.copy(isPushing = false) }
        _events.emit(htmlRes.exceptionOrNull()?.message ?: "Failed to push index.html.")
        return@launch
      }
      val jsonRes =
        repo.pushTextFile(
          owner = parts[0],
          repo = parts[1],
          branch = branch,
          filePath = "${prefix}vercel.json",
          content = _vercelState.value.generatedVercelJson,
          commitMessage = "chore: add vercel.json config via DroidForge",
        )
      _vercelState.update { it.copy(isPushing = false) }
      jsonRes
        .onSuccess { _events.emit(successMessage) }
        .onFailure { err -> _events.emit(err.message ?: "Failed to push vercel.json.") }
    }
  }

  class Factory(private val repo: GitHubRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
  }
}
