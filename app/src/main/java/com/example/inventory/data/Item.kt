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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.time.LocalDateTime

/**
 * Entity data class represents a single row in the database.
 */
@Entity(primaryKeys=["feed_name", "post_id"])
data class Item(
    // @PrimaryKey(autoGenerate = true)
    // val id: Int = 0,

    // TODO: change this to a foreign key into Feeds db
    @ColumnInfo(name = "feed_name")
    val feedName: String,
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
    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "read")
    val read: Boolean,
)

///**
// * Returns the passed in price in currency format.
// */
//fun Item.getFormattedPrice(): String =
//    NumberFormat.getCurrencyInstance().format(itemPrice)