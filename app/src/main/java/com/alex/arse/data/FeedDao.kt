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
package com.alex.arse.data

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
interface FeedDao {

    @Query("SELECT * from feed ORDER BY name ASC")
    fun getFeeds(): Flow<List<Feed>>
    @Query("SELECT * from feed WHERE id = :feedId")
    fun getFeed(feedId: Int): Flow<Feed>

    @Query("SELECT * from feed ORDER BY name ASC")
    suspend fun getAllFeedsNow(): List<Feed>
    @Query("SELECT * from feed WHERE id IN (:feedIds) ORDER BY name ASC")
    suspend fun getFeedsNow(feedIds: IntArray): List<Feed>
    @Query("SELECT * from feed WHERE id = :feedId")
    suspend fun getFeedNow(feedId: Int): Feed

    @Query("DELETE from feed WHERE id = :feedId")
    suspend fun delete(feedId: Int)

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(feed: Feed)

    @Update
    suspend fun update(feed: Feed)

    @Delete
    suspend fun delete(feed: Feed)
}
