package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiDiagnosisUiState
import com.example.data.network.GitHubArtifactDto
import com.example.data.network.GitHubJobDto
import com.example.domain.JobLogParser
import com.example.domain.JobLogReport
import com.example.domain.LogFileLocation
import com.example.domain.LogSeverity
import com.example.domain.ParsedLogLine
import com.example.ui.components.BrowserHelper
import com.example.ui.components.FormatUtils
import com.example.ui.components.MonoBadge
import com.example.ui.components.RunStatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
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
  onLoadJobLogs: (jobId: Long, jobName: String) -> Unit,
  onCloseJobLogs: () -> Unit,
  onExplainError: (jobName: String, report: JobLogReport, focusLine: ParsedLogLine?) -> Unit,
  onDismissAiDiagnosis: () -> Unit,
  onOpenFileInEditor: (filePath: String, lineNumber: Int, errorLineText: String, aiPatchHint: String?) -> Unit,
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
        val isLogOpen = state.selectedLogJobId == job.id
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          JobStepsCard(
            job = job,
            strings = strings,
            expanded = isExpanded,
            isLogOpen = isLogOpen,
            onToggle = { expandedJobs[job.id] = !isExpanded },
            onToggleLogs = {
              if (isLogOpen) onCloseJobLogs() else onLoadJobLogs(job.id, job.name)
            },
          )
          if (isLogOpen) {
            val activeReport = state.jobLogReport?.takeIf { it.jobId == job.id }
            val repoShortName = repoFullName.substringAfterLast('/')
            if (state.aiDiagnosisState !is AiDiagnosisUiState.Idle) {
              val focusedLogLine =
                when (val ai = state.aiDiagnosisState) {
                  is AiDiagnosisUiState.Success ->
                    ai.diagnosis.focusLineNumber?.let { ln ->
                      activeReport?.lines?.firstOrNull { it.lineNumber == ln }
                    }
                  else -> null
                } ?: activeReport?.errorLines?.firstOrNull()
              val detectedLocation =
                focusedLogLine?.let { JobLogParser.extractFileLocation(it.text, repoShortName) }

              AiDiagnosisCard(
                strings = strings,
                aiState = state.aiDiagnosisState,
                detectedLocation = detectedLocation,
                onRetry = { lineNum ->
                  if (activeReport != null) {
                    val lineObj =
                      lineNum?.let { n -> activeReport.lines.firstOrNull { it.lineNumber == n } }
                    onExplainError(job.name, activeReport, lineObj)
                  }
                },
                onDismiss = onDismissAiDiagnosis,
                onOpenEditor = { loc, patchHint ->
                  onOpenFileInEditor(
                    loc.filePath,
                    loc.lineNumber,
                    focusedLogLine?.text.orEmpty(),
                    patchHint,
                  )
                },
              )
            }
            JobLogViewerCard(
              job = job,
              repoShortName = repoShortName,
              strings = strings,
              isLoading = state.isLoadingLogs,
              isAnalyzingAi = state.aiDiagnosisState is AiDiagnosisUiState.Loading,
              report = activeReport,
              errorMessage = state.logErrorMessage,
              onRefresh = { onLoadJobLogs(job.id, job.name) },
              onExplainAll = { report -> onExplainError(job.name, report, null) },
              onExplainLine = { report, line -> onExplainError(job.name, report, line) },
              onOpenFile = { loc, lineText ->
                val hint = (state.aiDiagnosisState as? AiDiagnosisUiState.Success)?.diagnosis?.patchSnippet
                onOpenFileInEditor(loc.filePath, loc.lineNumber, lineText, hint)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun JobStepsCard(
  job: GitHubJobDto,
  strings: AppStrings,
  expanded: Boolean,
  isLogOpen: Boolean,
  onToggle: () -> Unit,
  onToggleLogs: () -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 6.dp)) {
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

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        if (isLogOpen) {
          FilledTonalButton(
            onClick = onToggleLogs,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_logs_${job.id}"),
          ) {
            Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.hideLogs)
          }
        } else {
          OutlinedButton(
            onClick = onToggleLogs,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_logs_${job.id}"),
          ) {
            Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.viewLogs)
          }
        }
      }
    }
  }
}

