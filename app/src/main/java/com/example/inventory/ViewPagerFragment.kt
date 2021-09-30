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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.inventory.databinding.ItemPagerBinding

/**
 * Main fragment displaying details for all items in the database.
 */
class ViewPagerFragment : Fragment() {
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao(),
            (activity?.application as InventoryApplication).database.feedDao()
        )
    }

    private var _binding: ItemPagerBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: ViewPagerFragmentArgs by navArgs()

    // Prevent ViewPager list from updating once we've sent it out.
    private var skipListRefresh: Boolean = false
    private var positionSet: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ItemPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // val id = navigationArgs.itemId
        val position = navigationArgs.itemPosition
        Log.i("ViewPager", "position: $position")

        val adapter = ViewPagerAdapter(this.requireContext()) { postId, feedId ->
            // Log.i("ViewPager", "Looking at $postId, $positionSet")
            if (positionSet) {
                // Log.i("ViewPager", "marking post $postId read")
                viewModel.markItemRead(postId, feedId)
            }
        }
        // viewpager_binding.viewPager.layout = LinearLayoutManager(this.context)
        binding.viewPager.adapter = adapter
        // binding.viewPager.offscreenPageLimit = 3

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adapter.onPostViewed(position)
            }
        })

        loadFeeds(adapter)

//        binding.floatingActionButton.setOnClickListener {
//            val action = ItemListFragmentDirections.actionItemListFragmentToAddItemFragment(
//                getString(R.string.add_fragment_title)
//            )
//            this.findNavController().navigate(action)
//        }
    }

    private fun setPosition(position: Int) {
        Log.i("ViewPager", "setting position in adapter to $position")
        positionSet = true
        if (position > 0) {
            binding.viewPager.setCurrentItem(position, false)
        }
    }

    private fun loadFeeds(adapter: ViewPagerAdapter) {
        viewModel.retrieveUnreadItemsInFeeds(navigationArgs.feedIds).observe(this.viewLifecycleOwner) { items ->
            if (!skipListRefresh) {
                skipListRefresh = true
                items.let {
                    adapter.submitList(it)
                }

                setPosition(navigationArgs.itemPosition)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // binding.viewPager.unregisterOnPageChangeCallback(this)
        Log.i("ViewPager", "destroyed")
        skipListRefresh = false
        positionSet = false
    }
}
