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
package com.example.inventory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Database access object to access the Inventory database
 */
@Dao
interface ItemDao {

    @Query("SELECT * from item WHERE feed_id IN (:feedIds) AND read = 0 ORDER BY timestamp ASC")
    fun getUnreadItemsInFeedsAsc(feedIds: IntArray): Flow<List<Item>>
    @Query("SELECT * from item WHERE feed_id IN (:feedIds) AND read = 0 ORDER BY timestamp DESC")
    fun getUnreadItemsInFeedsDesc(feedIds: IntArray): Flow<List<Item>>
    @Query("SELECT * from item WHERE feed_id IN (:feedIds) ORDER BY timestamp ASC")
    fun getItemsInFeedsAsc(feedIds: IntArray): Flow<List<Item>>
    @Query("SELECT * from item WHERE feed_id IN (:feedIds) ORDER BY timestamp DESC")
    fun getItemsInFeedsDesc(feedIds: IntArray): Flow<List<Item>>

    // uhh
    @Query("UPDATE item SET read = 1 WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun markRead(postId: Int, feedId: Int)
    @Query("UPDATE item SET read = 0 WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun markUnread(postId: Int, feedId: Int)

    @Query("UPDATE item SET read = NOT read WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun toggleRead(postId: Int, feedId: Int)

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Item)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)
}
