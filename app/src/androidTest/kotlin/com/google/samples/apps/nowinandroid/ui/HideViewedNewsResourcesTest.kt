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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.google.samples.apps.nowinandroid.MainActivity
import com.google.samples.apps.nowinandroid.core.data.repository.NewsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.rules.GrantPostNotificationsPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import com.google.samples.apps.nowinandroid.feature.foryou.api.R as FeatureForyouR
import com.google.samples.apps.nowinandroid.feature.settings.impl.R as SettingsR

/**
 * End-to-end tests for hiding already viewed news resources from the For You feed.
 */
@HiltAndroidTest
class HideViewedNewsResourcesTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val postNotificationsPermission = GrantPostNotificationsPermissionRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var newsRepository: NewsRepository

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private val settings by composeTestRule.stringResource(
        SettingsR.string.feature_settings_impl_top_app_bar_action_icon_description,
    )
    private val hide by composeTestRule.stringResource(
        SettingsR.string.feature_settings_impl_viewed_news_resources_hide,
    )
    private val ok by composeTestRule.stringResource(
        SettingsR.string.feature_settings_impl_dismiss_dialog_button_text,
    )
    private val showViewedNewsResources by composeTestRule.stringResource(
        FeatureForyouR.string.feature_foryou_api_show_viewed_news_resources,
    )

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun hidingViewedNewsResourcesFromSettings_removesThemFromForYouUntilShownAgain() {
        val newsResource = runBlocking { newsRepository.getNewsResources().first().first() }

        // GIVEN the user follows a topic and has already viewed one of its news resources
        runBlocking {
            userDataRepository.setFollowedTopicIds(setOf(newsResource.topics.first().id))
            userDataRepository.setShouldHideOnboarding(true)
            userDataRepository.setNewsResourceViewed(newsResource.id, viewed = true)
        }

        composeTestRule.apply {
            // WHEN they choose to hide read articles in the settings dialog
            onNodeWithContentDescription(settings).performClick()
            onNodeWithText(hide).performScrollTo().performClick()
            onNodeWithText(hide).assertIsSelected()
            onNodeWithText(ok).performClick()
            waitUntil(timeoutMillis = 5_000) { hideViewedNewsResources() }

            // THEN the feed tells them a read article is hidden
            onNodeWithTag("forYou:feed")
                .performScrollToNode(hasTestTag(HIDDEN_VIEWED_NEWS_RESOURCES_TAG))
            onNodeWithTag(HIDDEN_VIEWED_NEWS_RESOURCES_TAG).assertExists()

            // WHEN they ask to see read articles again
            onNodeWithText(showViewedNewsResources).performClick()
            waitUntil(timeoutMillis = 5_000) { !hideViewedNewsResources() }

            // THEN the notice goes away and the viewed news resource is back in the feed
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(HIDDEN_VIEWED_NEWS_RESOURCES_TAG)
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            onNodeWithTag("forYou:feed")
                .performScrollToNode(hasTestTag("newsResourceCard:${newsResource.id}"))
            onNodeWithTag("newsResourceCard:${newsResource.id}").assertExists()
        }
    }

    private fun hideViewedNewsResources(): Boolean = runBlocking {
        userDataRepository.userData.first().hideViewedNewsResources
    }
}

private const val HIDDEN_VIEWED_NEWS_RESOURCES_TAG = "forYou:hiddenViewedNewsResources"
