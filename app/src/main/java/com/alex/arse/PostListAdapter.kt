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

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alex.arse.data.Post
import com.alex.arse.databinding.PostListItemBinding
import org.jsoup.Jsoup
import java.lang.Integer.min
import java.text.SimpleDateFormat
import java.util.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

class PostListAdapter(private val isMultiFeed: Boolean, private val viewModel: ArseViewModel,
                      private val onPostClicked: (Int) -> Unit,
                      private val onPostLongClicked: (postId: Int, feedId: Int, position: Int) -> Unit) :
    ListAdapter<Post, PostListAdapter.PostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.i("PostListAdapter", "parent: ${parent.toString()}, ${parent.width}, ${parent.height}")
        return PostViewHolder(
            PostListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            ), isMultiFeed, viewModel
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            onPostClicked(position)
        }
        holder.itemView.setOnLongClickListener {
            onPostLongClicked(current.postId, current.feedId, position)
            true
        }
        holder.bind(current)
        holder.itemView.setTag("postId".hashCode(), current.postId)
        holder.itemView.setTag("feedId".hashCode(), current.feedId)
    }

    class PostViewHolder(private var binding: PostListItemBinding, private val isMultiFeed: Boolean, private val viewModel: ArseViewModel) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.postAuthor.text = post.author
            if (isMultiFeed) {
                viewModel.retrieveFeedAndRunCallback(post.feedId) { feed ->
                    if (feed.name != post.author) {
                        if (post.author.isBlank()) {
                            binding.postAuthor.text = "${feed.name}"
                        } else {
                            binding.postAuthor.text = "${post.author} (${feed.name})"
                        }
                    }
                }
            }
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm")
            binding.postDate.text = sdf.format(Date(post.timestamp))

            val ssb = SpannableStringBuilder("${post.title} -  ${post.description}")
            ssb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, post.title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.postDescription.text = ssb
            // binding.itemDescription.ellipsize = TextUtils.TruncateAt.END
            // binding.itemName.isSingleLine = true

            // If already read, activate readView to "grey out".
            if (post.read) {
                binding.readView.visibility = View.VISIBLE
            } else {
                binding.readView.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldPost: Post, newPost: Post): Boolean {
                return oldPost.postId == newPost.postId
            }

            override fun areContentsTheSame(oldPost: Post, newPost: Post): Boolean {
                return oldPost.postId == newPost.postId
            }
        }
    }

    fun onPostDismiss(position: Int) {
        // items.removeAt(position)
        // actually remove from db? / mark as read
        // notifyItemRemoved(position)
        // onSwiped()
    }
}
