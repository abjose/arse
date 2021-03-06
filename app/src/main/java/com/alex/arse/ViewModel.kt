/*
 * Copyright (C) 2021 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alex.arse

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alex.arse.data.Feed
import com.alex.arse.data.FeedDao
import com.alex.arse.data.Post
import com.alex.arse.data.PostDao
import kotlinx.coroutines.launch


class ArseViewModel(private val postDao: PostDao, private val feedDao: FeedDao) : ViewModel() {

    val allFeeds: LiveData<List<Feed>> = feedDao.getFeeds().asLiveData()

    fun updateFeed(feedId: Int, url: String, name: String, category: String) {
        val updatedFeed = getUpdatedFeed(feedId, url, name, "", category, 0)
        updateFeed(updatedFeed)
    }
    fun updateFeedHash(feed: Feed, newHash: Int) {
        val updatedFeed = getUpdatedFeed(feed.id, feed.url, feed.name, "", feed.category, newHash)
        updateFeed(updatedFeed)
    }

    /**
     * Launching a new coroutine to update an feed in a non-blocking way
     */
    private fun updateFeed(feed: Feed) {
        viewModelScope.launch {
            feedDao.update(feed)
        }
    }

    // ???
    fun retrieveFeedAndRunCallback(feedId: Int, callback: (feed: Feed) -> Unit) {
        viewModelScope.launch {
            val feed = feedDao.getFeedNow(feedId)
            callback(feed)
        }
    }
    fun retrieveFeedsAndRunCallback(feedIds: IntArray, callback: (feeds: List<Feed>) -> Unit) {
        viewModelScope.launch {
            val feeds = feedDao.getFeedsNow(feedIds)
            callback(feeds)
        }
    }
    fun retrieveAllFeedsAndRunCallback(callback: (feeds: List<Feed>) -> Unit) {
        viewModelScope.launch {
            val feeds = feedDao.getAllFeedsNow()
            callback(feeds)
        }
    }

    fun markPostRead(postId: Int, feedId: Int) {
        viewModelScope.launch {
            postDao.markRead(postId, feedId)
        }
    }
    fun markPostUnread(postId: Int, feedId: Int) {
        viewModelScope.launch {
            postDao.markUnread(postId, feedId)
        }
    }
    fun togglePostRead(postId: Int, feedId: Int) {
        viewModelScope.launch {
            postDao.toggleRead(postId, feedId)
        }
    }

    /**
     * Inserts the new Post into database.
     */
    fun addNewPost(post: Post) {
        // Will ignore if have matching (feed name + post ID) entity
        insertPost(post)
    }
    fun addNewFeed(feed: Feed) {
        insertFeed(feed)
    }
    fun addNewFeed(url: String, name: String, category: String) {
        val newFeed = getNewFeed(url, name, "todo", category)
        insertFeed(newFeed)
    }

    // Warning - this doesn't necessarily prune to exactly maxPosts. Seems to be a race between
    // pruning and writing new posts.
    fun prunePosts(feedId: Int, maxPosts: Int) {
        viewModelScope.launch {
            val CHUNK_SIZE = 10;

            // Can't get this to work as a single query :'( LIMIT doesn't seem to work
            val postIds = postDao.getPostIdsInFeedDescNow(feedId)
            if (postIds.size > maxPosts) {
                val postsToRemove = postIds.subList(maxPosts, postIds.size)
                for (chunk in postsToRemove.chunked(CHUNK_SIZE)) {
                    postDao.deletePostsFromFeed(feedId, chunk)
                }
            }
        }
    }

    /**
     * Launching a new coroutine to insert an post in a non-blocking way
     */
    private fun insertPost(post: Post) {
        viewModelScope.launch {
            postDao.insert(post)
        }
    }
    private fun insertFeed(feed: Feed) {
        viewModelScope.launch {
            feedDao.insert(feed)
        }
    }

    /**
     * Launching a new coroutine to delete a feed in a non-blocking way
     */
    fun deleteFeed(id: Int) {
        viewModelScope.launch {
            feedDao.delete(id)
        }
    }

    /**
     * Retrieve a feed from the repository.
     */
    fun retrieveFeed(feedId: Int): LiveData<Feed> {
        return feedDao.getFeed(feedId).asLiveData()
    }

    fun retrievePostsInFeeds(feedIds: IntArray, include_read: Boolean = false, ascending: Boolean = true): LiveData<List<Post>> {
        return if (include_read) {
            if (ascending) {
                postDao.getPostsInFeedsAsc(feedIds).asLiveData()
            } else {
                postDao.getPostsInFeedsDesc(feedIds).asLiveData()
            }
        } else {
            if (ascending) {
                postDao.getUnreadPostsInFeedsAsc(feedIds).asLiveData()
            } else {
                postDao.getUnreadPostsInFeedsDesc(feedIds).asLiveData()
            }
        }
    }

    fun countUnreadPostsInFeedsLive(feedIds: IntArray): LiveData<Int> {
        return postDao.countUnreadPostsInFeedsLive(feedIds).asLiveData()
    }
    fun countUnreadPostsAndRunCallback(feedIds: IntArray, callback: (count: Int) -> Unit) {
        viewModelScope.launch {
            val count = postDao.countUnreadPostsInFeeds(feedIds)
            callback(count)
        }
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    fun isFeedEntryValid(url: String, name: String, category: String): Boolean {
        if (url.isBlank() || name.isBlank()) {
            return false
        }
        return true
    }

    private fun getUpdatedFeed(id: Int, url: String, name: String, htmlUrl: String, category: String, hash: Int): Feed {
        return Feed(
            id = id,
            url = url,
            name = name,
            htmlUrl = htmlUrl,
            category = category,
            contentHash = hash,
        )
    }
}

fun getNewFeed(url: String, name: String, htmlUrl: String, category: String): Feed {
    return Feed(
        url = url,
        name = name,
        htmlUrl = htmlUrl,
        category = category,
        contentHash = 0,
    )
}

/**
 * Factory class to instantiate the [ViewModel] instance.
 */
class InventoryViewModelFactory(private val postDao: PostDao, private val feedDao: FeedDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArseViewModel(postDao, feedDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}