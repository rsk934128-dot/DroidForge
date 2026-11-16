package com.example.domain

enum class DiffLineType {
  UNCHANGED,
  ADDED,
  REMOVED,
}

data class DiffLine(
  val type: DiffLineType,
  val oldLineNumber: Int?,
  val newLineNumber: Int?,
  val text: String,
)

data class DiffSummary(
  val lines: List<DiffLine>,
  val addedCount: Int,
  val removedCount: Int,
) {
  val hasChanges: Boolean
    get() = addedCount > 0 || removedCount > 0
}

object DiffEngine {

  fun computeLineDiff(original: String, updated: String): DiffSummary {
    val oldLines = if (original.isEmpty()) emptyList() else original.split('\n')
    val newLines = if (updated.isEmpty()) emptyList() else updated.split('\n')

    if (oldLines == newLines) {
      val unchanged =
        oldLines.mapIndexed { idx, line ->
          DiffLine(DiffLineType.UNCHANGED, idx + 1, idx + 1, line)
        }
      return DiffSummary(lines = unchanged, addedCount = 0, removedCount = 0)
    }

    val m = oldLines.size
    val n = newLines.size

    // Guard DP memory for huge files (>1800 lines): trim common prefix/suffix first
    var prefix = 0
    while (prefix < m && prefix < n && oldLines[prefix] == newLines[prefix]) {
      prefix++
    }
    var suffix = 0
    while (
      suffix < (m - prefix) &&
        suffix < (n - prefix) &&
        oldLines[m - 1 - suffix] == newLines[n - 1 - suffix]
    ) {
      suffix++
    }

    val result = ArrayList<DiffLine>(maxOf(m, n) + 8)
    for (i in 0 until prefix) {
      result.add(DiffLine(DiffLineType.UNCHANGED, i + 1, i + 1, oldLines[i]))
    }

    val midOld = oldLines.subList(prefix, m - suffix)
    val midNew = newLines.subList(prefix, n - suffix)
    val midM = midOld.size
    val midN = midNew.size

    var addedCount = 0
    var removedCount = 0

    if (midM * midN <= 400_000) {
      val lcs = Array(midM + 1) { IntArray(midN + 1) }
      for (i in midM - 1 downTo 0) {
        for (j in midN - 1 downTo 0) {
          lcs[i][j] =
            if (midOld[i] == midNew[j]) {
              1 + lcs[i + 1][j + 1]
            } else {
              maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
      }

      var i = 0
      var j = 0
      while (i < midM && j < midN) {
        when {
          midOld[i] == midNew[j] -> {
            result.add(
              DiffLine(
                DiffLineType.UNCHANGED,
                prefix + i + 1,
                prefix + j + 1,
                midOld[i],
              )
            )
            i++
            j++
          }
          lcs[i + 1][j] >= lcs[i][j + 1] -> {
            result.add(DiffLine(DiffLineType.REMOVED, prefix + i + 1, null, midOld[i]))
            removedCount++
            i++
          }
          else -> {
            result.add(DiffLine(DiffLineType.ADDED, null, prefix + j + 1, midNew[j]))
            addedCount++
            j++
          }
        }
      }
      while (i < midM) {
        result.add(DiffLine(DiffLineType.REMOVED, prefix + i + 1, null, midOld[i]))
        removedCount++
        i++
      }
      while (j < midN) {
        result.add(DiffLine(DiffLineType.ADDED, null, prefix + j + 1, midNew[j]))
        addedCount++
        j++
      }
    } else {
      for (i in midOld.indices) {
        result.add(DiffLine(DiffLineType.REMOVED, prefix + i + 1, null, midOld[i]))
        removedCount++
      }
      for (j in midNew.indices) {
        result.add(DiffLine(DiffLineType.ADDED, null, prefix + j + 1, midNew[j]))
        addedCount++
      }
    }

    for (k in 0 until suffix) {
      val oldIdx = m - suffix + k
      val newIdx = n - suffix + k
      result.add(DiffLine(DiffLineType.UNCHANGED, oldIdx + 1, newIdx + 1, oldLines[oldIdx]))
    }

    return DiffSummary(lines = result, addedCount = addedCount, removedCount = removedCount)
  }

  fun replaceLineRange(
    original: String,
    startLineOneIndexed: Int,
    endLineOneIndexed: Int,
    replacementSnippet: String,
  ): String {
    val lines = original.split('\n').toMutableList()
    if (lines.isEmpty()) return replacementSnippet
    val safeStart = (startLineOneIndexed - 1).coerceIn(0, lines.lastIndex)
    val safeEnd = (endLineOneIndexed - 1).coerceIn(safeStart, lines.lastIndex)
    val replacementLines = replacementSnippet.trimEnd('\n').split('\n')
    val before = lines.subList(0, safeStart)
    val after = if (safeEnd + 1 < lines.size) lines.subList(safeEnd + 1, lines.size) else emptyList()
    return (before + replacementLines + after).joinToString("\n")
  }
}
