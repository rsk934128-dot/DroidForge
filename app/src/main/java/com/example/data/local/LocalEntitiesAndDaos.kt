package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarked_repos")
data class BookmarkedRepoEntity(
  @PrimaryKey val fullName: String,
  val ownerLogin: String,
  val name: String,
  val avatarUrl: String,
  val description: String,
  val defaultBranch: String,
  val language: String,
  val stars: Int,
  val isPrivate: Boolean,
  val htmlUrl: String,
  val savedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_workflow_runs")
data class CachedWorkflowRunEntity(
  @PrimaryKey val runId: Long,
  val repoFullName: String,
  val runNumber: Int,
  val title: String,
  val status: String,
  val conclusion: String?,
  val branch: String,
  val headSha: String,
  val event: String,
  val htmlUrl: String,
  val updatedAt: String,
)

@Entity(tableName = "editor_drafts")
data class EditorDraftEntity(
  @PrimaryKey val draftKey: String,
  val repoFullName: String,
  val baseBranch: String,
  val filePath: String,
  val targetLineNumber: Int,
  val errorText: String,
  val originalContent: String,
  val editedContent: String,
  val newBranchName: String,
  val commitMessage: String,
  val prTitle: String,
  val prBody: String,
  val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface BookmarkedRepoDao {
  @Query("SELECT * FROM bookmarked_repos ORDER BY savedAt DESC")
  fun observeAll(): Flow<List<BookmarkedRepoEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_repos WHERE fullName = :fullName)")
  fun observeIsBookmarked(fullName: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(repo: BookmarkedRepoEntity)

  @Query("DELETE FROM bookmarked_repos WHERE fullName = :fullName")
  suspend fun deleteByFullName(fullName: String)
}

@Dao
interface CachedWorkflowRunDao {
  @Query("SELECT * FROM cached_workflow_runs WHERE repoFullName = :repoFullName ORDER BY runNumber DESC")
  fun observeRunsForRepo(repoFullName: String): Flow<List<CachedWorkflowRunEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(runs: List<CachedWorkflowRunEntity>)

  @Query("DELETE FROM cached_workflow_runs WHERE repoFullName = :repoFullName")
  suspend fun clearForRepo(repoFullName: String)
}

@Dao
interface EditorDraftDao {
  @Query("SELECT * FROM editor_drafts ORDER BY updatedAt DESC")
  fun observeAllDrafts(): Flow<List<EditorDraftEntity>>

  @Query("SELECT * FROM editor_drafts WHERE draftKey = :draftKey LIMIT 1")
  suspend fun getDraft(draftKey: String): EditorDraftEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(draft: EditorDraftEntity)

  @Query("DELETE FROM editor_drafts WHERE draftKey = :draftKey")
  suspend fun deleteDraft(draftKey: String)
}
