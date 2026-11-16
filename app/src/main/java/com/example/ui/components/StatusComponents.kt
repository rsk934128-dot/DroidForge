package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalStatusPalette
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Composable
fun RunStatusPill(
  status: String?,
  conclusion: String?,
  strings: AppStrings,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val normalizedStatus = status?.lowercase(Locale.US) ?: "unknown"
  val normalizedConclusion = conclusion?.lowercase(Locale.US)

  val isRunning = normalizedStatus == "in_progress"
  val isQueued = normalizedStatus == "queued" || normalizedStatus == "waiting"

  val (label, tint, bgColor) =
    when {
      isRunning -> Triple(strings.statusRunning, palette.running, palette.runningContainer)
      isQueued -> Triple(strings.statusQueued, palette.queued, palette.queuedContainer)
      normalizedConclusion == "success" ->
        Triple(strings.statusSuccess, palette.success, palette.successContainer)
      normalizedConclusion == "cancelled" || normalizedConclusion == "skipped" ->
        Triple(
          strings.statusCancelled,
          MaterialTheme.colorScheme.onSurfaceVariant,
          MaterialTheme.colorScheme.surfaceContainerHigh,
        )
      else -> Triple(strings.statusFailed, palette.failed, palette.failedContainer)
    }

  val pulseAlpha =
    if (isRunning || isQueued) {
      val transition = rememberInfiniteTransition(label = "status_pulse")
      val alpha by
        transition.animateFloat(
          initialValue = 0.35f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
          label = "pulse_alpha",
        )
      alpha
    } else {
      1f
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = bgColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (isRunning || isQueued) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .alpha(pulseAlpha)
              .clip(CircleShape)
              .background(tint)
        )
      } else {
        val icon =
          when (normalizedConclusion) {
            "success" -> Icons.Rounded.CheckCircle
            "cancelled", "skipped" -> Icons.Rounded.Cancel
            else -> Icons.Rounded.Error
          }
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = tint,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = tint,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun MonoBadge(
  text: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = containerColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(13.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

object FormatUtils {
  fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(Locale.US, "%.2f MB", mb)
    else String.format(Locale.US, "%.0f KB", kb)
  }

  fun formatRelativeIso(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "--"
    return try {
      val instant = Instant.parse(isoTimestamp)
      val diff = Duration.between(instant, Instant.now())
      val mins = diff.toMinutes()
      when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
      }
    } catch (_: Exception) {
      isoTimestamp.take(10)
    }
  }

  fun formatStepDuration(startedAt: String?, completedAt: String?): String {
    if (startedAt.isNullOrBlank() || completedAt.isNullOrBlank()) return ""
    return try {
      val sec = Duration.between(Instant.parse(startedAt), Instant.parse(completedAt)).seconds
      if (sec < 60) "${sec}s" else "${sec / 60}m ${sec % 60}s"
    } catch (_: Exception) {
      ""
    }
  }
}

object BrowserHelper {
  fun openUrl(context: Context, url: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "No browser app found to open link", Toast.LENGTH_SHORT).show()
    }
  }
}

@Composable
fun ClockBadge(isoTimestamp: String?, modifier: Modifier = Modifier) {
  MonoBadge(
    text = FormatUtils.formatRelativeIso(isoTimestamp),
    icon = Icons.Rounded.Schedule,
    modifier = modifier,
  )
}
