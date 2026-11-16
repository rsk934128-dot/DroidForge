package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.GitHubOAuthManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DroidForge", appName)
  }

  @Test
  fun `oauth authorize url includes pkce and redirect uri`() {
    val manager =
      GitHubOAuthManager(
        clientIdProvider = { "test_client_123" },
        clientSecretProvider = { "" },
      )
    assertTrue(manager.isOAuthConfigured())
    val uri = manager.createAuthorizationUrl().getOrThrow()
    assertEquals("https", uri.scheme)
    assertEquals("github.com", uri.host)
    assertEquals("/login/oauth/authorize", uri.path)
    assertEquals("test_client_123", uri.getQueryParameter("client_id"))
    assertEquals(GitHubOAuthManager.REDIRECT_URI, uri.getQueryParameter("redirect_uri"))
    assertEquals("S256", uri.getQueryParameter("code_challenge_method"))
    assertNotNull(uri.getQueryParameter("state"))
    assertNotNull(uri.getQueryParameter("code_challenge"))
  }

  @Test
  fun `oauth callback rejects mismatched csrf state`() = runTest {
    val manager =
      GitHubOAuthManager(
        clientIdProvider = { "test_client_123" },
        clientSecretProvider = { "" },
      )
    manager.createAuthorizationUrl().getOrThrow()
    val forgedUri = Uri.parse("droidforge://oauth/callback?code=abc123&state=wrong_state")
    assertTrue(manager.isOAuthCallbackUri(forgedUri))
    val result = manager.exchangeCallbackForToken(forgedUri)
    assertFalse(result.isSuccess)
  }
}
