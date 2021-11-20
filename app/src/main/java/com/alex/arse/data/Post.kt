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

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Entity data class represents a single row in the database.
 */
@Entity(primaryKeys=["feed_id", "post_id"])
data class Post(
    // @PrimaryKey(autoGenerate = true)
    // val id: Int = 0,

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
    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "read")
    val read: Boolean,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "title",
        parcel.readString() ?: "author",
        parcel.readString() ?: "parcel",
        parcel.readLong() ?: 0,
        parcel.readString() ?: "content",
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(feedId)
        parcel.writeInt(postId)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(link)
        parcel.writeLong(timestamp)
        parcel.writeString(content)
        parcel.writeByte(if (read) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}

///**
// * Returns the passed in price in currency format.
// */
//fun Item.getFormattedPrice(): String =
//    NumberFormat.getCurrencyInstance().format(itemPrice)