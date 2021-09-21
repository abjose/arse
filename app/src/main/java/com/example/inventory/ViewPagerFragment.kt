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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.databinding.ItemListFragmentBinding
import com.example.inventory.databinding.ItemPagerBinding

/**
 * Main fragment displaying details for all items in the database.
 */
class ViewPagerFragment : Fragment() {
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao()
        )
    }

    private var _binding: ItemPagerBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: ViewPagerFragmentArgs by navArgs()

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

//        // Retrieve the item details using the itemId.
//        // Attach an observer on the data (instead of polling for changes) and only update the
//        // the UI when the data actually changes.
//        viewModel.retrieveItem(id).observe(this.viewLifecycleOwner) { selectedItem ->
//            item = selectedItem
//            bind(item)
//        }

        val adapter = ViewPagerAdapter()
        // viewpager_binding.viewPager.layout = LinearLayoutManager(this.context)
        binding.viewPager.adapter = adapter

        // Attach an observer on the allItems list to update the UI automatically when the data
        // changes.
        // viewModel.allItems.observe(this.viewLifecycleOwner) { items ->
        viewModel.unreadItems.observe(this.viewLifecycleOwner) { items ->
            items.let {
                adapter.submitList(it)
                // viewpager_adapter.submitList(it)
            }
        }

        // Must be a better way to do this - can see it flash to a different view at first.
        binding.viewPager.post {
            binding.viewPager.setCurrentItem(position, false)
        }

//        binding.floatingActionButton.setOnClickListener {
//            val action = ItemListFragmentDirections.actionItemListFragmentToAddItemFragment(
//                getString(R.string.add_fragment_title)
//            )
//            this.findNavController().navigate(action)
//        }
    }
}
