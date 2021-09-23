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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.inventory.data.Feed
import com.example.inventory.data.Item
import com.example.inventory.databinding.FeedListFragmentBinding
import kotlinx.android.synthetic.main.feed_list_fragment.*
import kotlinx.android.synthetic.main.item_list_fragment.*

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

    private var feedCategoryMap: HashMap<String, MutableList<Feed>> = HashMap<String, MutableList<Feed>>()

//    val data: HashMap<String, List<String>>
//        get() {
//            val listData = HashMap<String, List<String>>()
//
//            // REPLACE THESE WITH REAL FEEDS!!!
//
//            val redmiMobiles = ArrayList<String>()
//            redmiMobiles.add("Redmi Y2")
//            redmiMobiles.add("Redmi S2")
//            redmiMobiles.add("Redmi Note 5 Pro")
//            redmiMobiles.add("Redmi Note 5")
//            redmiMobiles.add("Redmi 5 Plus")
//            redmiMobiles.add("Redmi Y1")
//            redmiMobiles.add("Redmi 3S Plus")
//
//            val micromaxMobiles = ArrayList<String>()
//            micromaxMobiles.add("Micromax Bharat Go")
//            micromaxMobiles.add("Micromax Bharat 5 Pro")
//            micromaxMobiles.add("Micromax Bharat 5")
//            micromaxMobiles.add("Micromax Canvas 1")
//            micromaxMobiles.add("Micromax Dual 5")
//
//            val appleMobiles = ArrayList<String>()
//            appleMobiles.add("iPhone 8")
//            appleMobiles.add("iPhone 8 Plus")
//            appleMobiles.add("iPhone X")
//            appleMobiles.add("iPhone 7 Plus")
//            appleMobiles.add("iPhone 7")
//            appleMobiles.add("iPhone 6 Plus")
//
//            val samsungMobiles = ArrayList<String>()
//            samsungMobiles.add("Samsung Galaxy S9+")
//            samsungMobiles.add("Samsung Galaxy Note 7")
//            samsungMobiles.add("Samsung Galaxy Note 5 Dual")
//            samsungMobiles.add("Samsung Galaxy S8")
//            samsungMobiles.add("Samsung Galaxy A8")
//            samsungMobiles.add("Samsung Galaxy Note 4")
//
//            listData["Redmi"] = redmiMobiles
//            listData["Micromax"] = micromaxMobiles
//            listData["Apple"] = appleMobiles
//            listData["Samsung"] = samsungMobiles
//
//            return listData
//        }

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


//        val adapter = FeedListAdapter { feed: Feed, position: Int ->
//            val action = FeedListFragmentDirections.actionFeedListFragmentToItemListFragment(item.postId, item.feedName, position)
//            this.findNavController().navigate(action)
//        }

        // val listData = data
        // val titleList = ArrayList(listData.keys)
        // val adapter = FeedListAdapter(this.requireContext(), titleList as ArrayList<String>, listData)
        val adapter = FeedListAdapter(this.requireContext())
        binding.expandableListView.setAdapter(adapter)

//        binding.expandableListView.setOnGroupExpandListener { groupPosition -> Toast.makeText(applicationContext, (titleList as ArrayList<String>)[groupPosition] + " List Expanded.", Toast.LENGTH_SHORT).show() }
//        binding.expandableListView.setOnGroupCollapseListener { groupPosition -> Toast.makeText(applicationContext, (titleList as ArrayList<String>)[groupPosition] + " List Collapsed.", Toast.LENGTH_SHORT).show() }

        binding.expandableListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            // Toast.makeText(this.requireContext(), "Clicked: " + (titleList as ArrayList<String>)[groupPosition] + " -> " + listData[(titleList as ArrayList<String>)[groupPosition]]!!.get(childPosition), Toast.LENGTH_SHORT).show()

            val keys = ArrayList(feedCategoryMap.keys)
            val feed = feedCategoryMap[keys[groupPosition]]!![childPosition]
            val action = FeedListFragmentDirections.actionFeedListFragmentToItemListFragment(feed.url)
            this.findNavController().navigate(action)

            false
        }

        binding.expandableListView.setOnItemLongClickListener { parent, view, position, id ->
            if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val groupPosition = ExpandableListView.getPackedPositionGroup(id)
                val childPosition = ExpandableListView.getPackedPositionChild(id)

                // TODO: switch to Feed editing menu? or just have straight from feed items list
                Toast.makeText(this.requireContext(), "long-clicked " + groupPosition.toString() + ", " + childPosition.toString() , Toast.LENGTH_SHORT).show()

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
            }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = FeedListFragmentDirections.actionFeedListFragmentToEditFeedFragment("")
            this.findNavController().navigate(action)
        }
    }
}
