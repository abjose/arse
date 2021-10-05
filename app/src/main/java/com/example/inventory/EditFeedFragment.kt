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

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventory.data.Feed
import com.example.inventory.data.Item
import com.example.inventory.databinding.FragmentAddItemBinding
import com.example.inventory.databinding.FragmentEditFeedBinding

/**
 * Fragment to add or update a feed in the Inventory database.
 */
class EditFeedFragment : Fragment() {

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    // to share the ViewModel across fragments.
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao(),
            (activity?.application as InventoryApplication).database.feedDao(),
        )
    }
    private val navigationArgs: EditFeedFragmentArgs by navArgs()

    lateinit var feed: Feed

    // Binding object instance corresponding to the fragment_add_item.xml layout
    // This property is non-null between the onCreateView() and onDestroyView() lifecycle callbacks,
    // when the view hierarchy is attached to the fragment
    private var _binding: FragmentEditFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    private fun isEntryValid(): Boolean {
        return viewModel.isFeedEntryValid(
            binding.feedUrl.text.toString(),
            binding.feedName.text.toString(),
            binding.category.text.toString(),
        )
    }

    /**
     * Binds views with the passed in [item] information.
     */
    private fun bind(feed: Feed) {
        (activity as AppCompatActivity).supportActionBar!!.title = "Editing ${feed.name}"
        binding.apply {
            feedUrl.setText(feed.url, TextView.BufferType.SPANNABLE)
            feedName.setText(feed.name, TextView.BufferType.SPANNABLE)
            category.setText(feed.category, TextView.BufferType.SPANNABLE)
            saveAction.setOnClickListener { updateFeed() }
            deleteAction.setOnClickListener { deleteFeed() }
            loadOpmlAction.isVisible = false
        }
    }

    /**
     * Inserts the new Item into database and navigates up to list fragment.
     */
    private fun addNewFeed() {
        if (isEntryValid()) {
            viewModel.addNewFeed(
                binding.feedUrl.text.toString(),
                binding.feedName.text.toString(),
                binding.category.text.toString(),
            )
            val action = EditFeedFragmentDirections.actionEditFeedFragmentToFeedListFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Updates an existing Item in the database and navigates up to list fragment.
     */
    private fun updateFeed() {
        if (isEntryValid()) {
            viewModel.updateFeed(
                navigationArgs.feedId,
                binding.feedUrl.text.toString(),
                binding.feedName.text.toString(),
                binding.category.text.toString(),
            )
            val action = EditFeedFragmentDirections.actionEditFeedFragmentToFeedListFragment()
            findNavController().navigate(action)
        }
    }

    private fun deleteFeed() {
        if (isEntryValid()) {
            viewModel.deleteFeed(navigationArgs.feedId)
            val action = EditFeedFragmentDirections.actionEditFeedFragmentToFeedListFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Called when the view is created.
     * The itemId Navigation argument determines the edit item  or add new item.
     * If the itemId is positive, this method retrieves the information from the database and
     * allows the user to update it.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val feedId = navigationArgs.feedId
        if (feedId > 0) {
            viewModel.retrieveFeed(feedId).observe(this.viewLifecycleOwner) { selectedFeed ->
                feed = selectedFeed
                bind(feed)
            }
        } else {
            binding.saveAction.setOnClickListener {
                addNewFeed()
            }
            binding.deleteAction.isVisible = false
        }

        binding.loadOpmlAction.setOnClickListener {
            val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            val action = EditFeedFragmentDirections.actionEditFeedFragmentToFeedListFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Called before fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard.
        val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data //The uri with the location of the file
            Log.i("Activity", "got da data, uri: " + uri.toString())
            if (uri != null) {
                val inputStream = requireContext().contentResolver.openInputStream(uri);
                val feeds = OPMLParser().parse(inputStream!!)
                for (feed in feeds) {
                    viewModel.addNewFeed(feed)
                }
            }
        }
    }
}
