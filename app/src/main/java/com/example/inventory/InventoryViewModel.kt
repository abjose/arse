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
import com.example.inventory.data.Item
import com.example.inventory.data.ItemDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class InventoryViewModel(private val itemDao: ItemDao, private val feedDao: FeedDao) : ViewModel() {

    // Cache all items form the database using LiveData.
//    val allItems: LiveData<List<Item>> = itemDao.getItems().asLiveData()
//    val unreadItems: LiveData<List<Item>> = itemDao.getUnreadItems().asLiveData()
    val allFeeds: LiveData<List<Feed>> = feedDao.getFeeds().asLiveData()

//    /**
//     * Returns true if stock is available to sell, false otherwise.
//     */
//    fun isStockAvailable(item: Item): Boolean {
//        return (item.quantityInStock > 0)
//    }

//    /**
//     * Updates an existing Item in the database.
//     */
//    fun updateItem(
//        itemId: Int,
//        itemName: String,
//        itemPrice: String,
//        itemCount: String
//    ) {
//        val updatedItem = getUpdatedItemEntry(itemId, itemName, itemPrice, itemCount)
//        updateItem(updatedItem)
//    }

    fun updateFeed(feedId: Int, url: String, name: String, category: String) {
        val updatedFeed = getUpdatedeedEntry(feedId, url, name, "", category)
        updateFeed(updatedFeed)
    }

    /**
     * Launching a new coroutine to update an item in a non-blocking way
     */
    private fun updateItem(item: Item) {
        viewModelScope.launch {
            itemDao.update(item)
        }
    }
    private fun updateFeed(feed: Feed) {
        viewModelScope.launch {
            feedDao.update(feed)
        }
    }

//    /**
//     * Decreases the stock by one unit and updates the database.
//     */
//    fun sellItem(item: Item) {
//        if (item.quantityInStock > 0) {
//            // Decrease the quantity by 1
//            val newItem = item.copy(quantityInStock = item.quantityInStock - 1)
//            updateItem(newItem)
//        }
//    }

    // fun markItemRead(item: Item) {
    fun markItemRead(postId: Int, feedId: Int) {
        viewModelScope.launch {
            itemDao.markRead(postId, feedId)
        }
//        // val item = itemDao.getItem(id)
//        val item = retrieveItem(id)
//        item.
//
//        viewModelScope.launch {
//            val item = itemDao.getItem(id)
//
//            val newItem = item.copy(read = true)
//            updateItem(newItem)
//        }
    }

    /**
     * Inserts the new Item into database.
     */
//    fun addNewItem(postId: Int, title: String, author: String, feedName: String, link: String, timestamp: Long, content: String) {
//        val newItem = getNewItemEntry(postId, title, author, feedName, link, timestamp, content)
//        insertItem(newItem)
//    }
    fun addNewItem(item: Item) {
        // Will ignore if have matching (feed name + post ID) entity
        insertItem(item)
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
    private fun insertItem(item: Item) {
        viewModelScope.launch {
            itemDao.insert(item)
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
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemDao.delete(item)
        }
    }
    fun deleteFeed(id: Int) {
        viewModelScope.launch {
            feedDao.delete(id)
        }
    }

    /**
     * Retrieve an item from the repository.
     */
    fun retrieveItem(postId: Int, feedId: Int): LiveData<Item> {
        return itemDao.getItem(postId, feedId).asLiveData()
    }
    fun retrieveFeed(feedId: Int): LiveData<Feed> {
        return feedDao.getFeed(feedId).asLiveData()
    }

    fun retrieveUnreadItemsInFeedLive(feedId: Int): LiveData<List<Item>> {
        return itemDao.getUnreadItemsInFeed(feedId).asLiveData()
    }
    fun retrieveUnreadItemsInFeed(feedId: Int): Flow<List<Item>> {
        return itemDao.getUnreadItemsInFeed(feedId)
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    fun isEntryValid(itemName: String, itemPrice: String, itemCount: String): Boolean {
        if (itemName.isBlank() || itemPrice.isBlank() || itemCount.isBlank()) {
            return false
        }
        return true
    }

    fun isFeedEntryValid(url: String, name: String, category: String): Boolean {
        if (url.isBlank() || name.isBlank()) {
            return false
        }
        return true
    }

    /**
     * Returns an instance of the [Item] entity class with the item info entered by the user.
     * This will be used to add a new entry to the Inventory database.
     */
    private fun getNewItemEntry(feedId: Int, postId: Int, title: String, author: String, link: String, timestamp: Long, content: String): Item {
        return Item(
            feedId = feedId,
            postId = postId,
            title = title,
            author = author,
            link = link,
            timestamp = timestamp,
            content = content,
            read = false
        )
    }
    private fun getNewFeedEntry(url: String, name: String, htmlUrl: String, category: String): Feed {
        return Feed(
            url = url,
            name = name,
            htmlUrl = htmlUrl,
            category = category
        )
    }

    private fun getUpdatedeedEntry(id: Int, url: String, name: String, htmlUrl: String, category: String): Feed {
        return Feed(
            id = id,
            url = url,
            name = name,
            htmlUrl = htmlUrl,
            category = category
        )
    }

//    /**
//     * Called to update an existing entry in the Inventory database.
//     * Returns an instance of the [Item] entity class with the item info updated by the user.
//     */
//    private fun getUpdatedItemEntry(
//        itemId: Int,
//        itemName: String,
//        itemPrice: String,
//        itemCount: String,
//        // read: Boolean,
//    ): Item {
//        return Item(
//            id = itemId,
//            itemName = itemName,
//            itemPrice = itemPrice.toDouble(),
//            quantityInStock = itemCount.toInt(),
//            read = false,
//        )
//    }
}

/**
 * Factory class to instantiate the [ViewModel] instance.
 */
class InventoryViewModelFactory(private val itemDao: ItemDao, private val feedDao: FeedDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(itemDao, feedDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}