package com.example.data.auth

import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class GitHubOAuthTokenResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  val scope: String? = null,
  val error: String? = null,
  @Json(name = "error_description") val errorDescription: String? = null,
)

data class PendingOAuthSession(
  val state: String,
  val codeVerifier: String,
)

class GitHubOAuthManager(
  private val clientIdProvider: () -> String = { BuildConfig.GITHUB_CLIENT_ID },
  private val clientSecretProvider: () -> String = { BuildConfig.GITHUB_CLIENT_SECRET },
) {
  companion object {
    const val REDIRECT_URI = "droidforge://oauth/callback"
    const val SCOPES = "repo workflow read:user"
    private const val PLACEHOLDER_ID = "YOUR_GITHUB_OAUTH_CLIENT_ID"
    private const val PLACEHOLDER_SECRET = "YOUR_GITHUB_OAUTH_CLIENT_SECRET"
  }

  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()

  private val moshi = Moshi.Builder().build()
  private val tokenAdapter = moshi.adapter(GitHubOAuthTokenResponse::class.java)

  @Volatile private var pendingSession: PendingOAuthSession? = null

  fun isOAuthConfigured(): Boolean {
    val id = clientIdProvider().trim()
    return id.isNotEmpty() && id != PLACEHOLDER_ID
  }

  fun createAuthorizationUrl(): Result<Uri> {
    val clientId = clientIdProvider().trim()
    if (clientId.isEmpty() || clientId == PLACEHOLDER_ID) {
      return Result.failure(
        IllegalStateException(
          "GitHub OAuth Client ID is not configured. Set GITHUB_CLIENT_ID in the Secrets panel (callback: $REDIRECT_URI)."
        )
      )
    }
    val state = generateRandomUrlSafeString(24)
    val codeVerifier = generateRandomUrlSafeString(48)
    val codeChallenge = computeS256Challenge(codeVerifier)
    pendingSession = PendingOAuthSession(state = state, codeVerifier = codeVerifier)

    val uri =
      Uri.parse("https://github.com/login/oauth/authorize")
        .buildUpon()
        .appendQueryParameter("client_id", clientId)
        .appendQueryParameter("redirect_uri", REDIRECT_URI)
        .appendQueryParameter("scope", SCOPES)
        .appendQueryParameter("state", state)
        .appendQueryParameter("code_challenge", codeChallenge)
        .appendQueryParameter("code_challenge_method", "S256")
        .build()

    return Result.success(uri)
  }

  fun isOAuthCallbackUri(uri: Uri?): Boolean =
    uri != null &&
      uri.scheme == "droidforge" &&
      uri.host == "oauth" &&
      uri.path?.startsWith("/callback") == true

  suspend fun exchangeCallbackForToken(callbackUri: Uri): Result<GitHubOAuthTokenResponse> {
    val error = callbackUri.getQueryParameter("error")
    if (!error.isNullOrBlank()) {
      val desc = callbackUri.getQueryParameter("error_description") ?: error
      pendingSession = null
      return Result.failure(IllegalStateException("GitHub OAuth cancelled: $desc"))
    }

    val code = callbackUri.getQueryParameter("code")
    val returnedState = callbackUri.getQueryParameter("state")
    val session = pendingSession
    pendingSession = null

    if (code.isNullOrBlank()) {
      return Result.failure(IllegalStateException("Missing authorization code from GitHub callback."))
    }
    if (session == null || returnedState != session.state) {
      return Result.failure(SecurityException("OAuth state verification failed. Please retry login."))
    }

    val clientId = clientIdProvider().trim()
    val clientSecret = clientSecretProvider().trim()
    val hasSecret = clientSecret.isNotEmpty() && clientSecret != PLACEHOLDER_SECRET

    return withContext(Dispatchers.IO) {
      try {
        val formBuilder =
          FormBody.Builder()
            .add("client_id", clientId)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", session.codeVerifier)
        if (hasSecret) {
          formBuilder.add("client_secret", clientSecret)
        }

        val request =
          Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .post(formBuilder.build())
            .build()

        httpClient.newCall(request).execute().use { response ->
          val rawBody = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext Result.failure(
              IOException("OAuth token exchange failed (${response.code})")
            )
          }
          val parsed =
            tokenAdapter.fromJson(rawBody)
              ?: return@withContext Result.failure(IOException("Invalid OAuth token response"))
          if (!parsed.accessToken.isNullOrBlank()) {
            Result.success(parsed)
          } else {
            val msg = parsed.errorDescription ?: parsed.error ?: "GitHub did not return an access token."
            Result.failure(IOException(msg))
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  private fun generateRandomUrlSafeString(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  private fun computeS256Challenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }
}
