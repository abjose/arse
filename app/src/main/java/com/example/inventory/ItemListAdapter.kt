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

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.data.Item
import com.example.inventory.databinding.ItemListItemBinding
import org.jsoup.Jsoup
import java.lang.Integer.min
import java.text.SimpleDateFormat
import java.util.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

class ItemListAdapter(private val onItemClicked: (Item, Int) -> Unit) :
    ListAdapter<Item, ItemListAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        Log.i("ItemListAdapter", "parent: ${parent.toString()}, ${parent.width}, ${parent.height}")
        return ItemViewHolder(
            ItemListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(current, position)
        }
        holder.bind(current)
        holder.itemView.setTag("postId".hashCode(), current.postId)
        holder.itemView.setTag("feedId".hashCode(), current.feedId)
    }

    class ItemViewHolder(private var binding: ItemListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.itemAuthor.text = item.author
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm")
            binding.itemDate.text = sdf.format(Date(item.timestamp))

            val contentString = Jsoup.parse(item.content).text()
            val ssb = SpannableStringBuilder("${item.title} -  ${contentString.subSequence(0, min(200, contentString.length))}")
            ssb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, item.title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.itemDescription.text = ssb
            // binding.itemDescription.ellipsize = TextUtils.TruncateAt.END
            // binding.itemName.isSingleLine = true
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.postId == newItem.postId
            }
        }
    }

    fun onItemDismiss(position: Int) {
        // items.removeAt(position)
        // actually remove from db? / mark as read
        // notifyItemRemoved(position)
        // onSwiped()
    }
}
