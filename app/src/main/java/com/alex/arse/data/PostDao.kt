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
interface PostDao {

    @Query("SELECT * from post WHERE feed_id IN (:feedIds) AND read = 0 ORDER BY timestamp ASC")
    fun getUnreadPostsInFeedsAsc(feedIds: IntArray): Flow<List<Post>>
    @Query("SELECT * from post WHERE feed_id IN (:feedIds) AND read = 0 ORDER BY timestamp DESC")
    fun getUnreadPostsInFeedsDesc(feedIds: IntArray): Flow<List<Post>>
    @Query("SELECT * from post WHERE feed_id IN (:feedIds) ORDER BY timestamp ASC")
    fun getPostsInFeedsAsc(feedIds: IntArray): Flow<List<Post>>
    @Query("SELECT * from post WHERE feed_id IN (:feedIds) ORDER BY timestamp DESC")
    fun getPostsInFeedsDesc(feedIds: IntArray): Flow<List<Post>>

    @Query("SELECT COUNT(*) from post WHERE feed_id IN (:feedIds) AND read = 0")
    fun countUnreadPostsInFeedsLive(feedIds: IntArray): Flow<Int>
    @Query("SELECT COUNT(*) from post WHERE feed_id IN (:feedIds) AND read = 0")
    suspend fun countUnreadPostsInFeeds(feedIds: IntArray): Int

    // uhh
    @Query("UPDATE post SET read = 1 WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun markRead(postId: Int, feedId: Int)
    @Query("UPDATE post SET read = 0 WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun markUnread(postId: Int, feedId: Int)

    @Query("UPDATE post SET read = NOT read WHERE post_id = :postId AND feed_id = :feedId")
    suspend fun toggleRead(postId: Int, feedId: Int)

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Post into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(post: Post)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("SELECT post_id FROM post WHERE feed_id = :feedId ORDER BY timestamp DESC")
    suspend fun getPostIdsInFeedDescNow(feedId: Int): List<Int>

    @Query("DELETE FROM post WHERE feed_id = :feedId AND post_id = :postId")
    suspend fun deletePostFromFeed(feedId: Int, postId: Int)

    // Warning: seems this can't handle more than a few posts at a time.
    @Query("DELETE FROM post WHERE feed_id = :feedId AND post_id IN (:posts)")
    suspend fun deletePostsFromFeed(feedId: Int, posts: List<Int>)
}
