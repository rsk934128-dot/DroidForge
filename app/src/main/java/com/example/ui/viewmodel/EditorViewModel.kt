package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AIErrorService
import com.example.data.local.EditorDraftEntity
import com.example.data.network.PullRequestOutcome
import com.example.data.repository.GitHubRepository
import com.example.domain.DiffEngine
import com.example.domain.DiffSummary
import com.example.domain.JobLogParser
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
  val repoFullName: String = "",
  val baseBranch: String = "main",
  val filePath: String = "",
  val targetLineNumber: Int = 1,
  val errorText: String = "",
  val aiPatchHint: String? = null,
  val isLoadingFile: Boolean = false,
  val originalContent: String = "",
  val editedContent: String = "",
  val fileHtmlUrl: String = "",
  val loadErrorMessage: String? = null,
  val isGeneratingAiFix: Boolean = false,
  val aiExplanation: String? = null,
  val aiFixError: String? = null,
  val diffSummary: DiffSummary = DiffSummary(emptyList(), 0, 0),
  val newBranchName: String = "",
  val commitMessage: String = "",
  val prTitle: String = "",
  val prBody: String = "",
  val isCreatingPr: Boolean = false,
  val prErrorMessage: String? = null,
  val prOutcome: PullRequestOutcome? = null,
  val isDraftRestored: Boolean = false,
  val isSavingDraft: Boolean = false,
  val lastDraftSavedAt: Long? = null,
  val isOfflineDraftMode: Boolean = false,
)

