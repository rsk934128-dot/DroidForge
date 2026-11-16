package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.BookmarkedRepoEntity
import com.example.data.network.GitHubReleaseAssetDto
import com.example.data.network.GitHubWorkflowRunDto
import com.example.ui.components.BrowserHelper
import com.example.ui.components.ClockBadge
import com.example.ui.components.FormatUtils
import com.example.ui.components.MonoBadge
import com.example.ui.components.RunStatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.PipelineUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
  strings: AppStrings,
  pipelineState: PipelineUiState,
  bookmarkedRepos: List<BookmarkedRepoEntity>,
  hasPat: Boolean,
  onSelectSavedRepo: (BookmarkedRepoEntity) -> Unit,
  onRefresh: () -> Unit,
  onOpenReposTab: () -> Unit,
  onOpenYamlTab: () -> Unit,
  onInspectRun: (GitHubWorkflowRunDto) -> Unit,
  onTriggerWorkflow: (workflowId: Long, branch: String, note: String?, onDone: () -> Unit) -> Unit,
  onDownloadAssetToUri: (url: String, uri: android.net.Uri, fileName: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showDispatchSheet by rememberSaveable { mutableStateOf(false) }
  var pendingSaveAsset by remember { mutableStateOf<GitHubReleaseAssetDto?>(null) }

  val saveAssetLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
      val asset = pendingSaveAsset
      if (uri != null && asset != null) {
        onDownloadAssetToUri(asset.browserDownloadUrl, uri, asset.name)
      }
      pendingSaveAsset = null
    }

  if (pipelineState.repoFullName.isBlank()) {
    Box(
      modifier = modifier.fillMaxSize().padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        Column(
          modifier = Modifier.padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.RocketLaunch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(text = strings.selectRepoFirst, style = MaterialTheme.typography.titleLarge)
          Text(
            text = strings.emptyExploreBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = onOpenReposTab,
            modifier = Modifier.testTag("go_to_repos_button"),
          ) {
            Text(strings.goToRepos)
          }
        }
      }
    }
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (bookmarkedRepos.isNotEmpty()) {
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(bookmarkedRepos, key = { it.fullName }) { repo ->
            FilterChip(
              selected = repo.fullName == pipelineState.repoFullName,
              onClick = { onSelectSavedRepo(repo) },
              label = { Text(repo.name, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("switch_repo_${repo.name}"),
            )
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = strings.activeRepoLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = pipelineState.repoFullName,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { showDispatchSheet = true },
              enabled = pipelineState.workflows.isNotEmpty(),
              modifier = Modifier.weight(1f).testTag("trigger_build_button"),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.triggerBuild)
            }
            OutlinedButton(
              onClick = onRefresh,
              modifier = Modifier.testTag("refresh_pipeline_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = strings.refresh, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.refresh)
            }
          }
          if (pipelineState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }
        }
      }
    }

    if (pipelineState.errorMessage != null) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text(
            text = pipelineState.errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }

    item {
      Text(
        text = "${strings.workflowsTitle} (${pipelineState.workflows.size})",
        style = MaterialTheme.typography.titleMedium,
      )
    }

    if (pipelineState.workflows.isEmpty() && !pipelineState.isLoading) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(text = strings.noWorkflowsFound, style = MaterialTheme.typography.titleSmall)
            Text(
              text = strings.noWorkflowsHint,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
              onClick = onOpenYamlTab,
              modifier = Modifier.testTag("open_yaml_from_builds"),
            ) {
              Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.openYamlStudio)
            }
          }
        }
      }
    } else {
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(pipelineState.workflows, key = { it.id }) { wf ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.clickable { showDispatchSheet = true },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Terminal,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(text = wf.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = wf.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    val apkReleases =
      pipelineState.releases.filter { release ->
        release.assets.any { it.name.endsWith(".apk", ignoreCase = true) }
      }
    if (apkReleases.isNotEmpty()) {
      item {
        Text(
          text = strings.releasesTitle,
          style = MaterialTheme.typography.titleMedium,
        )
      }
      items(apkReleases, key = { it.id }) { release ->
        val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Android,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                  style = MaterialTheme.typography.titleSmall,
                )
              }
              MonoBadge(text = release.tagName)
            }
            apkAssets.forEach { asset ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = asset.name,
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${FormatUtils.formatBytes(asset.size)} · ${asset.downloadCount} downloads",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  FilledTonalButton(
                    onClick = {
                      pendingSaveAsset = asset
                      saveAssetLauncher.launch(asset.name)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_release_apk_${asset.id}"),
                  ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.downloadApk, style = MaterialTheme.typography.labelSmall)
                  }
                  OutlinedButton(
                    onClick = { BrowserHelper.openUrl(context, asset.browserDownloadUrl) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_release_apk_${asset.id}"),
                  ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = strings.openReleaseLink, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "${strings.recentRunsTitle} (${pipelineState.runs.size})",
        style = MaterialTheme.typography.titleMedium,
      )
    }

    if (pipelineState.runs.isEmpty() && !pipelineState.isLoading) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Build,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(32.dp),
            )
            Text(strings.noRunsYet, style = MaterialTheme.typography.titleSmall)
            Text(
              strings.noRunsHint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(pipelineState.runs, key = { it.id }) { run ->
        WorkflowRunCard(
          run = run,
          strings = strings,
          onClick = { onInspectRun(run) },
        )
      }
    }
  }

  if (showDispatchSheet) {
    TriggerBuildSheet(
      strings = strings,
      pipelineState = pipelineState,
      hasPat = hasPat,
      onDismiss = { showDispatchSheet = false },
      onDispatch = { wfId, branch, note ->
        onTriggerWorkflow(wfId, branch, note) { showDispatchSheet = false }
      },
    )
  }
}

