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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.inventory.data.Item
import com.example.inventory.databinding.ItemListFragmentBinding
import kotlinx.android.synthetic.main.item_list_fragment.*

// import com.example.inventory.databinding.ItemPagerBinding

/**
 * Main fragment displaying details for all items in the database.
 */
class ItemListFragment : Fragment() {
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao(),
            (activity?.application as InventoryApplication).database.feedDao()
        )
    }

    private var _binding: ItemListFragmentBinding? = null
    private val binding get() = _binding!!
    private val na = NetworkActivity()
    private val navigationArgs: ItemListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ItemListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun refresh() {
        // Toast.makeText(this.requireContext(), "Failed to load feed URL" , Toast.LENGTH_SHORT).show()

        swipe_refresh.isRefreshing = true
        na.loadFeed(navigationArgs.feedIds, navigationArgs.feedUrls, this.requireContext(), viewModel) {
            Log.i("ItemListFragment", "refresh is done")
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        val adapter = ItemListAdapter(navigationArgs.feedIds.size > 1, viewModel) { position: Int ->
            val action = ItemListFragmentDirections.actionItemListFragmentToViewPagerFragment(position, navigationArgs.feedIds)
            this.findNavController().navigate(action)
        }
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.recyclerView.adapter = adapter

        // Move this stuff somewhere else
        // https://stackoverflow.com/questions/49827752/how-to-implement-drag-and-drop-and-swipe-to-delete-in-recyclerview
        val fragmentLifecycleOwner = this.viewLifecycleOwner
        val itemTouchHelperCallback =
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    adapter.onItemDismiss(viewHolder.adapterPosition)

                    val postId = viewHolder.itemView.getTag("postId".hashCode()) as Int
                    val feedId = viewHolder.itemView.getTag("feedId".hashCode()) as Int
                    Log.v("SWIPED", postId.toString() + ", " + feedId);
                    viewModel.markItemRead(postId, feedId)
                }
            }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        swipe_refresh.setOnRefreshListener {
            val LOG_TAG = "REFRESH"
            Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout")

            refresh()
        }

        viewModel.retrieveFeedAndRunCallback(navigationArgs.feedIds[0]) { feed ->
            if (navigationArgs.feedIds.size == 1) {
                (activity as AppCompatActivity).supportActionBar!!.title = feed.name
            } else {
                (activity as AppCompatActivity).supportActionBar!!.title = feed.category
            }
        }

        // Attach an observer on the allItems list to update the UI automatically when the data
        // changes.
        viewModel.retrieveUnreadItemsInFeeds(navigationArgs.feedIds).observe(this.viewLifecycleOwner) { items ->
            items.let {
                adapter.submitList(it)
            }
        }

        binding.floatingActionButton.setOnClickListener {
            refresh()
        }
    }
}
