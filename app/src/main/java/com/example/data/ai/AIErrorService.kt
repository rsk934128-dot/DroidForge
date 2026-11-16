package com.example.data.ai

import android.util.Log
import com.example.ui.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AIErrorService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val INFERENCE_TIMEOUT_MS = 60_000L

data class AiErrorDiagnosis(
  val summary: String,
  val rootCause: String,
  val fixSteps: List<String>,
  val patchSnippet: String,
  val focusLineNumber: Int? = null,
)

data class AiFileFixSuggestion(
  val explanation: String,
  val startLine: Int,
  val endLine: Int,
  val replacementCode: String,
)

sealed interface AiDiagnosisUiState {
  data object Idle : AiDiagnosisUiState

  data class Loading(val focusLineNumber: Int? = null) : AiDiagnosisUiState

  data class Success(val diagnosis: AiErrorDiagnosis) : AiDiagnosisUiState

  data class Error(
    val message: String,
    val focusLineNumber: Int? = null,
  ) : AiDiagnosisUiState
}

class AIErrorService {

  private val diagnosisSchema =
    Schema.obj(
      properties =
        mapOf(
          "summary" to
            Schema.string(
              description = "Clear plain-language explanation of what failed in the Android/Gradle build."
            ),
          "rootCause" to
            Schema.string(
              description = "Concise technical root cause (e.g., unresolved symbol, JDK mismatch, Gradle config error)."
            ),
          "fixSteps" to
            Schema.array(
              items = Schema.string(),
              description = "Ordered actionable steps for the developer to fix the error.",
            ),
          "patchSnippet" to
            Schema.string(
              description = "Ready-to-copy Kotlin, Gradle, or YAML code snippet that resolves the issue (empty string if no code edit needed)."
            ),
        ),
      optionalProperties = listOf("patchSnippet"),
    )

  private val fileFixSchema =
    Schema.obj(
      properties =
        mapOf(
          "explanation" to
            Schema.string(
              description = "Brief explanation of the exact change being made in the file."
            ),
          "startLine" to
            Schema.integer(
              description = "1-indexed start line number in the file to replace."
            ),
          "endLine" to
            Schema.integer(
              description = "1-indexed inclusive end line number in the file to replace."
            ),
          "replacementCode" to
            Schema.string(
              description = "Pure replacement code for lines startLine..endLine preserving exact indentation, without line numbers or markdown fences."
            ),
        ),
    )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = diagnosisSchema
        },
    )
  }

  private val fileFixModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = fileFixSchema
        },
    )
  }

  suspend fun analyzeBuildError(
    jobName: String,
    sanitizedLogContext: String,
    language: AppLanguage,
    focusLineNumber: Int? = null,
  ): AiErrorDiagnosis {
    val languageInstruction =
      if (language == AppLanguage.BENGALI) {
        "Respond in natural, clear Bengali (বাংলা) for summary, rootCause, and fixSteps, keeping technical identifiers, Gradle tasks, and code in English. Keep patchSnippet as pure valid code/config."
      } else {
        "Respond in clear, concise English. Keep patchSnippet as pure valid code/config without markdown fences."
      }

    val focusNote =
      if (focusLineNumber != null) {
        "Focus primarily on error line $focusLineNumber and explain how the surrounding log lines lead to it."
      } else {
        "Identify the primary failure causing the job to fail."
      }

    val prompt =
      """
      You are an expert Android, Kotlin, Gradle, and GitHub Actions CI/CD build debugger for DroidForge.
      Analyze the sanitized build log snippet below from job "$jobName".
      $focusNote
      $languageInstruction
      Never ask for or invent secrets or tokens.

      Sanitized Build Log:
      $sanitizedLogContext
      """.trimIndent()

    try {
      val response = withTimeout(INFERENCE_TIMEOUT_MS) { model.generateContent(prompt) }
      val jsonText =
        response.text?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("Gemini returned an empty response.")

      val json = JSONObject(jsonText)
      val summary = json.optString("summary").trim()
      val rootCause = json.optString("rootCause").trim()
      val stepsArray = json.optJSONArray("fixSteps")
      val fixSteps =
        if (stepsArray != null) {
          (0 until stepsArray.length())
            .mapNotNull { idx -> stepsArray.optString(idx)?.trim()?.takeIf { it.isNotEmpty() } }
        } else {
          emptyList()
        }
      val patchSnippet = json.optString("patchSnippet", "").trim()

      if (summary.isBlank() && rootCause.isBlank()) {
        throw IllegalStateException("AI response did not include diagnostic details.")
      }

      return AiErrorDiagnosis(
        summary = summary.ifBlank { rootCause },
        rootCause = rootCause.ifBlank { summary },
        fixSteps = fixSteps,
        patchSnippet = patchSnippet,
        focusLineNumber = focusLineNumber,
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI error analysis timed out", e)
      throw IllegalStateException("AI analysis timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI error analysis failed", e)
      throw e
    }
  }

  suspend fun suggestFileFix(
    filePath: String,
    targetLine: Int,
    numberedFileExcerpt: String,
    errorText: String,
    language: AppLanguage,
  ): AiFileFixSuggestion {
    val languageInstruction =
      if (language == AppLanguage.BENGALI) {
        "Write the explanation in clear Bengali (বাংলা), while keeping replacementCode as valid pure source code."
      } else {
        "Write the explanation in clear, concise English."
      }

    val prompt =
      """
      You are an Android, Kotlin, Gradle, and CI/CD engineer assisting in DroidForge's code editor.
      Target file: $filePath
      Reported error around line $targetLine:
      $errorText

      Numbered excerpt from the actual file:
      $numberedFileExcerpt

      Propose the minimal accurate replacement for lines startLine..endLine that fixes this error.
      $languageInstruction
      Return pure replacementCode with exact line indentation and NO line number prefixes or markdown fences.
      """.trimIndent()

    try {
      val response = withTimeout(INFERENCE_TIMEOUT_MS) { fileFixModel.generateContent(prompt) }
      val jsonText =
        response.text?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("Gemini returned an empty file fix response.")

      val json = JSONObject(jsonText)
      val explanation = json.optString("explanation").trim()
      val startLine = json.optInt("startLine", targetLine).coerceAtLeast(1)
      val endLine = json.optInt("endLine", startLine).coerceAtLeast(startLine)
      val replacementCode = json.optString("replacementCode", "")

      if (replacementCode.isEmpty() && explanation.isBlank()) {
        throw IllegalStateException("AI did not return a valid code fix.")
      }

      return AiFileFixSuggestion(
        explanation = explanation.ifBlank { "Applied AI fix around line $startLine" },
        startLine = startLine,
        endLine = endLine,
        replacementCode = replacementCode,
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI file fix timed out", e)
      throw IllegalStateException("AI file fix timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI file fix failed", e)
      throw e
    }
  }
}
