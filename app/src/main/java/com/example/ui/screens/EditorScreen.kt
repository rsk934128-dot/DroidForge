package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DiffLineType
import com.example.ui.components.BrowserHelper
import com.example.ui.components.MonoBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.EditorUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
  strings: AppStrings,
  state: EditorUiState,
  hasPat: Boolean,
  onBack: () -> Unit,
  onRetryLoad: () -> Unit,
  onEditContent: (String) -> Unit,
  onResetEdits: () -> Unit,
  onGenerateAiPatch: () -> Unit,
  onNewBranchChange: (String) -> Unit,
  onCommitMessageChange: (String) -> Unit,
  onPrTitleChange: (String) -> Unit,
  onPrBodyChange: (String) -> Unit,
  onCreatePr: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val statusPalette = LocalStatusPalette.current
  val coroutineScope = rememberCoroutineScope()
  var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Code, 1 = Diff, 2 = Safe PR
  var isTextEditMode by rememberSaveable { mutableStateOf(false) }

  val codeLines =
    androidx.compose.runtime.remember(state.editedContent) {
      if (state.editedContent.isEmpty()) emptyList() else state.editedContent.split('\n')
    }
  val codeListState = rememberLazyListState()

  LaunchedEffect(state.filePath, state.originalContent, state.targetLineNumber) {
    if (codeLines.isNotEmpty() && !isTextEditMode) {
      val targetIdx = (state.targetLineNumber - 1).coerceIn(0, codeLines.lastIndex)
      val scrollTarget = (targetIdx - 3).coerceAtLeast(0)
      codeListState.animateScrollToItem(scrollTarget)
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      windowInsets = WindowInsets(0.dp),
      title = {
        Column {
          Text(
            text = state.filePath.substringAfterLast('/').ifBlank { strings.editorTitle },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${state.repoFullName} · ${state.baseBranch}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      },
      navigationIcon = {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_back_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
      },
      actions = {
        if (state.fileHtmlUrl.isNotBlank()) {
          IconButton(
            onClick = { BrowserHelper.openUrl(context, state.fileHtmlUrl) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_open_github_file"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = strings.openInGitHub)
          }
        }
      },
    )

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (state.errorText.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = CoralRed,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "L${state.targetLineNumber}: ${state.errorText}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      val diffBadge =
        if (state.diffSummary.hasChanges) {
          " (+${state.diffSummary.addedCount}/-${state.diffSummary.removedCount})"
        } else {
          ""
        }

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
          icon = { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("editor_tab_code"),
        ) {
          Text(strings.editorTabCode, maxLines = 1)
        }
        SegmentedButton(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
          icon = { Icon(Icons.Rounded.Difference, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("editor_tab_diff"),
        ) {
          Text("${strings.editorTabDiff}$diffBadge", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SegmentedButton(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
          icon = { Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("editor_tab_pr"),
        ) {
          Text(strings.editorTabPr, maxLines = 1)
        }
      }

      if (state.isLoadingFile || state.isGeneratingAiFix || state.isCreatingPr) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      if (state.isSavingDraft || state.lastDraftSavedAt != null || state.isDraftRestored || state.isOfflineDraftMode) {
        val statusIcon =
          when {
            state.isOfflineDraftMode -> Icons.Rounded.CloudOff
            state.isSavingDraft -> Icons.Rounded.Save
            else -> Icons.Rounded.CloudDone
          }
        val statusText =
          when {
            state.isOfflineDraftMode -> strings.draftOfflineModeBanner
            state.isSavingDraft -> strings.draftSaving
            state.isDraftRestored -> strings.draftRestoredBanner
            else -> strings.draftSavedOffline
          }
        val statusColor =
          if (state.isOfflineDraftMode) MaterialTheme.colorScheme.tertiary
          else EmeraldMint
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = statusColor.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().testTag("editor_draft_status_banner"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = statusText,
              tint = statusColor,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelMedium,
              color = statusColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      if (state.loadErrorMessage != null) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = state.loadErrorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
            FilledTonalButton(
              onClick = onRetryLoad,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_retry_load"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.retryAiAnalysis)
            }
          }
        }
      }

      if (state.aiExplanation != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = state.aiExplanation,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      if (state.aiFixError != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
        ) {
          Text(
            text = state.aiFixError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
          )
        }
      }

      if (state.loadErrorMessage == null && !state.isLoadingFile) {
        when (selectedTab) {
          0 -> {
            // Action bar for Code Editor
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (!isTextEditMode && codeLines.isNotEmpty()) {
                OutlinedButton(
                  onClick = {
                    val idx = (state.targetLineNumber - 1).coerceIn(0, codeLines.lastIndex)
                    coroutineScope.launch {
                      codeListState.animateScrollToItem((idx - 3).coerceAtLeast(0))
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_jump_to_line_button"),
                ) {
                  Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("${strings.jumpToLine}${state.targetLineNumber}")
                }
              }

              OutlinedButton(
                onClick = { isTextEditMode = !isTextEditMode },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_toggle_edit_mode"),
              ) {
                Icon(
                  imageVector = if (isTextEditMode) Icons.Rounded.Visibility else Icons.Rounded.Edit,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isTextEditMode) strings.viewLinesToggle else strings.editSourceToggle)
              }

              FilledTonalButton(
                onClick = { onGenerateAiPatch() },
                enabled = !state.isGeneratingAiFix && state.editedContent.isNotBlank(),
                colors =
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = SkyCyan.copy(alpha = 0.18f),
                    contentColor = SkyCyan,
                  ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_ai_fix_button"),
              ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.aiAutoPatch)
              }

              if (state.diffSummary.hasChanges) {
                OutlinedButton(
                  onClick = onResetEdits,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_reset_button"),
                ) {
                  Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(strings.resetEdits)
                }
              }
            }

            if (!state.aiPatchHint.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusPalette.codeSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = state.aiPatchHint,
                    style = CodeTextStyle,
                    color = EmeraldMint,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(
                    onClick = {
                      val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      clipboard.setPrimaryClip(ClipData.newPlainText("AI Patch Hint", state.aiPatchHint))
                      Toast.makeText(context, strings.fixCopied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_copy_hint_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.ContentCopy,
                      contentDescription = strings.copyFix,
                      tint = TextPrimaryDark,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }

            if (isTextEditMode) {
              OutlinedTextField(
                value = state.editedContent,
                onValueChange = onEditContent,
                textStyle = CodeTextStyle,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_source_textfield"),
              )
            } else {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusPalette.codeSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.weight(1f).fillMaxWidth(),
              ) {
                LazyColumn(
                  state = codeListState,
                  contentPadding = PaddingValues(vertical = 8.dp),
                  modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                ) {
                  itemsIndexed(codeLines) { idx, lineText ->
                    val lineNum = idx + 1
                    val isTargetLine = lineNum == state.targetLineNumber
                    val rowBg = if (isTargetLine) CoralRed.copy(alpha = 0.22f) else Color.Transparent
                    val numColor = if (isTargetLine) CoralRed else TextSecondaryDark
                    Row(
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .background(rowBg)
                          .clickable { isTextEditMode = true }
                          .padding(horizontal = 10.dp, vertical = 3.dp),
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = lineNum.toString().padStart(4, ' '),
                        style = CodeTextStyle,
                        color = numColor,
                      )
                      Text(
                        text = lineText,
                        style = CodeTextStyle,
                        color = if (isTargetLine) TextPrimaryDark else TextPrimaryDark.copy(alpha = 0.9f),
                      )
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // Diff Preview Tab
            if (!state.diffSummary.hasChanges) {
              Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.padding(16.dp),
                ) {
                  Text(
                    text = strings.noDiffYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                      onClick = { onGenerateAiPatch() },
                      enabled = !state.isGeneratingAiFix,
                    ) {
                      Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text(strings.aiAutoPatch)
                    }
                    OutlinedButton(onClick = {
                      isTextEditMode = true
                      selectedTab = 0
                    }) {
                      Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text(strings.editSourceToggle)
                    }
                  }
                }
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                MonoBadge(text = "+${state.diffSummary.addedCount}")
                MonoBadge(text = "-${state.diffSummary.removedCount}")
                Text(
                  text = state.filePath,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusPalette.codeSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("editor_diff_viewer"),
              ) {
                LazyColumn(
                  contentPadding = PaddingValues(vertical = 8.dp),
                  modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                ) {
                  itemsIndexed(state.diffSummary.lines) { _, diffLine ->
                    val prefix =
                      when (diffLine.type) {
                        DiffLineType.ADDED -> "+"
                        DiffLineType.REMOVED -> "-"
                        DiffLineType.UNCHANGED -> " "
                      }
                    val bg =
                      when (diffLine.type) {
                        DiffLineType.ADDED -> EmeraldMint.copy(alpha = 0.16f)
                        DiffLineType.REMOVED -> CoralRed.copy(alpha = 0.16f)
                        DiffLineType.UNCHANGED -> Color.Transparent
                      }
                    val fg =
                      when (diffLine.type) {
                        DiffLineType.ADDED -> EmeraldMint
                        DiffLineType.REMOVED -> CoralRed
                        DiffLineType.UNCHANGED -> TextPrimaryDark.copy(alpha = 0.78f)
                      }
                    val lineLabel =
                      (diffLine.newLineNumber ?: diffLine.oldLineNumber)?.toString()?.padStart(4, ' ')
                        ?: "    "
                    Row(
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .background(bg)
                          .padding(horizontal = 10.dp, vertical = 2.dp),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(text = lineLabel, style = CodeTextStyle, color = TextSecondaryDark)
                      Text(text = prefix, style = CodeTextStyle, color = fg)
                      Text(text = diffLine.text, style = CodeTextStyle, color = fg)
                    }
                  }
                }
              }
            }
          }

          2 -> {
            // Safe Branch + Pull Request Tab
            Column(
              modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              if (state.prOutcome != null) {
                val outcome = state.prOutcome
                Card(
                  shape = RoundedCornerShape(18.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                  modifier = Modifier.fillMaxWidth().testTag("pr_success_card"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                      )
                      Text(
                        text = "${strings.prCreatedTitle} (#${outcome.prNumber})",
                        style = MaterialTheme.typography.titleSmall,
                      )
                    }
                    MonoBadge(text = "${outcome.branchName} -> ${state.baseBranch}")
                    Button(
                      onClick = { BrowserHelper.openUrl(context, outcome.prUrl) },
                      modifier = Modifier.fillMaxWidth().testTag("open_created_pr_button"),
                    ) {
                      Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(8.dp))
                      Text(strings.openPrOnGitHub)
                    }
                  }
                }
              }

              if (!hasPat) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                  Text(
                    text = strings.needsPatHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                  )
                }
              }

              if (state.prErrorMessage != null) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.errorContainer,
                ) {
                  Text(
                    text = state.prErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                MonoBadge(text = "Base: ${state.baseBranch}")
                MonoBadge(text = "+${state.diffSummary.addedCount} / -${state.diffSummary.removedCount}")
              }

              OutlinedTextField(
                value = state.newBranchName,
                onValueChange = onNewBranchChange,
                label = { Text(strings.newBranchLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pr_branch_input"),
              )
              OutlinedTextField(
                value = state.commitMessage,
                onValueChange = onCommitMessageChange,
                label = { Text(strings.commitMsgLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pr_commit_input"),
              )
              OutlinedTextField(
                value = state.prTitle,
                onValueChange = onPrTitleChange,
                label = { Text(strings.prTitleLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pr_title_input"),
              )
              OutlinedTextField(
                value = state.prBody,
                onValueChange = onPrBodyChange,
                label = { Text(strings.prBodyLabel) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("pr_body_input"),
              )

              Button(
                onClick = onCreatePr,
                enabled =
                  hasPat &&
                    state.diffSummary.hasChanges &&
                    state.newBranchName.isNotBlank() &&
                    !state.isCreatingPr,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("editor_create_pr_button"),
              ) {
                Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.createBranchPrButton)
              }
            }
          }
        }
      }
    }
  }
}
