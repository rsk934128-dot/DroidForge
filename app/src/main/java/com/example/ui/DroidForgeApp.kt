package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BrowserHelper
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.BuildsScreen
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.ReposScreen
import com.example.ui.screens.RunDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DroidForgeTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PipelineViewModel
import kotlinx.coroutines.flow.merge

const val ROUTE_REPOS = "repos"
const val ROUTE_BUILDS = "builds"
const val ROUTE_RUN_DETAIL = "run_detail"
const val ROUTE_YAML = "yaml"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidForgeApp(
  mainViewModel: MainViewModel,
  pipelineViewModel: PipelineViewModel,
) {
  val prefs by mainViewModel.prefs.collectAsStateWithLifecycle()
  val bookmarkedRepos by mainViewModel.bookmarkedRepos.collectAsStateWithLifecycle()
  val exploreState by mainViewModel.exploreState.collectAsStateWithLifecycle()
  val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
  val settingsState by mainViewModel.settingsState.collectAsStateWithLifecycle()
  val yamlState by mainViewModel.yamlState.collectAsStateWithLifecycle()
  val vercelState by mainViewModel.vercelState.collectAsStateWithLifecycle()
  val pipelineState by pipelineViewModel.pipelineState.collectAsStateWithLifecycle()
  val runDetailState by pipelineViewModel.runDetailState.collectAsStateWithLifecycle()

  val strings = remember(prefs.language) { stringsFor(prefs.language) }
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  LaunchedEffect(prefs.activeRepoFullName) {
    if (prefs.activeRepoFullName.isNotBlank() &&
      pipelineState.repoFullName != prefs.activeRepoFullName
    ) {
      pipelineViewModel.loadPipeline(prefs.activeRepoFullName)
    }
  }

  LaunchedEffect(Unit) {
    merge(mainViewModel.events, pipelineViewModel.events).collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val navItems =
    listOf(
      NavDestination(ROUTE_REPOS, strings.navRepos, Icons.Rounded.Folder, Icons.Outlined.Folder),
      NavDestination(ROUTE_BUILDS, strings.navBuilds, Icons.Rounded.RocketLaunch, Icons.Outlined.RocketLaunch),
      NavDestination(ROUTE_YAML, strings.navYaml, Icons.Rounded.Code, Icons.Outlined.Code),
      NavDestination(ROUTE_SETTINGS, strings.navSettings, Icons.Rounded.Settings, Icons.Outlined.Settings),
    )

  fun switchTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  DroidForgeTheme(themeMode = prefs.themeMode) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp
      val showChrome = currentRoute != ROUTE_RUN_DETAIL

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          if (showChrome) {
            TopAppBar(
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.background
                ),
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Terminal,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(6.dp).size(18.dp),
                    )
                  }
                  Text(text = strings.appTitle, style = MaterialTheme.typography.titleLarge)
                }
              },
              actions = {
                Surface(
                  onClick = {
                    val next =
                      if (prefs.language == AppLanguage.ENGLISH) AppLanguage.BENGALI
                      else AppLanguage.ENGLISH
                    mainViewModel.setLanguage(next)
                  },
                  shape = RoundedCornerShape(999.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  modifier = Modifier.padding(end = 12.dp).testTag("topbar_language_toggle"),
                ) {
                  Text(
                    text = if (prefs.language == AppLanguage.ENGLISH) "EN · বাং" else "বাং · EN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  )
                }
              },
            )
          }
        },
        bottomBar = {
          if (showChrome && !useNavRail) {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { switchTab(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("nav_${item.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showChrome && useNavRail) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
              Spacer(Modifier.size(12.dp))
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationRailItem(
                  selected = selected,
                  onClick = { switchTab(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("rail_${item.route}"),
                )
              }
            }
          }

          Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
          ) {
            NavHost(
              navController = navController,
              startDestination = ROUTE_REPOS,
              modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            ) {
              composable(ROUTE_REPOS) {
                ReposScreen(
                  strings = strings,
                  searchQuery = searchQuery,
                  exploreState = exploreState,
                  bookmarkedRepos = bookmarkedRepos,
                  activeRepoFullName = prefs.activeRepoFullName,
                  rateLimit = settingsState.rateLimit,
                  hasPat = prefs.githubPat.isNotBlank(),
                  onQueryChange = mainViewModel::onSearchQueryChange,
                  onSearchSubmit = mainViewModel::lookupGitHub,
                  onLoadMyRepos = mainViewModel::loadMyRepos,
                  onStartOAuth = {
                    mainViewModel.startGitHubOAuth { authUri ->
                      BrowserHelper.openUrl(context, authUri.toString())
                    }
                  },
                  onToggleBookmark = { repoDto ->
                    mainViewModel.toggleBookmark(
                      repoDto,
                      strings.bookmarkAdded,
                      strings.bookmarkRemoved,
                    )
                  },
                  onRemoveBookmark = { fullName ->
                    mainViewModel.removeBookmark(fullName, strings.bookmarkRemoved)
                  },
                  onSelectRepoForPipeline = { repoDto ->
                    mainViewModel.selectActiveRepo(repoDto)
                    pipelineViewModel.loadPipeline(repoDto.fullName)
                    switchTab(ROUTE_BUILDS)
                  },
                  onSelectSavedRepoForPipeline = { saved ->
                    mainViewModel.selectActiveRepoByName(saved.fullName, saved.defaultBranch)
                    pipelineViewModel.loadPipeline(saved.fullName)
                    switchTab(ROUTE_BUILDS)
                  },
                )
              }

              composable(ROUTE_BUILDS) {
                BuildsScreen(
                  strings = strings,
                  pipelineState = pipelineState,
                  bookmarkedRepos = bookmarkedRepos,
                  hasPat = prefs.githubPat.isNotBlank(),
                  onSelectSavedRepo = { saved ->
                    mainViewModel.selectActiveRepoByName(saved.fullName, saved.defaultBranch)
                    pipelineViewModel.loadPipeline(saved.fullName)
                  },
                  onRefresh = {
                    if (prefs.activeRepoFullName.isNotBlank()) {
                      pipelineViewModel.loadPipeline(prefs.activeRepoFullName)
                    }
                  },
                  onOpenReposTab = { switchTab(ROUTE_REPOS) },
                  onOpenYamlTab = { switchTab(ROUTE_YAML) },
                  onInspectRun = { run ->
                    pipelineViewModel.inspectRun(pipelineState.repoFullName, run)
                    navController.navigate(ROUTE_RUN_DETAIL)
                  },
                  onTriggerWorkflow = { workflowId, branch, note, onDone ->
                    pipelineViewModel.triggerWorkflow(
                      workflowId = workflowId,
                      branch = branch,
                      buildNote = note,
                      successMsg = strings.dispatchSuccess,
                      onDone = onDone,
                    )
                  },
                  onDownloadAssetToUri = pipelineViewModel::downloadToUri,
                )
              }

              composable(ROUTE_RUN_DETAIL) {
                RunDetailScreen(
                  strings = strings,
                  repoFullName = pipelineState.repoFullName,
                  state = runDetailState,
                  onBack = {
                    pipelineViewModel.clearRunDetail()
                    navController.popBackStack()
                  },
                  onRerun = pipelineViewModel::rerunCurrentRun,
                  onCancel = pipelineViewModel::cancelCurrentRun,
                  onDownloadArtifact = pipelineViewModel::downloadToUri,
                )
              }

              composable(ROUTE_YAML) {
                GeneratorScreen(
                  strings = strings,
                  language = prefs.language,
                  activeRepoFullName = prefs.activeRepoFullName,
                  state = yamlState,
                  vercelState = vercelState,
                  onSelectTask = mainViewModel::updateGradleTask,
                  onSelectJdk = mainViewModel::updateJdkVersion,
                  onBranchChange = mainViewModel::updateTargetBranch,
                  onSelectTriggerMode = mainViewModel::updateTriggerMode,
                  onToggleAutoRelease = mainViewModel::updatePublishRelease,
                  onPushToRepo = { mainViewModel.pushYamlToActiveRepo(strings.pushSuccess) },
                  onPortalAppNameChange = mainViewModel::updatePortalAppName,
                  onPortalTaglineChange = mainViewModel::updatePortalTagline,
                  onPortalAccentSelect = mainViewModel::updatePortalAccent,
                  onPortalSubfolderSelect = mainViewModel::updatePortalSubfolder,
                  onPushVercelPortal = {
                    mainViewModel.pushVercelPortalToActiveRepo(strings.vercelPushSuccess)
                  },
                )
              }

              composable(ROUTE_SETTINGS) {
                SettingsScreen(
                  strings = strings,
                  prefs = prefs,
                  verificationState = settingsState,
                  onStartOAuth = {
                    mainViewModel.startGitHubOAuth { authUri ->
                      BrowserHelper.openUrl(context, authUri.toString())
                    }
                  },
                  onSaveCredentials = { pat, handle ->
                    mainViewModel.saveGitHubSettings(pat, handle, "Settings saved")
                  },
                  onClearPat = { mainViewModel.clearPat("Token cleared") },
                  onVerifyConnection = mainViewModel::verifyConnection,
                  onSelectLanguage = mainViewModel::setLanguage,
                  onSelectTheme = mainViewModel::setThemeMode,
                )
              }
            }
          }
        }
      }
    }
  }
}
