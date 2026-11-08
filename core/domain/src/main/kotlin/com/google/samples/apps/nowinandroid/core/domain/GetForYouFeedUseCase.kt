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

import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.ForYouFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * A use case which builds the For You feed from the news resources for the user's followed
 * topics, leaving out the ones the user has already viewed if they asked for that.
 */
class GetForYouFeedUseCase @Inject constructor(
    private val userNewsResourceRepository: UserNewsResourceRepository,
    private val userDataRepository: UserDataRepository,
) {
    operator fun invoke(): Flow<ForYouFeed> = combine(
        userNewsResourceRepository.observeAllForFollowedTopics(),
        userDataRepository.userData
            .map { it.hideViewedNewsResources }
            .distinctUntilChanged(),
    ) { newsResources, hideViewedNewsResources ->
        if (hideViewedNewsResources) {
            val (unviewed, viewed) = newsResources.partition { !it.hasBeenViewed }
            ForYouFeed(
                newsResources = unviewed,
                hiddenViewedCount = viewed.size,
            )
        } else {
            ForYouFeed(newsResources = newsResources)
        }
    }
}
