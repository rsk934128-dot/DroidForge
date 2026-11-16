package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.GitHubOAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.EditorDraftEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

  @Test
  fun `room editorDraftDao upserts observes and deletes offline drafts`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val dao = db.editorDraftDao()
      val draft =
        EditorDraftEntity(
          draftKey = "rsk934128-dot/DroidForge::main::app/Main.kt",
          repoFullName = "rsk934128-dot/DroidForge",
          baseBranch = "main",
          filePath = "app/Main.kt",
          targetLineNumber = 18,
          errorText = "Unresolved reference: foo",
          originalContent = "val x = foo()",
          editedContent = "val x = bar()",
          newBranchName = "droidforge/fix-main-l18",
          commitMessage = "fix: resolve foo reference",
          prTitle = "fix: resolve foo in Main.kt",
          prBody = "Auto-saved draft test",
          updatedAt = 123456L,
        )
      dao.upsert(draft)

      val fetched = dao.getDraft(draft.draftKey)
      assertNotNull(fetched)
      assertEquals("val x = bar()", fetched?.editedContent)

      val list = dao.observeAllDrafts().first()
      assertEquals(1, list.size)
      assertEquals("app/Main.kt", list.first().filePath)

      dao.deleteDraft(draft.draftKey)
      assertNull(dao.getDraft(draft.draftKey))
      assertTrue(dao.observeAllDrafts().first().isEmpty())
    } finally {
      db.close()
    }
  }
}
