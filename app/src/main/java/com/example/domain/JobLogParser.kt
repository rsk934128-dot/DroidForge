package com.example.domain

enum class LogSeverity {
  NORMAL,
  WARNING,
  ERROR,
}

data class ParsedLogLine(
  val lineNumber: Int,
  val text: String,
  val severity: LogSeverity,
)

data class JobLogReport(
  val jobId: Long,
  val jobName: String,
  val lines: List<ParsedLogLine>,
  val errorLines: List<ParsedLogLine>,
  val totalLines: Int,
  val truncated: Boolean,
)

object JobLogParser {
  private val ansiRegex = Regex("\u001B\\[[;\\d]*[ -/]*[@-~]")
  private val timestampPrefixRegex = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\s?")

  fun stripAnsi(input: String): String =
    input.replace(ansiRegex, "").replace(timestampPrefixRegex, "")

  fun classify(line: String): LogSeverity {
    val lower = line.lowercase()
    return when {
      lower.contains("##[error]") ||
        lower.contains("build failed") ||
        lower.contains("execution failed for task") ||
        lower.contains("e: file://") ||
        lower.contains("exception") ||
        lower.contains("error:") ||
        lower.contains("fatal:") ||
        lower.contains(" failed") -> LogSeverity.ERROR
      lower.contains("##[warning]") ||
        lower.contains("w: file://") ||
        lower.contains("warning:") -> LogSeverity.WARNING
      else -> LogSeverity.NORMAL
    }
  }

  fun parse(
    jobId: Long,
    jobName: String,
    rawText: String,
    maxTailLines: Int = 350,
  ): JobLogReport {
    val allRawLines = rawText.lineSequence().toList()
    val totalLines = allRawLines.size
    val parsedAll =
      allRawLines.mapIndexed { index, raw ->
        val clean = stripAnsi(raw)
        ParsedLogLine(
          lineNumber = index + 1,
          text = clean,
          severity = classify(clean),
        )
      }
    val errorLines = parsedAll.filter { it.severity == LogSeverity.ERROR }
    val truncated = parsedAll.size > maxTailLines
    val tailLines = if (truncated) parsedAll.takeLast(maxTailLines) else parsedAll
    return JobLogReport(
      jobId = jobId,
      jobName = jobName,
      lines = tailLines,
      errorLines = errorLines,
      totalLines = totalLines,
      truncated = truncated,
    )
  }

  private val gitHubTokenRegex = Regex("""\b(gh[pousr]_[A-Za-z0-9_]{12,}|github_pat_[A-Za-z0-9_]{12,})\b""")
  private val bearerRegex = Regex("""(?i)(Authorization:\s*Bearer\s+|Bearer\s+)[A-Za-z0-9._\-]{12,}""")
  private val secretAssignRegex =
    Regex("""(?i)\b(API_KEY|CLIENT_SECRET|KEY_PASSWORD|STORE_PASSWORD|TOKEN|SECRET)\s*[:=]\s*["']?([^\s"']+)["']?""")
  private val homePathRegex = Regex("""(/Users/[^/\s:]+|/home/[^/\s:]+|C:\\Users\\[^\\\s:]+)""")

  fun redactSensitiveData(input: String): String =
    input
      .replace(gitHubTokenRegex, "[REDACTED_TOKEN]")
      .replace(bearerRegex, "$1[REDACTED_TOKEN]")
      .replace(secretAssignRegex, "$1=[REDACTED]")
      .replace(homePathRegex, "<USER_HOME>")

