package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.network.GitHubArtifactDto
import com.example.data.network.GitHubJobDto
import com.example.ui.components.BrowserHelper
import com.example.ui.components.FormatUtils
import com.example.ui.components.MonoBadge
import com.example.ui.components.RunStatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.viewmodel.RunDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
  strings: AppStrings,
  repoFullName: String,
  state: RunDetailUiState,
  onBack: () -> Unit,
  onRerun: () -> Unit,
  onCancel: () -> Unit,
  onDownloadArtifact: (url: String, uri: Uri, fileName: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val expandedJobs = remember { mutableStateMapOf<Long, Boolean>() }
  var pendingArtifact by remember { mutableStateOf<GitHubArtifactDto?>(null) }

  val saveZipLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
      val artifact = pendingArtifact
      if (uri != null && artifact != null) {
        onDownloadArtifact(artifact.archiveDownloadUrl, uri, "${artifact.name}.zip")
      }
      pendingArtifact = null
    }

  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
      title = {
        Text(
          text = state.run?.let { "#${it.runNumber} · ${strings.runDetailsTitle}" } ?: strings.runDetailsTitle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      navigationIcon = {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("run_detail_back"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
      },
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      state.run?.let { run ->
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RunStatusPill(status = run.status, conclusion = run.conclusion, strings = strings)
                Text(
                  text = repoFullName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = run.displayTitle ?: run.name ?: "Run #${run.runNumber}",
                style = MaterialTheme.typography.titleMedium,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoBadge(text = run.headBranch ?: "main", icon = Icons.Rounded.AccountTree)
                MonoBadge(text = run.headSha.take(7), icon = Icons.Rounded.Commit)
                MonoBadge(text = run.event)
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                val active = run.status == "queued" || run.status == "in_progress"
                if (active) {
                  OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).testTag("cancel_run_button"),
                  ) {
                    Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.cancelRun)
                  }
                } else {
                  FilledTonalButton(
                    onClick = onRerun,
                    modifier = Modifier.weight(1f).testTag("rerun_workflow_button"),
                  ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.rerunJobs)
                  }
                }
                OutlinedButton(
                  onClick = { BrowserHelper.openUrl(context, run.htmlUrl) },
                  modifier = Modifier.weight(1f).testTag("open_run_github_button"),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(strings.openInGitHub)
                }
              }

              if (state.isLoading || state.isDownloading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }
            }
          }
        }
      }

      if (state.errorMessage != null) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(
              text = state.errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      item {
        Text(
          text = "${strings.artifactsAndApks} (${state.artifacts.size})",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (state.artifacts.isEmpty() && !state.isLoading) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
          ) {
            Text(
              text = strings.noArtifactsYet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
          }
        }
      } else {
        items(state.artifacts, key = { it.id }) { artifact ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = artifact.name,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = FormatUtils.formatBytes(artifact.sizeInBytes),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              FilledTonalButton(
                onClick = {
                  pendingArtifact = artifact
                  saveZipLauncher.launch("${artifact.name}.zip")
                },
                enabled = !artifact.expired && !state.isDownloading,
                modifier = Modifier.testTag("download_artifact_${artifact.id}"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.saveFile)
              }
            }
          }
        }
      }

      item {
        Text(
          text = "${strings.jobsAndSteps} (${state.jobs.size})",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      items(state.jobs, key = { it.id }) { job ->
        val isExpanded = expandedJobs.getOrPut(job.id) { true }
        JobStepsCard(
          job = job,
          strings = strings,
          expanded = isExpanded,
          onToggle = { expandedJobs[job.id] = !isExpanded },
        )
      }
    }
  }
}

@Composable
private fun JobStepsCard(
  job: GitHubJobDto,
  strings: AppStrings,
  expanded: Boolean,
  onToggle: () -> Unit,
) {
  val palette = LocalStatusPalette.current
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = job.name, style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RunStatusPill(status = job.status, conclusion = job.conclusion, strings = strings)
            job.runnerName?.takeIf { it.isNotBlank() }?.let { runner ->
              MonoBadge(text = runner)
            }
          }
        }
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = null,
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          job.steps.forEach { step ->
            val tint =
              when (step.conclusion) {
                "success" -> palette.success
                "failure" -> palette.failed
                null -> if (step.status == "in_progress") palette.running else palette.queued
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            val icon =
              when (step.conclusion) {
                "success" -> Icons.Rounded.CheckCircle
                "failure" -> Icons.Rounded.Error
                else -> Icons.Rounded.Schedule
              }
            val duration = FormatUtils.formatStepDuration(step.startedAt, step.completedAt)
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "${step.number}. ${step.name}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              if (duration.isNotEmpty()) {
                Text(
                  text = duration,
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
}
