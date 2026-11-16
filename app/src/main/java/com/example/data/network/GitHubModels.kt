package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubOwnerDto(
  val id: Long = 0L,
  val login: String = "",
  @Json(name = "avatar_url") val avatarUrl: String = "",
  @Json(name = "html_url") val htmlUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubUserDto(
  val id: Long = 0L,
  val login: String = "",
  val name: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String = "",
  @Json(name = "public_repos") val publicRepos: Int = 0,
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
  val id: Long = 0L,
  val name: String = "",
  @Json(name = "full_name") val fullName: String = "",
  val private: Boolean = false,
  val owner: GitHubOwnerDto = GitHubOwnerDto(),
  @Json(name = "html_url") val htmlUrl: String = "",
  val description: String? = null,
  @Json(name = "stargazers_count") val stargazersCount: Int = 0,
  @Json(name = "forks_count") val forksCount: Int = 0,
  val language: String? = null,
  @Json(name = "default_branch") val defaultBranch: String = "main",
  @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubBranchDto(
  val name: String = "",
  @Json(name = "protected") val isProtected: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  val workflows: List<GitHubWorkflowDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowDto(
  val id: Long = 0L,
  val name: String = "",
  val path: String = "",
  val state: String = "",
  @Json(name = "html_url") val htmlUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequest(
  val ref: String,
  val inputs: Map<String, String>? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubRunsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunDto(
  val id: Long = 0L,
  val name: String? = null,
  @Json(name = "run_number") val runNumber: Int = 0,
  val event: String = "",
  val status: String? = null,
  val conclusion: String? = null,
  @Json(name = "head_branch") val headBranch: String? = null,
  @Json(name = "head_sha") val headSha: String = "",
  @Json(name = "html_url") val htmlUrl: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "updated_at") val updatedAt: String = "",
  @Json(name = "run_started_at") val runStartedAt: String? = null,
  val actor: GitHubOwnerDto? = null,
  @Json(name = "display_title") val displayTitle: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubJobsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  val jobs: List<GitHubJobDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubJobDto(
  val id: Long = 0L,
  val name: String = "",
  val status: String = "",
  val conclusion: String? = null,
  @Json(name = "started_at") val startedAt: String? = null,
  @Json(name = "completed_at") val completedAt: String? = null,
  @Json(name = "runner_name") val runnerName: String? = null,
  val steps: List<GitHubStepDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubStepDto(
  val name: String = "",
  val status: String = "",
  val conclusion: String? = null,
  val number: Int = 0,
  @Json(name = "started_at") val startedAt: String? = null,
  @Json(name = "completed_at") val completedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactsResponse(
  @Json(name = "total_count") val totalCount: Int = 0,
  val artifacts: List<GitHubArtifactDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactDto(
  val id: Long = 0L,
  val name: String = "",
  @Json(name = "size_in_bytes") val sizeInBytes: Long = 0L,
  @Json(name = "archive_download_url") val archiveDownloadUrl: String = "",
  val expired: Boolean = false,
  @Json(name = "created_at") val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
  val id: Long = 0L,
  @Json(name = "tag_name") val tagName: String = "",
  val name: String? = null,
  @Json(name = "html_url") val htmlUrl: String = "",
  @Json(name = "published_at") val publishedAt: String? = null,
  val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAssetDto(
  val id: Long = 0L,
  val name: String = "",
  val size: Long = 0L,
  @Json(name = "download_count") val downloadCount: Int = 0,
  @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
  @Json(name = "content_type") val contentType: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitResponse(
  val resources: RateLimitResourcesDto = RateLimitResourcesDto(),
)

@JsonClass(generateAdapter = true)
data class RateLimitResourcesDto(
  val core: RateLimitCoreDto = RateLimitCoreDto(),
)

@JsonClass(generateAdapter = true)
data class RateLimitCoreDto(
  val limit: Int = 60,
  val remaining: Int = 60,
  val reset: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class GitHubFileContentResponse(
  val name: String = "",
  val path: String = "",
  val sha: String = "",
  val size: Long = 0L,
  val encoding: String? = null,
  val content: String? = null,
  @Json(name = "html_url") val htmlUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubPutFileRequest(
  val message: String,
  val content: String,
  val sha: String? = null,
  val branch: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubRefObjectDto(
  val sha: String = "",
  val type: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubRefResponse(
  val ref: String = "",
  @Json(name = "object") val obj: GitHubRefObjectDto = GitHubRefObjectDto(),
)

@JsonClass(generateAdapter = true)
data class GitHubCreateRefRequest(
  val ref: String,
  val sha: String,
)

@JsonClass(generateAdapter = true)
data class GitHubCreatePrRequest(
  val title: String,
  val head: String,
  val base: String,
  val body: String,
)

@JsonClass(generateAdapter = true)
data class GitHubPrBranchRefDto(
  val ref: String = "",
  val sha: String = "",
)

@JsonClass(generateAdapter = true)
data class GitHubPullRequestResponse(
  val id: Long = 0L,
  val number: Int = 0,
  val title: String = "",
  @Json(name = "html_url") val htmlUrl: String = "",
  val state: String = "",
  val draft: Boolean = false,
  val user: GitHubOwnerDto? = null,
  val head: GitHubPrBranchRefDto? = null,
  val base: GitHubPrBranchRefDto? = null,
  @Json(name = "updated_at") val updatedAt: String = "",
)

data class GitHubRepoFile(
  val path: String,
  val content: String,
  val sha: String,
  val branch: String,
  val htmlUrl: String,
)

data class PullRequestOutcome(
  val branchName: String,
  val prNumber: Int,
  val prUrl: String,
  val commitMessage: String,
)
