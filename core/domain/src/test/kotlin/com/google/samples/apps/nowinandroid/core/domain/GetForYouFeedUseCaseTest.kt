/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.samples.apps.nowinandroid.core.domain

import com.google.samples.apps.nowinandroid.core.data.repository.CompositeUserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.ForYouFeed
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.testing.repository.TestNewsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class GetForYouFeedUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val newsRepository = TestNewsRepository()
    private val userDataRepository = TestUserDataRepository()

    private val useCase = GetForYouFeedUseCase(
        userNewsResourceRepository = CompositeUserNewsResourceRepository(
            newsRepository = newsRepository,
            userDataRepository = userDataRepository,
        ),
        userDataRepository = userDataRepository,
    )

    @Test
    fun whenViewedNewsResourcesAreShown_feedContainsEveryFollowedNewsResource() = runTest {
        val userData = emptyUserData.copy(
            followedTopics = setOf(testTopic.id),
            viewedNewsResources = setOf("2"),
            hideViewedNewsResources = false,
        )
        newsRepository.sendNewsResources(testNewsResources)
        userDataRepository.setUserData(userData)

        assertEquals(
            ForYouFeed(
                newsResources = testNewsResources.map { UserNewsResource(it, userData) },
                hiddenViewedCount = 0,
            ),
            useCase().first(),
        )
    }

    @Test
    fun whenViewedNewsResourcesAreHidden_viewedOnesAreLeftOutAndCounted() = runTest {
        val userData = emptyUserData.copy(
            followedTopics = setOf(testTopic.id),
            viewedNewsResources = setOf("1", "3"),
            hideViewedNewsResources = true,
        )
        newsRepository.sendNewsResources(testNewsResources)
        userDataRepository.setUserData(userData)

        assertEquals(
            ForYouFeed(
                newsResources = listOf(UserNewsResource(testNewsResources[1], userData)),
                hiddenViewedCount = 2,
            ),
            useCase().first(),
        )
    }

    @Test
    fun whenEveryNewsResourceIsViewedAndHidden_feedIsEmpty() = runTest {
        val userData = emptyUserData.copy(
            followedTopics = setOf(testTopic.id),
            viewedNewsResources = testNewsResources.map(NewsResource::id).toSet(),
            hideViewedNewsResources = true,
        )
        newsRepository.sendNewsResources(testNewsResources)
        userDataRepository.setUserData(userData)

        assertEquals(
            ForYouFeed(
                newsResources = emptyList(),
                hiddenViewedCount = testNewsResources.size,
            ),
            useCase().first(),
        )
    }

    @Test
    fun viewedNewsResourcesForUnfollowedTopics_areNotCountedAsHidden() = runTest {
        val userData = emptyUserData.copy(
            followedTopics = emptySet(),
            viewedNewsResources = setOf("1"),
            hideViewedNewsResources = true,
        )
        newsRepository.sendNewsResources(testNewsResources)
        userDataRepository.setUserData(userData)

        assertEquals(
            ForYouFeed(newsResources = emptyList(), hiddenViewedCount = 0),
            useCase().first(),
        )
    }
}

private val testTopic = Topic(
    id = "T1",
    name = "Compose",
    shortDescription = "",
    longDescription = "",
    url = "",
    imageUrl = "",
)

private val testNewsResources = listOf("1", "2", "3").map { id ->
    NewsResource(
        id = id,
        title = "News $id",
        content = "Content $id",
        url = "https://example.com/$id",
        headerImageUrl = null,
        publishDate = Instant.parse("2022-10-0${id}T00:00:00.000Z"),
        type = "Article 📚",
        topics = listOf(testTopic),
    )
}