@Composable
private fun WorkflowRunCard(
  run: GitHubWorkflowRunDto,
  strings: AppStrings,
  onClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("workflow_run_card_${run.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RunStatusPill(status = run.status, conclusion = run.conclusion, strings = strings)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ClockBadge(isoTimestamp = run.updatedAt)
          MonoBadge(text = "#${run.runNumber}")
        }
      }

      Text(
        text = run.displayTitle ?: run.name ?: "Build #${run.runNumber}",
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MonoBadge(text = run.headBranch ?: "main", icon = Icons.Rounded.AccountTree)
          MonoBadge(text = run.headSha.take(7), icon = Icons.Rounded.Commit)
        }
        run.actor?.let { actor ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            AsyncImage(
              model = actor.avatarUrl,
              contentDescription = actor.login,
              modifier = Modifier.size(22.dp).clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
            Text(
              text = actor.login,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerBuildSheet(
  strings: AppStrings,
  pipelineState: PipelineUiState,
  hasPat: Boolean,
  onDismiss: () -> Unit,
  onDispatch: (workflowId: Long, branch: String, note: String?) -> Unit,
) {
  var selectedWorkflowId by remember {
    mutableStateOf(pipelineState.workflows.firstOrNull()?.id ?: 0L)
  }
  var selectedBranch by remember {
    mutableStateOf(pipelineState.branches.firstOrNull()?.name ?: "main")
  }
  var buildNote by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(text = strings.dispatchSheetTitle, style = MaterialTheme.typography.titleLarge)

      if (!hasPat) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = strings.needsPatHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Text(text = strings.selectWorkflow, style = MaterialTheme.typography.labelLarge)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pipelineState.workflows, key = { it.id }) { wf ->
          FilterChip(
            selected = wf.id == selectedWorkflowId,
            onClick = { selectedWorkflowId = wf.id },
            label = { Text(wf.name) },
          )
        }
      }

      Text(text = strings.selectBranch, style = MaterialTheme.typography.labelLarge)
      if (pipelineState.branches.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(pipelineState.branches, key = { it.name }) { branch ->
            FilterChip(
              selected = branch.name == selectedBranch,
              onClick = { selectedBranch = branch.name },
              label = { Text(branch.name, style = MaterialTheme.typography.labelMedium) },
            )
          }
        }
      } else {
        OutlinedTextField(
          value = selectedBranch,
          onValueChange = { selectedBranch = it },
          label = { Text(strings.selectBranch) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      OutlinedTextField(
        value = buildNote,
        onValueChange = { buildNote = it },
        label = { Text(strings.buildVariantInput) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("dispatch_note_input"),
      )

      Button(
        onClick = { onDispatch(selectedWorkflowId, selectedBranch, buildNote) },
        enabled = !pipelineState.isDispatching && selectedWorkflowId != 0L,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_dispatch_button"),
      ) {
        if (pipelineState.isDispatching) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(8.dp))
        }
        Text(strings.dispatchNow)
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}
