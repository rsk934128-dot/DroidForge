package com.example

import com.example.domain.DiffEngine
import com.example.domain.DiffLineType
import com.example.domain.GradleBuildTask
import com.example.domain.JobLogParser
import com.example.domain.LogSeverity
import com.example.domain.PortalAccent
import com.example.domain.VercelPortalConfig
import com.example.domain.VercelPortalGenerator
import com.example.domain.WorkflowConfig
import com.example.domain.WorkflowTriggerMode
import com.example.domain.WorkflowYamlGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun workflowYamlGenerator_includesExpectedGradleTaskAndReleaseStep() {
    val yaml =
      WorkflowYamlGenerator.generate(
        WorkflowConfig(
          task = GradleBuildTask.ASSEMBLE_RELEASE,
          jdkVersion = "21",
          branch = "main",
          triggerMode = WorkflowTriggerMode.MANUAL_AND_PUSH,
          publishRelease = true,
        )
      )
    assertTrue(yaml.contains("./gradlew assembleRelease"))
    assertTrue(yaml.contains("java-version: '21'"))
    assertTrue(yaml.contains("softprops/action-gh-release@v2"))
    assertTrue(yaml.contains("workflow_dispatch:"))
  }

  @Test
  fun vercelPortalGenerator_generatesLiveReleaseHtmlAndConfig() {
    val html =
      VercelPortalGenerator.generateIndexHtml(
        VercelPortalConfig(
          appName = "DroidForge",
          tagline = "Mobile CI/CD Builder",
          repoFullName = "rsk934128-dot/droidforge",
          accent = PortalAccent.CYAN,
        )
      )
    val vercelJson = VercelPortalGenerator.generateVercelJson()
    assertTrue(html.contains("rsk934128-dot/droidforge"))
    assertTrue(html.contains("#38BDF8"))
    assertTrue(html.contains("api.github.com/repos/"))
    assertTrue(vercelJson.contains("cleanUrls"))
  }

  @Test
  fun jobLogParser_stripsAnsiTimestampsAndClassifiesErrorsAndWarnings() {
    val rawLog =
      listOf(
        "2026-11-16T10:12:00.1234567Z Starting Gradle Daemon...",
        "2026-11-16T10:12:04.0000000Z \u001B[33m##[warning]Deprecated Gradle features were used in this build\u001B[0m",
        "2026-11-16T10:12:08.5555555Z > Task :app:compileDebugKotlin FAILED",
        "2026-11-16T10:12:08.9999999Z \u001B[31me: file:///app/Main.kt:12:5 Unresolved reference: foo\u001B[0m",
        "2026-11-16T10:12:09.1000000Z BUILD FAILED in 9s",
      ).joinToString("\n")

    val report = JobLogParser.parse(jobId = 42L, jobName = "build-apk", rawText = rawLog)

    assertEquals(42L, report.jobId)
    assertEquals(5, report.totalLines)
    assertFalse(report.truncated)
    assertEquals(LogSeverity.NORMAL, report.lines[0].severity)
    assertEquals("Starting Gradle Daemon...", report.lines[0].text)
    assertEquals(LogSeverity.WARNING, report.lines[1].severity)
    assertTrue(report.lines[1].text.startsWith("##[warning]"))
    assertFalse(report.lines[1].text.contains("\u001B"))
    assertEquals(3, report.errorLines.size)
    assertTrue(report.errorLines.all { it.severity == LogSeverity.ERROR })
    assertEquals("BUILD FAILED in 9s", report.errorLines.last().text)
  }

  @Test
  fun jobLogParser_redactsSensitiveTokensAndPathsInAiContext() {
    val rawLog =
      listOf(
        "Running in /home/runner/work/DroidForge/app/src/main/java/Main.kt",
        "Authorization: Bearer ghp_SecretTokenValue1234567890",
        "GITHUB_TOKEN=ghp_AnotherSecretValue987654321",
        "e: file:///home/alice/project/Main.kt:42:10 Unresolved reference: bar",
        "BUILD FAILED in 4s",
      ).joinToString("\n")

    val report = JobLogParser.parse(jobId = 99L, jobName = "assembleDebug", rawText = rawLog)
    val sanitized = JobLogParser.buildSanitizedErrorContext(report, focusLine = report.errorLines.first())

    assertFalse(sanitized.contains("ghp_SecretTokenValue1234567890"))
    assertFalse(sanitized.contains("ghp_AnotherSecretValue987654321"))
    assertFalse(sanitized.contains("/home/runner"))
    assertFalse(sanitized.contains("/home/alice"))
    assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
    assertTrue(sanitized.contains("<USER_HOME>"))
    assertTrue(sanitized.contains("Unresolved reference: bar"))
  }

  @Test
  fun jobLogParser_extractsFilePathAndLineNumberFromCompilerAndGradleLogs() {
    val kotlinLoc =
      JobLogParser.extractFileLocation(
        line = "e: file:///home/runner/work/DroidForge/DroidForge/app/src/main/java/com/example/MainActivity.kt:42:5 Unresolved reference",
        repoName = "DroidForge",
      )
    assertNotNull(kotlinLoc)
    assertEquals("app/src/main/java/com/example/MainActivity.kt", kotlinLoc?.filePath)
    assertEquals(42, kotlinLoc?.lineNumber)
    assertEquals(5, kotlinLoc?.columnNumber)

    val gradleLoc =
      JobLogParser.extractFileLocation(
        line = "Build file '/github/workspace/app/build.gradle.kts' line: 19",
        repoName = "DroidForge",
      )
    assertNotNull(gradleLoc)
    assertEquals("app/build.gradle.kts", gradleLoc?.filePath)
    assertEquals(19, gradleLoc?.lineNumber)
  }

  @Test
  fun diffEngine_computesLineDiffAndAppliesLineRangePatch() {
    val original =
      """
      fun greet() {
        val msg = "hello"
        println(msgg)
      }
      """.trimIndent()
    val patched =
      DiffEngine.replaceLineRange(
        original = original,
        startLineOneIndexed = 3,
        endLineOneIndexed = 3,
        replacementSnippet = "  println(msg)",
      )
    val diff = DiffEngine.computeLineDiff(original, patched)

    assertTrue(diff.hasChanges)
    assertEquals(1, diff.addedCount)
    assertEquals(1, diff.removedCount)
    assertTrue(diff.lines.any { it.type == DiffLineType.REMOVED && it.text.contains("msgg") })
    assertTrue(diff.lines.any { it.type == DiffLineType.ADDED && it.text.contains("println(msg)") })
  }

  @Test
  fun githubRepository_draftKeyNormalizesLeadingSlashAndBlankBranch() {
    val key1 =
      com.example.data.repository.GitHubRepository.draftKeyFor(
        repoFullName = "rsk934128-dot/DroidForge",
        baseBranch = "",
        filePath = "/app/src/main/java/com/example/MainActivity.kt",
      )
    val key2 =
      com.example.data.repository.GitHubRepository.draftKeyFor(
        repoFullName = " rsk934128-dot/DroidForge ",
        baseBranch = "main",
        filePath = "app/src/main/java/com/example/MainActivity.kt",
      )
    assertEquals(key2, key1)
    assertEquals("rsk934128-dot/DroidForge::main::app/src/main/java/com/example/MainActivity.kt", key1)
  }
}
