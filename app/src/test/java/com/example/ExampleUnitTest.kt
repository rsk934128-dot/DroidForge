package com.example

import com.example.domain.GradleBuildTask
import com.example.domain.PortalAccent
import com.example.domain.VercelPortalConfig
import com.example.domain.VercelPortalGenerator
import com.example.domain.WorkflowConfig
import com.example.domain.WorkflowTriggerMode
import com.example.domain.WorkflowYamlGenerator
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
}