@Composable
private fun AiDiagnosisCard(
  strings: AppStrings,
  aiState: AiDiagnosisUiState,
  detectedLocation: LogFileLocation?,
  onRetry: (focusLineNumber: Int?) -> Unit,
  onDismiss: () -> Unit,
  onOpenEditor: (LogFileLocation, String?) -> Unit,
) {
  val context = LocalContext.current
  val statusPalette = LocalStatusPalette.current

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (aiState is AiDiagnosisUiState.Error) {
            MaterialTheme.colorScheme.errorContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          }
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth().testTag("ai_diagnosis_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = strings.aiDiagnosisTitle,
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
        )
        val focusLine =
          when (aiState) {
            is AiDiagnosisUiState.Loading -> aiState.focusLineNumber
            is AiDiagnosisUiState.Success -> aiState.diagnosis.focusLineNumber
            is AiDiagnosisUiState.Error -> aiState.focusLineNumber
            AiDiagnosisUiState.Idle -> null
          }
        if (focusLine != null) {
          MonoBadge(text = "L$focusLine")
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("dismiss_ai_diagnosis_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = strings.dismissAiDiagnosis,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      when (aiState) {
        AiDiagnosisUiState.Idle -> Unit
        is AiDiagnosisUiState.Loading -> {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = strings.analyzingErrorWithAi,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        is AiDiagnosisUiState.Error -> {
          Text(
            text = aiState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
              onClick = { onRetry(aiState.focusLineNumber) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("retry_ai_diagnosis_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.retryAiAnalysis)
            }
          }
        }
        is AiDiagnosisUiState.Success -> {
          val diagnosis = aiState.diagnosis
          Text(
            text = diagnosis.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = strings.aiRootCauseLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = diagnosis.rootCause,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          if (diagnosis.fixSteps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = strings.aiFixStepsLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              diagnosis.fixSteps.forEachIndexed { idx, step ->
                Text(
                  text = "${idx + 1}. $step",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          if (diagnosis.patchSnippet.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.aiFixSnippetLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                FilledTonalButton(
                  onClick = {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                      ClipData.newPlainText("DroidForge AI Fix", diagnosis.patchSnippet)
                    )
                    Toast.makeText(context, strings.fixCopied, Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_ai_fix_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(strings.copyFix)
                }
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusPalette.codeSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
              ) {
                Text(
                  text = diagnosis.patchSnippet,
                  style = CodeTextStyle,
                  color = EmeraldMint,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(12.dp),
                )
              }
            }
          }

          if (detectedLocation != null) {
            FilledTonalButton(
              onClick = {
                onOpenEditor(detectedLocation, diagnosis.patchSnippet.takeIf { it.isNotBlank() })
              },
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("open_editor_from_ai_button"),
            ) {
              Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("${strings.openFileInEditor}: ${detectedLocation.filePath.substringAfterLast('/')}:${detectedLocation.lineNumber}")
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobLogViewerCard(
  job: GitHubJobDto,
  repoShortName: String,
  strings: AppStrings,
  isLoading: Boolean,
  isAnalyzingAi: Boolean,
  report: JobLogReport?,
  errorMessage: String?,
  onRefresh: () -> Unit,
  onExplainAll: (JobLogReport) -> Unit,
  onExplainLine: (JobLogReport, ParsedLogLine) -> Unit,
  onOpenFile: (LogFileLocation, String) -> Unit,
) {
  val context = LocalContext.current
  val statusPalette = LocalStatusPalette.current
  var errorsOnly by rememberSaveable(job.id) { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = statusPalette.codeSurface,
    border = BorderStroke(1.dp, ObsidianOutline),
    modifier = Modifier.fillMaxWidth().testTag("job_log_viewer_${job.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Terminal,
          contentDescription = null,
          tint = EmeraldMint,
          modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = job.name,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (report != null) {
            Text(
              text = "${report.totalLines} lines · ${report.errorLines.size} errors",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark,
            )
          }
        }
        IconButton(
          onClick = {
            val activeLines =
              if (report == null) emptyList()
              else if (errorsOnly) report.errorLines else report.lines
            val payload = activeLines.joinToString("\n") { "${it.lineNumber}: ${it.text}" }
            if (payload.isNotBlank()) {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("DroidForge Build Logs", payload))
              Toast.makeText(context, strings.logsCopied, Toast.LENGTH_SHORT).show()
            }
          },
          enabled = report != null,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_logs_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = strings.copyLogs,
            tint = TextPrimaryDark,
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onRefresh,
          enabled = !isLoading,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_logs_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = strings.refresh,
            tint = TextPrimaryDark,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
          text = strings.loadingLogs,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondaryDark,
        )
      }

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CoralRed.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.45f)),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = CoralRed,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
          )
        }
      }

      if (report != null) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = errorsOnly,
            onClick = { errorsOnly = true },
            label = { Text("${strings.errorsOnly} (${report.errorLines.size})") },
            colors =
              FilterChipDefaults.filterChipColors(
                containerColor = ObsidianSurfaceHigh,
                labelColor = TextSecondaryDark,
                selectedContainerColor = CoralRed.copy(alpha = 0.22f),
                selectedLabelColor = CoralRed,
              ),
            modifier = Modifier.testTag("filter_errors_only_chip"),
          )
          FilterChip(
            selected = !errorsOnly,
            onClick = { errorsOnly = false },
            label = { Text("${strings.fullLog} (${report.lines.size})") },
            colors =
              FilterChipDefaults.filterChipColors(
                containerColor = ObsidianSurfaceHigh,
                labelColor = TextSecondaryDark,
                selectedContainerColor = EmeraldMint.copy(alpha = 0.20f),
                selectedLabelColor = EmeraldMint,
              ),
            modifier = Modifier.testTag("filter_full_log_chip"),
          )
          FilledTonalButton(
            onClick = { onExplainAll(report) },
            enabled = !isAnalyzingAi && report.lines.isNotEmpty(),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = SkyCyan.copy(alpha = 0.18f),
                contentColor = SkyCyan,
              ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("ai_explain_all_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(text = strings.explainWithAi, style = MaterialTheme.typography.labelMedium)
          }
        }

        val displayedLines = if (errorsOnly) report.errorLines else report.lines
        if (displayedLines.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          ) {
            Text(
              text = strings.noErrorsInLog,
              style = MaterialTheme.typography.bodySmall,
              color = EmeraldMint,
            )
          }
        } else {
          val vScroll = rememberScrollState()
          val hScroll = rememberScrollState()
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            displayedLines.forEach { logLine ->
              val lineColor =
                when (logLine.severity) {
                  LogSeverity.ERROR -> CoralRed
                  LogSeverity.WARNING -> AmberWarm
                  LogSeverity.NORMAL -> TextPrimaryDark
                }
              val lineBg =
                when (logLine.severity) {
                  LogSeverity.ERROR -> CoralRed.copy(alpha = 0.14f)
                  LogSeverity.WARNING -> AmberWarm.copy(alpha = 0.10f)
                  LogSeverity.NORMAL -> Color.Transparent
                }
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .background(lineBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = logLine.lineNumber.toString().padStart(4, ' '),
                  style = CodeTextStyle,
                  color = TextSecondaryDark,
                )
                if (logLine.severity == LogSeverity.ERROR) {
                  Surface(
                    onClick = { if (!isAnalyzingAi) onExplainLine(report, logLine) },
                    shape = RoundedCornerShape(8.dp),
                    color = SkyCyan.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.50f)),
                    modifier = Modifier.testTag("ai_explain_line_${logLine.lineNumber}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = strings.explainWithAi,
                        tint = SkyCyan,
                        modifier = Modifier.size(12.dp),
                      )
                      Text(
                        text = strings.explainWithAi,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyCyan,
                      )
                    }
                  }
                }
                val fileLocation =
                  if (logLine.severity != LogSeverity.NORMAL) {
                    JobLogParser.extractFileLocation(logLine.text, repoShortName)
                  } else {
                    null
                  }
                if (fileLocation != null) {
                  Surface(
                    onClick = { onOpenFile(fileLocation, logLine.text) },
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldMint.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.50f)),
                    modifier = Modifier.testTag("open_file_line_${logLine.lineNumber}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = strings.openFileInEditor,
                        tint = EmeraldMint,
                        modifier = Modifier.size(12.dp),
                      )
                      Text(
                        text = "L${fileLocation.lineNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldMint,
                      )
                    }
                  }
                }
                Text(
                  text = logLine.text,
                  style = CodeTextStyle,
                  color = lineColor,
                )
              }
            }
          }
        }
      }
    }
  }
}
