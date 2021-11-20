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

package com.example.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Feed
import com.example.inventory.data.FeedDao
import com.example.inventory.data.Post
import com.example.inventory.data.PostDao
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class InventoryViewModel(private val itemDao: PostDao, private val feedDao: FeedDao) : ViewModel() {

    val allFeeds: LiveData<List<Feed>> = feedDao.getFeeds().asLiveData()

    fun updateFeed(feedId: Int, url: String, name: String, category: String) {
        val updatedFeed = getUpdatededEntry(feedId, url, name, "", category)
        updateFeed(updatedFeed)
    }

    /**
     * Launching a new coroutine to update an item in a non-blocking way
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

    // fun markItemRead(item: Item) {
    fun markItemRead(postId: Int, feedId: Int) {
        viewModelScope.launch {
            itemDao.markRead(postId, feedId)
        }
    }
    fun markItemUnread(postId: Int, feedId: Int) {
        viewModelScope.launch {
            itemDao.markUnread(postId, feedId)
        }
    }
    fun toggleItemRead(postId: Int, feedId: Int) {
        viewModelScope.launch {
            itemDao.toggleRead(postId, feedId)
        }
    }

    /**
     * Inserts the new Item into database.
     */
//    fun addNewItem(postId: Int, title: String, author: String, feedName: String, link: String, timestamp: Long, content: String) {
//        val newItem = getNewItemEntry(postId, title, author, feedName, link, timestamp, content)
//        insertItem(newItem)
//    }
    fun addNewItem(post: Post) {
        // Will ignore if have matching (feed name + post ID) entity
        insertItem(post)
    }
    fun addNewFeed(feed: Feed) {
        insertFeed(feed)
    }
    fun addNewFeed(url: String, name: String, category: String) {
        val newFeed = getNewFeedEntry(url, name, "todo", category)
        insertFeed(newFeed)
    }

    /**
     * Launching a new coroutine to insert an item in a non-blocking way
     */
    private fun insertItem(post: Post) {
        viewModelScope.launch {
            itemDao.insert(post)
        }
    }
    private fun insertFeed(feed: Feed) {
        viewModelScope.launch {
            feedDao.insert(feed)
        }
    }

    /**
     * Launching a new coroutine to delete an item in a non-blocking way
     */
    fun deleteFeed(id: Int) {
        viewModelScope.launch {
            feedDao.delete(id)
        }
    }

    /**
     * Retrieve an item from the repository.
     */
    fun retrieveFeed(feedId: Int): LiveData<Feed> {
        return feedDao.getFeed(feedId).asLiveData()
    }

    fun retrieveItemsInFeeds(feedIds: IntArray, include_read: Boolean = false, ascending: Boolean = true): LiveData<List<Post>> {
        return if (include_read) {
            if (ascending) {
                itemDao.getItemsInFeedsAsc(feedIds).asLiveData()
            } else {
                itemDao.getItemsInFeedsDesc(feedIds).asLiveData()
            }
        } else {
            if (ascending) {
                itemDao.getUnreadItemsInFeedsAsc(feedIds).asLiveData()
            } else {
                itemDao.getUnreadItemsInFeedsDesc(feedIds).asLiveData()
            }
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

    /**
     * Returns an instance of the [Post] entity class with the item info entered by the user.
     * This will be used to add a new entry to the Inventory database.
     */
    private fun getNewFeedEntry(url: String, name: String, htmlUrl: String, category: String): Feed {
        return Feed(
            url = url,
            name = name,
            htmlUrl = htmlUrl,
            category = category
        )
    }

    private fun getUpdatededEntry(id: Int, url: String, name: String, htmlUrl: String, category: String): Feed {
        return Feed(
            id = id,
            url = url,
            name = name,
            htmlUrl = htmlUrl,
            category = category
        )
    }
}

/**
 * Factory class to instantiate the [ViewModel] instance.
 */
class InventoryViewModelFactory(private val itemDao: PostDao, private val feedDao: FeedDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(itemDao, feedDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}