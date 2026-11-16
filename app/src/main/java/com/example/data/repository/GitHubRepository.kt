package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.auth.GitHubOAuthManager
import com.example.data.auth.GitHubOAuthTokenResponse
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkedRepoEntity
import com.example.data.local.CachedWorkflowRunEntity
import com.example.data.network.GitHubApiService
import com.example.data.network.GitHubArtifactDto
import com.example.data.network.GitHubBranchDto
import com.example.data.network.GitHubJobDto
import com.example.data.network.GitHubPutFileRequest
import com.example.data.network.GitHubReleaseDto
import com.example.data.network.GitHubRepoDto
import com.example.data.network.GitHubUserDto
import com.example.data.network.GitHubWorkflowDto
import com.example.data.network.GitHubWorkflowRunDto
import com.example.data.network.RateLimitCoreDto
import com.example.data.network.WorkflowDispatchRequest
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.prefs.UserPrefs
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed interface GitHubLookupTarget {
  data class UserRepos(val username: String) : GitHubLookupTarget
  data class SingleRepo(val owner: String, val repo: String) : GitHubLookupTarget
}

class GitHubRepository(
  private val appContext: Context,
  val prefsRepo: UserPreferencesRepository,
  private val db: AppDatabase,
) {
  @Volatile private var currentToken: String = ""

  private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder =
      original
        .newBuilder()
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "DroidForge-Android-CICD")
    val token = currentToken.trim()
    if (token.isNotEmpty() && original.url.host == "api.github.com") {
      builder.header("Authorization", "Bearer $token")
    }
    chain.proceed(builder.build())
  }

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .addNetworkInterceptor(authInterceptor)
      .addInterceptor(
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      )
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .build()

  private val moshi: Moshi = Moshi.Builder().build()

  private val api: GitHubApiService =
    Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GitHubApiService::class.java)

  fun updateTokenCache(token: String) {
    currentToken = token.trim()
  }

  val oauthManager = GitHubOAuthManager()

  fun isOAuthConfigured(): Boolean = oauthManager.isOAuthConfigured()

  fun createOAuthAuthorizeUri(): Result<Uri> = oauthManager.createAuthorizationUrl()

  fun isOAuthCallbackUri(uri: Uri?): Boolean = oauthManager.isOAuthCallbackUri(uri)

  suspend fun completeOAuthCallback(uri: Uri): Result<GitHubOAuthTokenResponse> {
    val result = oauthManager.exchangeCallbackForToken(uri)
    result.getOrNull()?.accessToken?.let { token ->
      val scope = result.getOrNull()?.scope.orEmpty()
      updateTokenCache(token)
      prefsRepo.saveOAuthToken(token, scope)
    }
    return result
  }

  private suspend fun ensureTokenSynced() {
    if (currentToken.isEmpty()) {
      currentToken = prefsRepo.prefsFlow.first().githubPat.trim()
    }
  }

  val prefsFlow: Flow<UserPrefs> = prefsRepo.prefsFlow
  val bookmarkedReposFlow: Flow<List<BookmarkedRepoEntity>> = db.bookmarkedRepoDao().observeAll()

  fun observeCachedRuns(repoFullName: String): Flow<List<CachedWorkflowRunEntity>> =
    db.cachedWorkflowRunDao().observeRunsForRepo(repoFullName)

  fun parseTarget(rawInput: String): GitHubLookupTarget? {
    val cleaned =
      rawInput
        .trim()
        .removePrefix("https://github.com/")
        .removePrefix("http://github.com/")
        .removePrefix("github.com/")
        .removeSuffix(".git")
        .trim('/')
    if (cleaned.isEmpty()) return null
    val parts = cleaned.split('/').filter { it.isNotBlank() }
    return when {
      parts.size >= 2 -> GitHubLookupTarget.SingleRepo(parts[0], parts[1])
      parts.size == 1 -> GitHubLookupTarget.UserRepos(parts[0])
      else -> null
    }
  }

  suspend fun fetchAuthenticatedUser(): Result<GitHubUserDto> =
    safeApiCall { api.getAuthenticatedUser() }

  suspend fun fetchRateLimit(): Result<RateLimitCoreDto> =
    safeApiCall { api.getRateLimit() }.map { it.resources.core }

  suspend fun fetchUserRepos(username: String): Result<List<GitHubRepoDto>> =
    safeApiCall { api.getUserRepos(username.trim()) }

  suspend fun fetchMyRepos(): Result<List<GitHubRepoDto>> =
    safeApiCall { api.getMyRepos() }

  suspend fun fetchRepo(owner: String, repo: String): Result<GitHubRepoDto> =
    safeApiCall { api.getRepo(owner.trim(), repo.trim()) }

  suspend fun fetchBranches(owner: String, repo: String): Result<List<GitHubBranchDto>> =
    safeApiCall { api.getBranches(owner, repo) }

  suspend fun fetchWorkflows(owner: String, repo: String): Result<List<GitHubWorkflowDto>> =
    safeApiCall { api.getWorkflows(owner, repo) }.map { it.workflows }

  suspend fun fetchWorkflowRuns(owner: String, repo: String): Result<List<GitHubWorkflowRunDto>> {
    val result = safeApiCall { api.getWorkflowRuns(owner, repo) }.map { it.workflowRuns }
    result.getOrNull()?.let { runs ->
      val fullName = "$owner/$repo"
      val entities =
        runs.map { run ->
          CachedWorkflowRunEntity(
            runId = run.id,
            repoFullName = fullName,
            runNumber = run.runNumber,
            title = run.displayTitle ?: run.name ?: "Workflow #${run.runNumber}",
            status = run.status ?: "unknown",
            conclusion = run.conclusion,
            branch = run.headBranch ?: "main",
            headSha = run.headSha.take(7),
            event = run.event,
            htmlUrl = run.htmlUrl,
            updatedAt = run.updatedAt,
          )
        }
      db.cachedWorkflowRunDao().upsertAll(entities)
    }
    return result
  }

  suspend fun fetchRunJobs(owner: String, repo: String, runId: Long): Result<List<GitHubJobDto>> =
    safeApiCall { api.getWorkflowRunJobs(owner, repo, runId) }.map { it.jobs }

  suspend fun fetchRunArtifacts(
    owner: String,
    repo: String,
    runId: Long,
  ): Result<List<GitHubArtifactDto>> =
    safeApiCall { api.getWorkflowRunArtifacts(owner, repo, runId) }.map { it.artifacts }

  suspend fun fetchReleases(owner: String, repo: String): Result<List<GitHubReleaseDto>> =
    safeApiCall { api.getReleases(owner, repo) }

  suspend fun dispatchWorkflow(
    owner: String,
    repo: String,
    workflowId: Long,
    branch: String,
    buildNote: String?,
  ): Result<Unit> {
    val inputs = buildNote?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("build_note" to it) }
    return safeApiCallUnit {
      api.dispatchWorkflow(
        owner = owner,
        repo = repo,
        workflowId = workflowId,
        request = WorkflowDispatchRequest(ref = branch, inputs = inputs),
      )
    }
  }

  suspend fun dispatchWorkflowByFileName(
    owner: String,
    repo: String,
    workflowFileName: String,
    branch: String,
    buildNote: String?,
  ): Result<Unit> {
    val inputs = buildNote?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("build_note" to it) }
    return safeApiCallUnit {
      api.dispatchWorkflowByFileName(
        owner = owner,
        repo = repo,
        workflowFileName = workflowFileName,
        request = WorkflowDispatchRequest(ref = branch, inputs = inputs),
      )
    }
  }

  suspend fun rerunWorkflow(owner: String, repo: String, runId: Long): Result<Unit> =
    safeApiCallUnit { api.rerunWorkflow(owner, repo, runId) }

  suspend fun cancelWorkflowRun(owner: String, repo: String, runId: Long): Result<Unit> =
    safeApiCallUnit { api.cancelWorkflowRun(owner, repo, runId) }

  suspend fun pushWorkflowYaml(
    owner: String,
    repo: String,
    branch: String,
    yamlContent: String,
  ): Result<Unit> =
    pushTextFile(
      owner = owner,
      repo = repo,
      branch = branch,
      filePath = ".github/workflows/android-apk.yml",
      content = yamlContent,
      commitMessage = "ci: add Android APK build workflow via DroidForge",
    )

  suspend fun pushTextFile(
    owner: String,
    repo: String,
    branch: String,
    filePath: String,
    content: String,
    commitMessage: String,
  ): Result<Unit> {
    ensureTokenSynced()
    if (currentToken.isBlank()) {
      return Result.failure(IllegalStateException("A GitHub Personal Access Token is required to push files."))
    }
    val existingSha =
      try {
        val resp = api.getFileContent(owner, repo, filePath, branch)
        if (resp.isSuccessful) resp.body()?.sha else null
      } catch (_: Exception) {
        null
      }
    val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    return safeApiCallUnit {
      api.putFileContent(
        owner = owner,
        repo = repo,
        path = filePath,
        body =
          GitHubPutFileRequest(
            message = commitMessage,
            content = encoded,
            sha = existingSha,
            branch = branch,
          ),
      )
    }
  }

  suspend fun downloadBinaryToUri(url: String, destinationUri: Uri): Result<Long> =
    withContext(Dispatchers.IO) {
      try {
        ensureTokenSynced()
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            return@withContext Result.failure(
              IOException("Download failed with HTTP ${response.code}")
            )
          }
          val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
          var totalBytes = 0L
          appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
            body.byteStream().use { input ->
              val buffer = ByteArray(8192)
              while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
                totalBytes += read
              }
            }
          } ?: return@withContext Result.failure(IOException("Unable to open destination file"))
          Result.success(totalBytes)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun toggleBookmark(repo: GitHubRepoDto): Boolean {
    val existing = db.bookmarkedRepoDao().observeIsBookmarked(repo.fullName).first()
    return if (existing) {
      db.bookmarkedRepoDao().deleteByFullName(repo.fullName)
      false
    } else {
      db.bookmarkedRepoDao()
        .upsert(
          BookmarkedRepoEntity(
            fullName = repo.fullName,
            ownerLogin = repo.owner.login,
            name = repo.name,
            avatarUrl = repo.owner.avatarUrl,
            description = repo.description ?: "",
            defaultBranch = repo.defaultBranch,
            language = repo.language ?: "Kotlin",
            stars = repo.stargazersCount,
            isPrivate = repo.private,
            htmlUrl = repo.htmlUrl,
          )
        )
      true
    }
  }

  suspend fun bookmarkRepo(repo: GitHubRepoDto) {
    db.bookmarkedRepoDao()
      .upsert(
        BookmarkedRepoEntity(
          fullName = repo.fullName,
          ownerLogin = repo.owner.login,
          name = repo.name,
          avatarUrl = repo.owner.avatarUrl,
          description = repo.description ?: "",
          defaultBranch = repo.defaultBranch,
          language = repo.language ?: "Kotlin",
          stars = repo.stargazersCount,
          isPrivate = repo.private,
          htmlUrl = repo.htmlUrl,
        )
      )
  }

  suspend fun removeBookmark(fullName: String) {
    db.bookmarkedRepoDao().deleteByFullName(fullName)
  }

  private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> =
    withContext(Dispatchers.IO) {
      try {
        ensureTokenSynced()
        val response = call()
        if (response.isSuccessful) {
          val body = response.body()
          if (body != null) {
            Result.success(body)
          } else {
            Result.failure(IOException("Empty response from GitHub API"))
          }
        } else {
          Result.failure(IOException(mapHttpError(response.code(), response.message())))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private suspend fun safeApiCallUnit(call: suspend () -> Response<Unit>): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        ensureTokenSynced()
        val response = call()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(IOException(mapHttpError(response.code(), response.message())))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun mapHttpError(code: Int, message: String): String =
    when (code) {
      401 -> "Authentication failed (401). Check your GitHub PAT in Settings."
      403 -> "Forbidden or API rate limit exceeded (403). Add a GitHub PAT with repo + workflow scopes."
      404 -> "Not found on GitHub (404). Verify repository name or access permissions."
      422 -> "Workflow cannot be dispatched (422). Ensure workflow_dispatch trigger exists on that branch."
      else -> "GitHub API error ($code): $message"
    }
}
