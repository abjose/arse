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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Entity data class represents a single row in the database.
 */
@Entity(primaryKeys=["feed_id", "post_id"], indices = [Index(value = ["feed_id", "post_id"])])
data class Post(
    // TODO: change this to a foreign key
    @ColumnInfo(name = "feed_id")
    val feedId: Int,
    @ColumnInfo(name = "post_id")
    val postId: Int,

    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "author")
    val author: String,
    @ColumnInfo(name = "link")
    val link: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    // Short summary of content in plaintext
    @ColumnInfo(name = "description")
    val description: String,
    // Full content, likely html
    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "read")
    val read: Boolean,
)