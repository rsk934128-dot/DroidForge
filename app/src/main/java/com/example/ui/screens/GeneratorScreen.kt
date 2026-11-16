package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.GradleBuildTask
import com.example.domain.PortalAccent
import com.example.domain.WorkflowTriggerMode
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.viewmodel.VercelPortalUiState
import com.example.ui.viewmodel.YamlStudioUiState

@Composable
fun GeneratorScreen(
  strings: AppStrings,
  language: AppLanguage,
  activeRepoFullName: String,
  state: YamlStudioUiState,
  vercelState: VercelPortalUiState,
  onSelectTask: (GradleBuildTask) -> Unit,
  onSelectJdk: (String) -> Unit,
  onBranchChange: (String) -> Unit,
  onSelectTriggerMode: (WorkflowTriggerMode) -> Unit,
  onToggleAutoRelease: (Boolean) -> Unit,
  onPushToRepo: () -> Unit,
  onPortalAppNameChange: (String) -> Unit,
  onPortalTaglineChange: (String) -> Unit,
  onPortalAccentSelect: (PortalAccent) -> Unit,
  onPortalSubfolderSelect: (Boolean) -> Unit,
  onPushVercelPortal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val statusPalette = LocalStatusPalette.current
  var selectedMode by rememberSaveable { mutableIntStateOf(0) } // 0 = YAML, 1 = Vercel Web Portal

  val exportYamlLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/yaml")
    ) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(state.generatedYaml.toByteArray(Charsets.UTF_8))
          }
          Toast.makeText(context, "Saved android-apk.yml", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
          Toast.makeText(context, "Failed to export file", Toast.LENGTH_SHORT).show()
        }
      }
    }

  val exportHtmlLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(vercelState.generatedHtml.toByteArray(Charsets.UTF_8))
          }
          Toast.makeText(context, "Saved index.html", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
          Toast.makeText(context, "Failed to export HTML", Toast.LENGTH_SHORT).show()
        }
      }
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedMode == 0,
          onClick = { selectedMode = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("studio_tab_yaml"),
        ) {
          Text(strings.studioTabYaml)
        }
        SegmentedButton(
          selected = selectedMode == 1,
          onClick = { selectedMode = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("studio_tab_vercel"),
        ) {
          Text(strings.studioTabVercel)
        }
      }
    }

    if (selectedMode == 0) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = strings.studioTitle, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = strings.studioSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
            Text(text = strings.gradleTaskLabel, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(GradleBuildTask.entries) { task ->
                FilterChip(
                  selected = state.config.task == task,
                  onClick = { onSelectTask(task) },
                  label = { Text(task.label) },
                  modifier = Modifier.testTag("yaml_task_${task.command}"),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = strings.jdkVersionLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf("17", "21").forEach { jdk ->
                    FilterChip(
                      selected = state.config.jdkVersion == jdk,
                      onClick = { onSelectJdk(jdk) },
                      label = { Text("JDK $jdk") },
                      modifier = Modifier.testTag("yaml_jdk_$jdk"),
                    )
                  }
                }
              }

              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = strings.triggerModeLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  WorkflowTriggerMode.entries.forEach { mode ->
                    FilterChip(
                      selected = state.config.triggerMode == mode,
                      onClick = { onSelectTriggerMode(mode) },
                      label = {
                        Text(
                          if (language == AppLanguage.BENGALI) mode.labelBn else mode.labelEn,
                          maxLines = 1,
                        )
                      },
                      modifier = Modifier.testTag("yaml_trigger_${mode.name}"),
                    )
                  }
                }
              }
            }

            OutlinedTextField(
              value = state.config.branch,
              onValueChange = onBranchChange,
              label = { Text(strings.targetBranchLabel) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("yaml_branch_input"),
            )

            if (state.config.task != GradleBuildTask.BUNDLE_RELEASE) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.autoReleaseLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f),
                )
                Switch(
                  checked = state.config.publishRelease,
                  onCheckedChange = onToggleAutoRelease,
                  modifier = Modifier.testTag("yaml_auto_release_switch"),
                )
              }
            }
          }
        }
      }

      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = statusPalette.codeSurface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
              text = ".github/workflows/android-apk.yml",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
              text = state.generatedYaml,
              style = CodeTextStyle,
              color = Color(0xFFE2E8F0),
              modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("android-apk.yml", state.generatedYaml))
              Toast.makeText(context, strings.yamlCopied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f).testTag("copy_yaml_button"),
          ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.copyYaml)
          }
          FilledTonalButton(
            onClick = { exportYamlLauncher.launch("android-apk.yml") },
            modifier = Modifier.weight(1f).testTag("export_yaml_button"),
          ) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.exportYaml)
          }
        }
      }

      item {
        Button(
          onClick = onPushToRepo,
          enabled = !state.isPushing && activeRepoFullName.isNotBlank(),
          modifier = Modifier.fillMaxWidth().testTag("push_yaml_button"),
        ) {
          if (state.isPushing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
          } else {
            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
          }
          Text(
            if (activeRepoFullName.isNotBlank()) "${strings.pushToRepo} ($activeRepoFullName)"
            else strings.selectRepoFirst
          )
        }
      }
    } else {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = strings.vercelTitle, style = MaterialTheme.typography.headlineSmall)
          }
          Text(
            text = strings.vercelSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
            OutlinedTextField(
              value = vercelState.config.appName,
              onValueChange = onPortalAppNameChange,
              label = { Text(strings.vercelAppNameLabel) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("vercel_app_name_input"),
            )

            OutlinedTextField(
              value = vercelState.config.tagline,
              onValueChange = onPortalTaglineChange,
              label = { Text(strings.vercelTaglineLabel) },
              maxLines = 2,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("vercel_tagline_input"),
            )

            Text(text = strings.vercelAccentLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              PortalAccent.entries.forEach { accent ->
                FilterChip(
                  selected = vercelState.config.accent == accent,
                  onClick = { onPortalAccentSelect(accent) },
                  label = { Text(accent.label) },
                  modifier = Modifier.testTag("vercel_accent_${accent.name}"),
                )
              }
            }

            Text(text = strings.vercelFolderLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = vercelState.config.useSubfolder,
                onClick = { onPortalSubfolderSelect(true) },
                label = { Text(strings.vercelFolderSub) },
                modifier = Modifier.testTag("vercel_path_subfolder"),
              )
              FilterChip(
                selected = !vercelState.config.useSubfolder,
                onClick = { onPortalSubfolderSelect(false) },
                label = { Text(strings.vercelFolderRoot) },
                modifier = Modifier.testTag("vercel_path_root"),
              )
            }
          }
        }
      }

      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = statusPalette.codeSurface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          val pathLabel =
            if (vercelState.config.useSubfolder) "vercel-site/index.html" else "index.html"
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
              text = pathLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
              text = vercelState.generatedHtml.lines().take(28).joinToString("\n") + "\n...",
              style = CodeTextStyle,
              color = Color(0xFFE2E8F0),
              modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("index.html", vercelState.generatedHtml))
              Toast.makeText(context, strings.vercelCopied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f).testTag("copy_vercel_html_button"),
          ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.copyYaml)
          }
          FilledTonalButton(
            onClick = { exportHtmlLauncher.launch("index.html") },
            modifier = Modifier.weight(1f).testTag("export_vercel_html_button"),
          ) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.exportYaml)
          }
        }
      }

      item {
        Button(
          onClick = onPushVercelPortal,
          enabled = !vercelState.isPushing && activeRepoFullName.isNotBlank(),
          modifier = Modifier.fillMaxWidth().testTag("push_vercel_portal_button"),
        ) {
          if (vercelState.isPushing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
          } else {
            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
          }
          Text(
            if (activeRepoFullName.isNotBlank()) "${strings.vercelPushButton} ($activeRepoFullName)"
            else strings.selectRepoFirst
          )
        }
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Text(
            text = strings.vercelDeploySteps,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }
  }
}
