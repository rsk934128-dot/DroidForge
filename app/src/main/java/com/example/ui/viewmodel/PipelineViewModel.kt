package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.network.GitHubArtifactDto
import com.example.data.network.GitHubBranchDto
import com.example.data.network.GitHubJobDto
import com.example.data.network.GitHubReleaseDto
import com.example.data.network.GitHubWorkflowDto
import com.example.data.network.GitHubWorkflowRunDto
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PipelineUiState(
  val repoFullName: String = "",
  val isLoading: Boolean = false,
  val isDispatching: Boolean = false,
  val workflows: List<GitHubWorkflowDto> = emptyList(),
  val branches: List<GitHubBranchDto> = emptyList(),
  val runs: List<GitHubWorkflowRunDto> = emptyList(),
  val releases: List<GitHubReleaseDto> = emptyList(),
  val errorMessage: String? = null,
)

data class RunDetailUiState(
  val run: GitHubWorkflowRunDto? = null,
  val isLoading: Boolean = false,
  val isDownloading: Boolean = false,
  val jobs: List<GitHubJobDto> = emptyList(),
  val artifacts: List<GitHubArtifactDto> = emptyList(),
  val errorMessage: String? = null,
)

class PipelineViewModel(private val repo: GitHubRepository) : ViewModel() {

  private val _pipelineState = MutableStateFlow(PipelineUiState())
  val pipelineState: StateFlow<PipelineUiState> = _pipelineState.asStateFlow()

  private val _runDetailState = MutableStateFlow(RunDetailUiState())
  val runDetailState: StateFlow<RunDetailUiState> = _runDetailState.asStateFlow()

  private val _events = MutableSharedFlow<String>()
  val events = _events.asSharedFlow()

  private var pollingJob: Job? = null

  fun loadPipeline(repoFullName: String, showSpinner: Boolean = true) {
    val parts = repoFullName.split('/')
    if (parts.size != 2) return
    val (owner, name) = parts
    _pipelineState.update {
      it.copy(
        repoFullName = repoFullName,
        isLoading = showSpinner,
        errorMessage = null,
      )
    }

    viewModelScope.launch {
      val workflowsRes = repo.fetchWorkflows(owner, name)
      val branchesRes = repo.fetchBranches(owner, name)
      val runsRes = repo.fetchWorkflowRuns(owner, name)
      val releasesRes = repo.fetchReleases(owner, name)

      val error =
        runsRes.exceptionOrNull()?.message
          ?: workflowsRes.exceptionOrNull()?.message

      val runs = runsRes.getOrElse { emptyList() }
      _pipelineState.update {
        it.copy(
          isLoading = false,
          workflows = workflowsRes.getOrElse { emptyList() },
          branches = branchesRes.getOrElse { emptyList() },
          runs = runs,
          releases = releasesRes.getOrElse { emptyList() },
          errorMessage = error,
        )
      }

      if (runs.any { it.status == "queued" || it.status == "in_progress" }) {
        startAutoPolling(repoFullName)
      } else {
        pollingJob?.cancel()
      }
    }
  }

  private fun startAutoPolling(repoFullName: String) {
    if (pollingJob?.isActive == true) return
    pollingJob =
      viewModelScope.launch {
        while (isActive) {
          delay(8_000)
          if (_pipelineState.value.repoFullName != repoFullName) break
          val parts = repoFullName.split('/')
          if (parts.size != 2) break
          val runs = repo.fetchWorkflowRuns(parts[0], parts[1]).getOrNull() ?: continue
          _pipelineState.update { it.copy(runs = runs) }
          val activeDetailRunId = _runDetailState.value.run?.id
          if (activeDetailRunId != null) {
            runs.firstOrNull { it.id == activeDetailRunId }?.let { updatedRun ->
              inspectRun(repoFullName, updatedRun, showSpinner = false)
            }
          }
          if (runs.none { it.status == "queued" || it.status == "in_progress" }) {
            break
          }
        }
      }
  }

  fun triggerWorkflow(
    workflowId: Long,
    branch: String,
    buildNote: String?,
    successMsg: String,
    onDone: () -> Unit,
  ) {
    val fullName = _pipelineState.value.repoFullName
    val parts = fullName.split('/')
    if (parts.size != 2) return
    viewModelScope.launch {
      _pipelineState.update { it.copy(isDispatching = true) }
      repo
        .dispatchWorkflow(parts[0], parts[1], workflowId, branch, buildNote)
        .onSuccess {
          _pipelineState.update { it.copy(isDispatching = false) }
          _events.emit(successMsg)
          onDone()
          delay(1_800)
          loadPipeline(fullName, showSpinner = false)
        }
        .onFailure { err ->
          _pipelineState.update { it.copy(isDispatching = false) }
          _events.emit(err.message ?: "Failed to dispatch workflow.")
        }
    }
  }

  fun inspectRun(repoFullName: String, run: GitHubWorkflowRunDto, showSpinner: Boolean = true) {
    val parts = repoFullName.split('/')
    if (parts.size != 2) return
    _runDetailState.update {
      it.copy(run = run, isLoading = showSpinner, errorMessage = null)
    }
    viewModelScope.launch {
      val jobsRes = repo.fetchRunJobs(parts[0], parts[1], run.id)
      val artifactsRes = repo.fetchRunArtifacts(parts[0], parts[1], run.id)
      _runDetailState.update {
        it.copy(
          isLoading = false,
          jobs = jobsRes.getOrElse { emptyList() },
          artifacts = artifactsRes.getOrElse { emptyList() },
          errorMessage = jobsRes.exceptionOrNull()?.message,
        )
      }
    }
  }

  fun rerunCurrentRun() {
    val fullName = _pipelineState.value.repoFullName
    val runId = _runDetailState.value.run?.id ?: return
    val parts = fullName.split('/')
    if (parts.size != 2) return
    viewModelScope.launch {
      repo
        .rerunWorkflow(parts[0], parts[1], runId)
        .onSuccess {
          _events.emit("Re-run queued on GitHub Actions")
          delay(1_500)
          loadPipeline(fullName, showSpinner = false)
        }
        .onFailure { err -> _events.emit(err.message ?: "Re-run failed.") }
    }
  }

  fun cancelCurrentRun() {
    val fullName = _pipelineState.value.repoFullName
    val runId = _runDetailState.value.run?.id ?: return
    val parts = fullName.split('/')
    if (parts.size != 2) return
    viewModelScope.launch {
      repo
        .cancelWorkflowRun(parts[0], parts[1], runId)
        .onSuccess {
          _events.emit("Cancellation requested")
          delay(1_200)
          loadPipeline(fullName, showSpinner = false)
        }
        .onFailure { err -> _events.emit(err.message ?: "Cancel failed.") }
    }
  }

  fun downloadToUri(url: String, destinationUri: Uri, fileName: String) {
    viewModelScope.launch {
      _runDetailState.update { it.copy(isDownloading = true) }
      repo
        .downloadBinaryToUri(url, destinationUri)
        .onSuccess { bytes ->
          _runDetailState.update { it.copy(isDownloading = false) }
          val mb = String.format("%.2f MB", bytes / (1024.0 * 1024.0))
          _events.emit("Saved $fileName ($mb)")
        }
        .onFailure { err ->
          _runDetailState.update { it.copy(isDownloading = false) }
          _events.emit(err.message ?: "Download failed.")
        }
    }
  }

  fun clearRunDetail() {
    _runDetailState.value = RunDetailUiState()
  }

  override fun onCleared() {
    super.onCleared()
    pollingJob?.cancel()
  }

  class Factory(private val repo: GitHubRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PipelineViewModel(repo) as T
  }
}
