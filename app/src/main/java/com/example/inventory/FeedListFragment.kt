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

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.inventory.data.Feed
import com.example.inventory.databinding.FeedListFragmentBinding
import java.util.*
import kotlin.collections.ArrayList

// import com.example.inventory.databinding.ItemPagerBinding

/**
 * Main fragment displaying details for all items in the database.
 */
class FeedListFragment : Fragment() {
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao(),
            (activity?.application as InventoryApplication).database.feedDao()
        )
    }

    private var _binding: FeedListFragmentBinding? = null
    private val binding get() = _binding!!

    private var state: Parcelable? = null
    private var currentPosition: Int? = null

    // Map from category to list of Feeds.
    private var feedCategoryMap: SortedMap<String, MutableList<Feed>> = sortedMapOf(compareBy<String> {
        it.toLowerCase()
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FeedListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeedListAdapter(this.requireContext(), { position: Int ->
            Log.i("FeedListAdapter", "onTextClick")
            openMultiFeed(position)
        }, { position: Int, isExpanded: Boolean ->
            Log.i("FeedListAdapter", "onIndicatorClick: $position, $isExpanded")
            if (isExpanded) {
                binding.expandableListView.collapseGroup(position);
            } else {
                binding.expandableListView.expandGroup(position);
            }
        })


        binding.expandableListView.setAdapter(adapter)

//        binding.expandableListView.setOnGroupExpandListener { groupPosition -> Toast.makeText(applicationContext, (titleList as ArrayList<String>)[groupPosition] + " List Expanded.", Toast.LENGTH_SHORT).show() }
//        binding.expandableListView.setOnGroupCollapseListener { groupPosition -> Toast.makeText(applicationContext, (titleList as ArrayList<String>)[groupPosition] + " List Collapsed.", Toast.LENGTH_SHORT).show() }

        binding.expandableListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            // Toast.makeText(this.requireContext(), "Clicked: " + (titleList as ArrayList<String>)[groupPosition] + " -> " + listData[(titleList as ArrayList<String>)[groupPosition]]!!.get(childPosition), Toast.LENGTH_SHORT).show()

            val keys = ArrayList(feedCategoryMap.keys)
            val feed = feedCategoryMap[keys[groupPosition]]!![childPosition]
            val action = FeedListFragmentDirections.actionFeedListFragmentToItemListFragment(intArrayOf(feed.id), arrayOf(feed.url))
            this.findNavController().navigate(action)

            false
        }

        binding.expandableListView.setOnItemLongClickListener { parent, view, position, id ->
            val groupPosition = ExpandableListView.getPackedPositionGroup(id)
            if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val childPosition = ExpandableListView.getPackedPositionChild(id)

                // Toast.makeText(this.requireContext(), "long-clicked " + groupPosition.toString() + ", " + childPosition.toString() , Toast.LENGTH_SHORT).show()

                val keys = ArrayList(feedCategoryMap.keys)
                val feed = feedCategoryMap[keys[groupPosition]]!![childPosition]
                val action = FeedListFragmentDirections.actionFeedListFragmentToEditFeedFragment(feed.id)
                this.findNavController().navigate(action)

                true
            } else {
                false
            }
        }

        // Attach an observer on the allItems list to update the UI automatically when the data
        // changes.
        viewModel.allFeeds.observe(this.viewLifecycleOwner) { feeds ->
            feeds.let {
                feedCategoryMap.clear()
                for (feed in it) {
                    if (feedCategoryMap.containsKey(feed.category)) {
                        feedCategoryMap.getValue(feed.category).add(feed)
                    } else {
                        feedCategoryMap[feed.category] = mutableListOf(feed)
                    }
                }

                adapter.setData(feedCategoryMap)

                if (state != null && currentPosition != null) {
                    // Log.i("ExpandableListView", "trying to restore listview state");
                    binding.expandableListView.onRestoreInstanceState(state);
                    binding.expandableListView.setSelection(currentPosition!!)
                }
            }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = FeedListFragmentDirections.actionFeedListFragmentToEditFeedFragment(0)
            this.findNavController().navigate(action)
        }
    }

    private fun openMultiFeed(groupPosition: Int) {
        val keys = ArrayList(feedCategoryMap.keys)
        val feeds = feedCategoryMap[keys[groupPosition]]!!

        val feedIds = IntArray(feeds.size)
        val feedUrls: Array<String> = Array(feeds.size) { "" }
        for (i in feeds.indices) {
            feedIds[i] = feeds[i].id
            feedUrls[i] = feeds[i].url
        }

        val action = FeedListFragmentDirections.actionFeedListFragmentToItemListFragment(feedIds, feedUrls)
        this.findNavController().navigate(action)
    }

    override fun onPause() {
        state = binding.expandableListView.onSaveInstanceState()
        currentPosition = binding.expandableListView.getFirstVisiblePosition();
        super.onPause()
    }
}
