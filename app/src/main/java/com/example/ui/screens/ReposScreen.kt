package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookmarkedRepoEntity
import com.example.data.network.GitHubRepoDto
import com.example.data.network.RateLimitCoreDto
import com.example.ui.components.MonoBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.ExploreReposState

@Composable
fun ReposScreen(
  strings: AppStrings,
  searchQuery: String,
  exploreState: ExploreReposState,
  bookmarkedRepos: List<BookmarkedRepoEntity>,
  activeRepoFullName: String,
  rateLimit: RateLimitCoreDto?,
  hasPat: Boolean,
  onQueryChange: (String) -> Unit,
  onSearchSubmit: (String) -> Unit,
  onLoadMyRepos: () -> Unit,
  onStartOAuth: () -> Unit,
  onToggleBookmark: (GitHubRepoDto) -> Unit,
  onRemoveBookmark: (String) -> Unit,
  onSelectRepoForPipeline: (GitHubRepoDto) -> Unit,
  onSelectSavedRepoForPipeline: (BookmarkedRepoEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSegment by rememberSaveable { mutableIntStateOf(1) } // 0 = Saved, 1 = Explore
  val keyboardController = LocalSoftwareKeyboardController.current
  val bookmarkedNames = bookmarkedRepos.map { it.fullName }.toSet()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      HeroPipelineBanner(strings = strings, rateLimit = rateLimit)
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text(strings.searchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
              KeyboardActions(
                onSearch = {
                  keyboardController?.hide()
                  selectedSegment = 1
                  onSearchSubmit(searchQuery)
                }
              ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).testTag("repo_search_input"),
          )
          Button(
            onClick = {
              keyboardController?.hide()
              selectedSegment = 1
              onSearchSubmit(searchQuery)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(54.dp).testTag("fetch_repos_button"),
          ) {
            Text(strings.fetchButton)
          }
        }

        val quickHandles = listOf("rsk934128-dot", "android", "JetBrains")
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          item {
            Text(
              text = "${strings.quickProfiles}:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (hasPat) {
            item {
              AssistChip(
                onClick = {
                  selectedSegment = 1
                  onLoadMyRepos()
                },
                label = { Text("My Repos", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("quick_my_repos"),
              )
            }
          } else {
            item {
              AssistChip(
                onClick = onStartOAuth,
                label = { Text(strings.signInWithGitHub, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("quick_oauth_signin"),
              )
            }
          }
          items(quickHandles) { handle ->
            AssistChip(
              onClick = {
                selectedSegment = 1
                onSearchSubmit(handle)
              },
              label = { Text("@$handle", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("quick_handle_$handle"),
            )
          }
        }
      }
    }

    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedSegment == 0,
          onClick = { selectedSegment = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("segment_saved_repos"),
        ) {
          Text("${strings.tabSavedRepos} (${bookmarkedRepos.size})")
        }
        SegmentedButton(
          selected = selectedSegment == 1,
          onClick = { selectedSegment = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("segment_explore_repos"),
        ) {
          Text(strings.tabExploreRepos)
        }
      }
    }

    if (selectedSegment == 0) {
      if (bookmarkedRepos.isEmpty()) {
        item {
          EmptyReposCard(
            title = strings.emptySavedTitle,
            body = strings.emptySavedBody,
          )
        }
      } else {
        items(bookmarkedRepos, key = { it.fullName }) { saved ->
          SavedRepoCard(
            repo = saved,
            isActive = saved.fullName == activeRepoFullName,
            strings = strings,
            onOpenPipeline = { onSelectSavedRepoForPipeline(saved) },
            onRemove = { onRemoveBookmark(saved.fullName) },
          )
        }
      }
    } else {
      when (exploreState) {
        is ExploreReposState.Idle -> {
          item {
            EmptyReposCard(
              title = strings.emptyExploreTitle,
              body = strings.emptyExploreBody,
            )
          }
        }
        is ExploreReposState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is ExploreReposState.Error -> {
          item {
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer
                ),
              shape = RoundedCornerShape(18.dp),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = exploreState.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
                FilledTonalButton(
                  onClick = { onSearchSubmit(searchQuery) },
                  modifier = Modifier.testTag("retry_fetch_button"),
                ) {
                  Text(strings.refresh)
                }
              }
            }
          }
        }
        is ExploreReposState.Success -> {
          item {
            Text(
              text = "${exploreState.queryLabel} · ${exploreState.repos.size} repositories",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          if (exploreState.repos.isEmpty()) {
            item {
              EmptyReposCard(
                title = strings.emptyExploreTitle,
                body = strings.emptyExploreBody,
              )
            }
          } else {
            items(exploreState.repos, key = { it.id }) { repoDto ->
              LiveRepoCard(
                repo = repoDto,
                isBookmarked = repoDto.fullName in bookmarkedNames,
                isActive = repoDto.fullName == activeRepoFullName,
                strings = strings,
                onToggleBookmark = { onToggleBookmark(repoDto) },
                onOpenPipeline = { onSelectRepoForPipeline(repoDto) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeroPipelineBanner(
  strings: AppStrings,
  rateLimit: RateLimitCoreDto?,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF07182A)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(168.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_ci_hero_1794814650260),
        contentDescription = strings.heroTitle,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors =
                  listOf(
                    Color(0xF007111C),
                    Color(0xC807111C),
                    Color(0x5507111C),
                  )
              )
            )
      )
      Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = strings.heroBadge,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          if (rateLimit != null) {
            Surface(
              color = Color(0xAA0B1117),
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "API ${rateLimit.remaining}/${rateLimit.limit}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = strings.heroTitle,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = strings.heroSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun LiveRepoCard(
  repo: GitHubRepoDto,
  isBookmarked: Boolean,
  isActive: Boolean,
  strings: AppStrings,
  onToggleBookmark: () -> Unit,
  onOpenPipeline: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onOpenPipeline)
        .testTag("live_repo_card_${repo.name}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AsyncImage(
          model = repo.owner.avatarUrl,
          contentDescription = repo.owner.login,
          modifier = Modifier.size(38.dp).clip(CircleShape),
          contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = repo.name,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (repo.private) {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Private",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
          Text(
            text = repo.fullName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onToggleBookmark,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("bookmark_${repo.name}"),
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
            tint =
              if (isBookmarked) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (!repo.description.isNullOrBlank()) {
        Text(
          text = repo.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repo.language?.let { lang ->
            MonoBadge(text = lang, icon = Icons.Rounded.Code)
          }
          MonoBadge(text = repo.defaultBranch, icon = Icons.Rounded.AccountTree)
          MonoBadge(text = "${repo.stargazersCount}", icon = Icons.Rounded.Star)
        }

        FilledTonalButton(
          onClick = onOpenPipeline,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("open_pipeline_${repo.name}"),
        ) {
          Text(strings.openCiCd, style = MaterialTheme.typography.labelLarge)
          Spacer(Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SavedRepoCard(
  repo: BookmarkedRepoEntity,
  isActive: Boolean,
  strings: AppStrings,
  onOpenPipeline: () -> Unit,
  onRemove: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onOpenPipeline)
        .testTag("saved_repo_card_${repo.name}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AsyncImage(
        model = repo.avatarUrl,
        contentDescription = repo.ownerLogin,
        modifier = Modifier.size(40.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = repo.fullName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MonoBadge(text = repo.language, icon = Icons.Rounded.Code)
          MonoBadge(text = repo.defaultBranch, icon = Icons.Rounded.AccountTree)
        }
      }
      IconButton(
        onClick = onRemove,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("remove_saved_${repo.name}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Remove",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilledTonalButton(
        onClick = onOpenPipeline,
        modifier = Modifier.testTag("saved_open_pipeline_${repo.name}"),
      ) {
        Icon(Icons.Rounded.RocketLaunch, contentDescription = strings.openCiCd, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
private fun EmptyReposCard(title: String, body: String) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.FolderOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
