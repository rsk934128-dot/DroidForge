package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.prefs.AuthMethod
import com.example.data.prefs.UserPrefs
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.SettingsVerificationState

@Composable
fun SettingsScreen(
  strings: AppStrings,
  prefs: UserPrefs,
  verificationState: SettingsVerificationState,
  onStartOAuth: () -> Unit,
  onSaveCredentials: (pat: String, defaultHandle: String) -> Unit,
  onClearPat: () -> Unit,
  onVerifyConnection: () -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSelectTheme: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  var patInput by remember(prefs.githubPat) { mutableStateOf(prefs.githubPat) }
  var handleInput by remember(prefs.defaultHandle) { mutableStateOf(prefs.defaultHandle) }
  var showPat by rememberSaveable { mutableStateOf(false) }
  val statusPalette = LocalStatusPalette.current

  LaunchedEffect(Unit) { onVerifyConnection() }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(text = strings.settingsTitle, style = MaterialTheme.typography.headlineSmall)
    }

    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = strings.oauthCardTitle, style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = strings.oauthCardDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (prefs.authMethod == AuthMethod.OAUTH) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = statusPalette.successContainer,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.oauthSignedInBadge,
                  style = MaterialTheme.typography.labelLarge,
                  color = statusPalette.success,
                  modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                  onClick = {
                    patInput = ""
                    onClearPat()
                  },
                  modifier = Modifier.testTag("oauth_sign_out_button"),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(strings.signOutGitHub)
                }
              }
            }
          } else {
            Button(
              onClick = onStartOAuth,
              enabled = !verificationState.isOAuthInProgress,
              modifier = Modifier.fillMaxWidth().testTag("oauth_sign_in_button"),
            ) {
              if (verificationState.isOAuthInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
              } else {
                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
              }
              Text(strings.signInWithGitHub)
            }
            if (!verificationState.isOAuthConfigured) {
              Text(
                text = strings.oauthSetupHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = strings.githubAuthTitle, style = MaterialTheme.typography.titleMedium)
          }

          Text(
            text = strings.githubAuthDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = patInput,
            onValueChange = { patInput = it },
            label = { Text(strings.patLabel) },
            singleLine = true,
            visualTransformation = if (showPat) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showPat = !showPat }) {
                Icon(
                  imageVector = if (showPat) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                  contentDescription = "Toggle token visibility",
                )
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("github_pat_input"),
          )

          OutlinedTextField(
            value = handleInput,
            onValueChange = { handleInput = it },
            label = { Text(strings.defaultHandleLabel) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("default_handle_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onSaveCredentials(patInput, handleInput) },
              modifier = Modifier.weight(1f).testTag("save_github_settings_button"),
            ) {
              Text(strings.saveConnection)
            }
            FilledTonalButton(
              onClick = onVerifyConnection,
              enabled = !verificationState.isVerifying,
              modifier = Modifier.testTag("verify_github_button"),
            ) {
              if (verificationState.isVerifying) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.testConnection)
              }
            }
            if (prefs.githubPat.isNotBlank()) {
              OutlinedButton(
                onClick = {
                  patInput = ""
                  onClearPat()
                },
                modifier = Modifier.testTag("clear_pat_button"),
              ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = strings.clearToken, modifier = Modifier.size(18.dp))
              }
            }
          }

          verificationState.verifiedUser?.let { user ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = statusPalette.successContainer,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                AsyncImage(
                  model = user.avatarUrl,
                  contentDescription = user.login,
                  modifier = Modifier.size(34.dp).clip(CircleShape),
                  contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = user.name?.takeIf { it.isNotBlank() } ?: user.login,
                    style = MaterialTheme.typography.titleSmall,
                    color = statusPalette.success,
                  )
                  Text(
                    text = "@${user.login} · ${user.publicRepos} public repos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = statusPalette.success)
              }
            }
          }

          verificationState.errorMessage?.let { err ->
            Text(
              text = err,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }

          verificationState.rateLimit?.let { limit ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(strings.rateLimitTitle, style = MaterialTheme.typography.labelLarge)
                Text(
                  text = "${limit.remaining} / ${limit.limit}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
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
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = strings.languageLabel, style = MaterialTheme.typography.titleMedium)
          }

          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AppLanguage.entries.forEachIndexed { idx, lang ->
              SegmentedButton(
                selected = prefs.language == lang,
                onClick = { onSelectLanguage(lang) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = AppLanguage.entries.size),
                modifier = Modifier.testTag("lang_${lang.code}"),
              ) {
                Text(lang.label)
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = strings.appearanceLabel, style = MaterialTheme.typography.titleMedium)
          }

          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val themes =
              listOf(
                ThemeMode.SYSTEM to strings.themeSystem,
                ThemeMode.DARK to strings.themeDark,
                ThemeMode.LIGHT to strings.themeLight,
              )
            themes.forEachIndexed { idx, (mode, label) ->
              SegmentedButton(
                selected = prefs.themeMode == mode,
                onClick = { onSelectTheme(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = themes.size),
                modifier = Modifier.testTag("theme_${mode.name}"),
              ) {
                Text(label)
              }
            }
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
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(text = strings.installGuideTitle, style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = strings.installGuideBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