  fun buildSanitizedErrorContext(
    report: JobLogReport,
    focusLine: ParsedLogLine? = null,
    maxChars: Int = 5500,
  ): String {
    val linesToUse = report.lines
    if (linesToUse.isEmpty()) return ""

    val selectedIndices = linkedSetOf<Int>()
    if (focusLine != null) {
      val targetIdx = linesToUse.indexOfFirst { it.lineNumber == focusLine.lineNumber }
      if (targetIdx >= 0) {
        val start = maxOf(0, targetIdx - 4)
        val end = minOf(linesToUse.lastIndex, targetIdx + 4)
        for (i in start..end) selectedIndices.add(i)
      }
    }

    if (selectedIndices.isEmpty()) {
      val errorIndices =
        linesToUse.indices.filter { linesToUse[it].severity == LogSeverity.ERROR }
      if (errorIndices.isNotEmpty()) {
        for (errIdx in errorIndices.takeLast(8)) {
          val start = maxOf(0, errIdx - 2)
          val end = minOf(linesToUse.lastIndex, errIdx + 2)
          for (i in start..end) selectedIndices.add(i)
        }
      } else {
        val start = maxOf(0, linesToUse.size - 40)
        for (i in start..linesToUse.lastIndex) selectedIndices.add(i)
      }
    }

    val sb = StringBuilder()
    for (idx in selectedIndices.sorted()) {
      val item = linesToUse[idx]
      val formatted = "${item.lineNumber}: ${redactSensitiveData(item.text)}\n"
      if (sb.length + formatted.length > maxChars) break
      sb.append(formatted)
    }

    if (sb.isEmpty() && focusLine != null) {
      sb.append("${focusLine.lineNumber}: ${redactSensitiveData(focusLine.text)}")
    }
    return sb.toString().trim()
  }

  private val compilerPathLineRegex =
    Regex("""(?:file://)?([A-Za-z0-9_./\\:-]+\.(?:kt|kts|java|xml|yml|yaml|gradle|properties|json|md|toml)):(\d+)(?::(\d+))?""")
  private val gradleBuildFileRegex =
    Regex("""(?:Build file|Script)\s+'([^']+)'\s+line:\s*(\d+)""")
  private val runnerRepoWorkRegex =
    Regex("""^.*?/work/([^/]+)/\1/(.+)$""")
  private val githubWorkspaceRegex =
    Regex("""^.*?/github/workspace/(.+)$""")
  private val commonProjectAnchorRegex =
    Regex("""^.*/((?:app|src|gradle|\.github|docs)/.+|build\.gradle(?:\.kts)?|settings\.gradle(?:\.kts)?|README\.md)$""")

  fun extractFileLocation(line: String, repoName: String? = null): LogFileLocation? {
    gradleBuildFileRegex.find(line)?.let { match ->
      val rawPath = match.groupValues[1]
      val lineNum = match.groupValues[2].toIntOrNull() ?: return null
      val normalized = normalizeRepoPath(rawPath, repoName)
      if (normalized.isNotBlank()) {
        return LogFileLocation(filePath = normalized, lineNumber = lineNum)
      }
    }

    compilerPathLineRegex.find(line)?.let { match ->
      val rawPath = match.groupValues[1]
      val lineNum = match.groupValues[2].toIntOrNull() ?: return null
      val colNum = match.groupValues[3].toIntOrNull()
      val normalized = normalizeRepoPath(rawPath, repoName)
      if (normalized.isNotBlank() && !normalized.startsWith("http")) {
        return LogFileLocation(filePath = normalized, lineNumber = lineNum, columnNumber = colNum)
      }
    }
    return null
  }

  fun normalizeRepoPath(rawPath: String, repoName: String? = null): String {
    var path =
      rawPath
        .trim()
        .removePrefix("file:///")
        .removePrefix("file://")
        .replace('\\', '/')
    if (rawPath.startsWith("file:///") && !path.startsWith("/")) {
      path = "/$path"
    }

    runnerRepoWorkRegex.find(path)?.let { return it.groupValues[2].trimStart('/') }
    githubWorkspaceRegex.find(path)?.let { return it.groupValues[1].trimStart('/') }

    if (!repoName.isNullOrBlank()) {
      val repoMarker = "/$repoName/"
      val idx = path.lastIndexOf(repoMarker)
      if (idx >= 0) {
        return path.substring(idx + repoMarker.length).trimStart('/')
      }
    }

    commonProjectAnchorRegex.find(path)?.let { return it.groupValues[1].trimStart('/') }

    return path.trimStart('/')
  }
}

data class LogFileLocation(
  val filePath: String,
  val lineNumber: Int,
  val columnNumber: Int? = null,
)