class EditorViewModel(
  private val repo: GitHubRepository,
  private val aiErrorService: AIErrorService = AIErrorService(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditorUiState())
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private var autoSaveJob: Job? = null

  fun openFile(
    repoFullName: String,
    baseBranch: String,
    filePath: String,
    targetLineNumber: Int,
    errorText: String,
    aiPatchHint: String? = null,
  ) {
    autoSaveJob?.cancel()
    val normalizedBranch = baseBranch.trim().ifBlank { "main" }
    val normalizedPath = filePath.trimStart('/')
    val cleanFileName = normalizedPath.substringAfterLast('/').substringBefore('.').lowercase()
      .replace(Regex("[^a-z0-9-]"), "-")
      .ifBlank { "patch" }
    val stamp = (System.currentTimeMillis() / 1000L) % 100_000L
    val suggestedBranch = "droidforge/fix-$cleanFileName-l$targetLineNumber-$stamp"
    val cleanError = JobLogParser.redactSensitiveData(errorText)

    _uiState.value =
      EditorUiState(
        repoFullName = repoFullName,
        baseBranch = normalizedBranch,
        filePath = normalizedPath,
        targetLineNumber = targetLineNumber.coerceAtLeast(1),
        errorText = cleanError,
        aiPatchHint = aiPatchHint?.takeIf { it.isNotBlank() },
        isLoadingFile = true,
        newBranchName = suggestedBranch,
        commitMessage = "fix: resolve build error at ${normalizedPath.substringAfterLast('/')}:$targetLineNumber",
        prTitle = "fix: resolve CI error in ${normalizedPath.substringAfterLast('/')} (L$targetLineNumber)",
        prBody =
          buildString {
            appendLine("## DroidForge AI-Assisted Fix")
            appendLine()
            appendLine("- **File:** `$normalizedPath` (Line `$targetLineNumber`)")
            if (cleanError.isNotBlank()) {
              appendLine("- **Reported Error:** `$cleanError`")
            }
            appendLine()
            appendLine("Reviewed and submitted via DroidForge Mobile CI/CD IDE.")
          },
      )

    val parts = repoFullName.split('/')
    if (parts.size != 2) {
      _uiState.update {
        it.copy(isLoadingFile = false, loadErrorMessage = "Invalid repository name: $repoFullName")
      }
      return
    }

    viewModelScope.launch {
      val draftKey = GitHubRepository.draftKeyFor(repoFullName, normalizedBranch, normalizedPath)
      val savedDraft = repo.getDraft(draftKey)

      repo
        .fetchFileContent(parts[0], parts[1], normalizedPath, normalizedBranch)
        .onSuccess { repoFile ->
          val shouldRestoreDraft =
            savedDraft != null && savedDraft.editedContent != repoFile.content
          val effectiveEdited =
            if (shouldRestoreDraft) savedDraft!!.editedContent else repoFile.content
          _uiState.update {
            it.copy(
              isLoadingFile = false,
              filePath = repoFile.path,
              originalContent = repoFile.content,
              editedContent = effectiveEdited,
              fileHtmlUrl = repoFile.htmlUrl,
              diffSummary = DiffEngine.computeLineDiff(repoFile.content, effectiveEdited),
              loadErrorMessage = null,
              isDraftRestored = shouldRestoreDraft,
              lastDraftSavedAt = if (shouldRestoreDraft) savedDraft?.updatedAt else null,
              newBranchName =
                if (shouldRestoreDraft && savedDraft!!.newBranchName.isNotBlank()) {
                  savedDraft.newBranchName
                } else {
                  it.newBranchName
                },
              commitMessage =
                if (shouldRestoreDraft && savedDraft!!.commitMessage.isNotBlank()) {
                  savedDraft.commitMessage
                } else {
                  it.commitMessage
                },
              prTitle =
                if (shouldRestoreDraft && savedDraft!!.prTitle.isNotBlank()) {
                  savedDraft.prTitle
                } else {
                  it.prTitle
                },
              prBody =
                if (shouldRestoreDraft && savedDraft!!.prBody.isNotBlank()) {
                  savedDraft.prBody
                } else {
                  it.prBody
                },
              isOfflineDraftMode = false,
            )
          }
        }
        .onFailure { err ->
          if (savedDraft != null) {
            _uiState.update {
              it.copy(
                isLoadingFile = false,
                filePath = savedDraft.filePath,
                targetLineNumber = savedDraft.targetLineNumber.coerceAtLeast(1),
                errorText = savedDraft.errorText.ifBlank { it.errorText },
                originalContent = savedDraft.originalContent,
                editedContent = savedDraft.editedContent,
                diffSummary = DiffEngine.computeLineDiff(savedDraft.originalContent, savedDraft.editedContent),
                newBranchName = savedDraft.newBranchName.ifBlank { it.newBranchName },
                commitMessage = savedDraft.commitMessage.ifBlank { it.commitMessage },
                prTitle = savedDraft.prTitle.ifBlank { it.prTitle },
                prBody = savedDraft.prBody.ifBlank { it.prBody },
                loadErrorMessage = null,
                isDraftRestored = true,
                lastDraftSavedAt = savedDraft.updatedAt,
                isOfflineDraftMode = true,
              )
            }
          } else {
            _uiState.update {
              it.copy(
                isLoadingFile = false,
                loadErrorMessage = err.message ?: "Unable to fetch file from GitHub.",
              )
            }
          }
        }
    }
  }

  fun resumeSavedDraft(draft: EditorDraftEntity) {
    autoSaveJob?.cancel()
    _uiState.value =
      EditorUiState(
        repoFullName = draft.repoFullName,
        baseBranch = draft.baseBranch.ifBlank { "main" },
        filePath = draft.filePath,
        targetLineNumber = draft.targetLineNumber.coerceAtLeast(1),
        errorText = draft.errorText,
        isLoadingFile = false,
        originalContent = draft.originalContent,
        editedContent = draft.editedContent,
        diffSummary = DiffEngine.computeLineDiff(draft.originalContent, draft.editedContent),
        newBranchName = draft.newBranchName,
        commitMessage = draft.commitMessage,
        prTitle = draft.prTitle,
        prBody = draft.prBody,
        isDraftRestored = true,
        lastDraftSavedAt = draft.updatedAt,
        isOfflineDraftMode = false,
      )

    val parts = draft.repoFullName.split('/')
    if (parts.size == 2) {
      viewModelScope.launch {
        repo
          .fetchFileContent(parts[0], parts[1], draft.filePath, draft.baseBranch)
          .onSuccess { repoFile ->
            _uiState.update { current ->
              if (current.repoFullName == draft.repoFullName && current.filePath == draft.filePath) {
                current.copy(fileHtmlUrl = repoFile.htmlUrl, isOfflineDraftMode = false)
              } else {
                current
              }
            }
          }
          .onFailure {
            _uiState.update { current ->
              if (current.repoFullName == draft.repoFullName && current.filePath == draft.filePath) {
                current.copy(isOfflineDraftMode = true)
              } else {
                current
              }
            }
          }
      }
    }
  }

  fun retryLoadFile() {
    val s = _uiState.value
    if (s.repoFullName.isBlank() || s.filePath.isBlank()) return
    openFile(
      repoFullName = s.repoFullName,
      baseBranch = s.baseBranch,
      filePath = s.filePath,
      targetLineNumber = s.targetLineNumber,
      errorText = s.errorText,
      aiPatchHint = s.aiPatchHint,
    )
  }

  fun updateEditedContent(newContent: String) {
    _uiState.update { state ->
      state.copy(
        editedContent = newContent,
        diffSummary = DiffEngine.computeLineDiff(state.originalContent, newContent),
        prOutcome = null,
        prErrorMessage = null,
      )
    }
    scheduleDraftAutoSave()
  }

  fun resetChanges() {
    autoSaveJob?.cancel()
    val state = _uiState.value
    val draftKey =
      if (state.repoFullName.isNotBlank() && state.filePath.isNotBlank()) {
        GitHubRepository.draftKeyFor(state.repoFullName, state.baseBranch, state.filePath)
      } else {
        null
      }
    _uiState.update { current ->
      current.copy(
        editedContent = current.originalContent,
        diffSummary = DiffEngine.computeLineDiff(current.originalContent, current.originalContent),
        aiExplanation = null,
        aiFixError = null,
        prErrorMessage = null,
        prOutcome = null,
        isDraftRestored = false,
        isSavingDraft = false,
        lastDraftSavedAt = null,
      )
    }
    if (draftKey != null) {
      viewModelScope.launch { repo.deleteDraft(draftKey) }
    }
  }

  fun generateAiFilePatch(language: AppLanguage) {
    val state = _uiState.value
    if (state.editedContent.isBlank()) return

    val lines = state.editedContent.split('\n')
    val focusIdx = (state.targetLineNumber - 1).coerceIn(0, lines.lastIndex)
    val startIdx = maxOf(0, focusIdx - 25)
    val endIdx = minOf(lines.lastIndex, focusIdx + 25)
    val numberedExcerpt =
      (startIdx..endIdx).joinToString("\n") { idx ->
        "${idx + 1}: ${JobLogParser.redactSensitiveData(lines[idx])}"
      }

    _uiState.update { it.copy(isGeneratingAiFix = true, aiFixError = null, aiExplanation = null) }

    viewModelScope.launch {
      try {
        val suggestion =
          aiErrorService.suggestFileFix(
            filePath = state.filePath,
            targetLine = state.targetLineNumber,
            numberedFileExcerpt = numberedExcerpt,
            errorText = state.errorText,
            language = language,
          )
        val patched =
          DiffEngine.replaceLineRange(
            original = state.editedContent,
            startLineOneIndexed = suggestion.startLine,
            endLineOneIndexed = suggestion.endLine,
            replacementSnippet = suggestion.replacementCode,
          )
        _uiState.update { current ->
          current.copy(
            isGeneratingAiFix = false,
            editedContent = patched,
            diffSummary = DiffEngine.computeLineDiff(current.originalContent, patched),
            aiExplanation = suggestion.explanation,
            aiFixError = null,
            prOutcome = null,
          )
        }
        scheduleDraftAutoSave()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("EditorViewModel", "AI file fix failed", e)
        _uiState.update {
          it.copy(
            isGeneratingAiFix = false,
            aiFixError = e.localizedMessage ?: "Failed to generate AI file fix.",
          )
        }
      }
    }
  }

  fun updateNewBranchName(value: String) {
    _uiState.update { it.copy(newBranchName = value, prErrorMessage = null) }
    scheduleDraftAutoSave()
  }

  fun updateCommitMessage(value: String) {
    _uiState.update { it.copy(commitMessage = value, prErrorMessage = null) }
    scheduleDraftAutoSave()
  }

  fun updatePrTitle(value: String) {
    _uiState.update { it.copy(prTitle = value, prErrorMessage = null) }
    scheduleDraftAutoSave()
  }

  fun updatePrBody(value: String) {
    _uiState.update { it.copy(prBody = value, prErrorMessage = null) }
    scheduleDraftAutoSave()
  }

  private fun scheduleDraftAutoSave() {
    autoSaveJob?.cancel()
    autoSaveJob =
      viewModelScope.launch {
        delay(1_200)
        val state = _uiState.value
        if (state.repoFullName.isBlank() || state.filePath.isBlank()) return@launch
        val draftKey = GitHubRepository.draftKeyFor(state.repoFullName, state.baseBranch, state.filePath)
        if (!state.diffSummary.hasChanges) {
          repo.deleteDraft(draftKey)
          _uiState.update { it.copy(isSavingDraft = false, lastDraftSavedAt = null, isDraftRestored = false) }
          return@launch
        }
        _uiState.update { it.copy(isSavingDraft = true) }
        val now = System.currentTimeMillis()
        repo.saveDraft(
          EditorDraftEntity(
            draftKey = draftKey,
            repoFullName = state.repoFullName,
            baseBranch = state.baseBranch,
            filePath = state.filePath,
            targetLineNumber = state.targetLineNumber,
            errorText = state.errorText,
            originalContent = state.originalContent,
            editedContent = state.editedContent,
            newBranchName = state.newBranchName,
            commitMessage = state.commitMessage,
            prTitle = state.prTitle,
            prBody = state.prBody,
            updatedAt = now,
          )
        )
        _uiState.update { it.copy(isSavingDraft = false, lastDraftSavedAt = now) }
      }
  }

  fun createPullRequest() {
    val state = _uiState.value
    if (!state.diffSummary.hasChanges) {
      _uiState.update { it.copy(prErrorMessage = "No changes to commit yet.") }
      return
    }
    val parts = state.repoFullName.split('/')
    if (parts.size != 2) return

    autoSaveJob?.cancel()
    _uiState.update { it.copy(isCreatingPr = true, prErrorMessage = null, prOutcome = null) }
    viewModelScope.launch {
      repo
        .pushFixAndCreatePR(
          owner = parts[0],
          repo = parts[1],
          baseBranch = state.baseBranch,
          newBranchName = state.newBranchName,
          filePath = state.filePath,
          updatedContent = state.editedContent,
          commitMessage = state.commitMessage,
          prTitle = state.prTitle,
          prBody = state.prBody,
        )
        .onSuccess { outcome ->
          val draftKey = GitHubRepository.draftKeyFor(state.repoFullName, state.baseBranch, state.filePath)
          repo.deleteDraft(draftKey)
          _uiState.update {
            it.copy(
              isCreatingPr = false,
              prOutcome = outcome,
              prErrorMessage = null,
              isDraftRestored = false,
              isSavingDraft = false,
              lastDraftSavedAt = null,
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isCreatingPr = false,
              prErrorMessage = err.message ?: "Failed to create Pull Request.",
            )
          }
        }
    }
  }

  class Factory(
    private val repo: GitHubRepository,
    private val aiErrorService: AIErrorService = AIErrorService(),
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      EditorViewModel(repo, aiErrorService) as T
  }
}
