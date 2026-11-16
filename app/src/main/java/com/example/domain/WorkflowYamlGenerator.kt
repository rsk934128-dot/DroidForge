package com.example.domain

enum class GradleBuildTask(val command: String, val label: String, val outputGlob: String) {
  ASSEMBLE_DEBUG("assembleDebug", "Debug APK", "app/build/outputs/apk/debug/*.apk"),
  ASSEMBLE_RELEASE("assembleRelease", "Release APK", "app/build/outputs/apk/release/*.apk"),
  BUNDLE_RELEASE("bundleRelease", "Play AAB", "app/build/outputs/bundle/release/*.aab"),
}

enum class WorkflowTriggerMode(val labelEn: String, val labelBn: String) {
  MANUAL_AND_PUSH("Push + Manual", "Push + ম্যানুয়াল"),
  MANUAL_ONLY("Manual Only", "শুধু ম্যানুয়াল"),
}

data class WorkflowConfig(
  val task: GradleBuildTask = GradleBuildTask.ASSEMBLE_DEBUG,
  val jdkVersion: String = "21",
  val branch: String = "main",
  val triggerMode: WorkflowTriggerMode = WorkflowTriggerMode.MANUAL_AND_PUSH,
  val publishRelease: Boolean = true,
)

object WorkflowYamlGenerator {
  fun generate(config: WorkflowConfig): String {
    val cleanBranch = config.branch.trim().ifEmpty { "main" }
    val pushBlock =
      if (config.triggerMode == WorkflowTriggerMode.MANUAL_AND_PUSH) {
        """
          push:
            branches: [ "$cleanBranch" ]
        """.trimIndent()
      } else {
        ""
      }

    val releaseBlock =
      if (config.publishRelease && config.task != GradleBuildTask.BUNDLE_RELEASE) {
        """

          - name: Publish APK Release
            if: github.ref == 'refs/heads/$cleanBranch'
            uses: softprops/action-gh-release@v2
            with:
              tag_name: build-${'$'}{{ github.run_number }}
              name: Build #${'$'}{{ github.run_number }} (${config.task.label})
              files: ${config.task.outputGlob}
            env:
              GITHUB_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        """.trimIndent()
      } else {
        ""
      }

    return buildString {
      appendLine("name: Android Cloud APK Build")
      appendLine()
      appendLine("on:")
      if (pushBlock.isNotEmpty()) {
        appendLine("  push:")
        appendLine("    branches: [ \"$cleanBranch\" ]")
      }
      appendLine("  workflow_dispatch:")
      appendLine("    inputs:")
      appendLine("      build_note:")
      appendLine("        description: 'Build note'")
      appendLine("        required: false")
      appendLine("        default: 'Triggered from DroidForge'")
      appendLine()
      if (config.publishRelease && config.task != GradleBuildTask.BUNDLE_RELEASE) {
        appendLine("permissions:")
        appendLine("  contents: write")
        appendLine()
      }
      appendLine("jobs:")
      appendLine("  build-android:")
      appendLine("    runs-on: ubuntu-latest")
      appendLine("    steps:")
      appendLine("      - name: Checkout repository")
      appendLine("        uses: actions/checkout@v4")
      appendLine()
      appendLine("      - name: Set up JDK ${config.jdkVersion}")
      appendLine("        uses: actions/setup-java@v4")
      appendLine("        with:")
      appendLine("          distribution: 'temurin'")
      appendLine("          java-version: '${config.jdkVersion}'")
      appendLine()
      appendLine("      - name: Setup Gradle")
      appendLine("        uses: gradle/actions/setup-gradle@v4")
      appendLine()
      appendLine("      - name: Grant execute permission for gradlew")
      appendLine("        run: chmod +x gradlew")
      appendLine()
      appendLine("      - name: Build ${config.task.label}")
      appendLine("        run: ./gradlew ${config.task.command}")
      appendLine()
      appendLine("      - name: Upload Build Artifact")
      appendLine("        uses: actions/upload-artifact@v4")
      appendLine("        with:")
      appendLine("          name: droidforge-${config.task.command}")
      appendLine("          path: ${config.task.outputGlob}")
      if (releaseBlock.isNotEmpty()) {
        appendLine()
        releaseBlock.lines().forEach { line ->
          if (line.isBlank()) appendLine() else appendLine("      $line")
        }
      }
    }.trimEnd()
  }
}
