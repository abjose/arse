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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.alex.arse.data.Post
import com.alex.arse.databinding.PostPagerBinding


class ViewPagerFragment : Fragment() {
    private val viewModel: ArseViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as ArseApplication).database.postDao(),
            (activity?.application as ArseApplication).database.feedDao()
        )
    }

    private var _binding: PostPagerBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: ViewPagerFragmentArgs by navArgs()

    // Save current post state.
    private var currentPostId: Int = 0
    private var currentPostFeedId: Int = 0

    // TODO: Copied from PostListFragment.
    private var viewRead: Boolean = false
    private var sortAscending: Boolean = false
    private var feedName: String = ""
    var postLiveData: LiveData<List<Post>>? = null
    var postCountLiveData: LiveData<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        (activity as AppCompatActivity).supportActionBar!!.isHideOnContentScrollEnabled = true

        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PostPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewRead = readPref(getString(R.string.viewread_pref)) ?: false
        sortAscending = readPref(getString(R.string.sortascending_pref)) ?: false

        val adapter = ViewPagerAdapter(this.requireContext()) { postId, feedId ->
            Log.i("ViewPager", "updating post state: $postId, $feedId")
            currentPostId = postId
            currentPostFeedId = feedId
            markCurrentPostRead()
        }

        binding.viewPager.adapter = adapter
        // binding.viewPager.offscreenPageLimit = 3

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adapter.onPostViewed(position)
            }
        })

        setupObservers()

//        binding.floatingActionButton.setOnClickListener {
//            val action = ItemListFragmentDirections.actionItemListFragmentToAddItemFragment(
//                getString(R.string.add_fragment_title)
//            )
//            this.findNavController().navigate(action)
//        }
    }

    // TODO: Copied from PostListFragment.
    private fun setupObservers() {
        viewModel.retrieveFeedAndRunCallback(navigationArgs.feedIds[0]) { feed ->
            if (navigationArgs.feedIds.size == 1) {
                feedName = feed.name
            } else {
                feedName = feed.category
            }
            (activity as AppCompatActivity).supportActionBar!!.title = feedName
        }

        observePosts()
        observePostCount()
    }

    // TODO: Copied from PostListFragment.
    private fun observePosts() {
        Log.i("PostListFragment", "calling observeData, include_read: $viewRead, ascending: $sortAscending")

        postLiveData?.removeObservers(this.viewLifecycleOwner)
        postLiveData = viewModel.retrievePostsInFeeds(navigationArgs.feedIds, include_read = viewRead, ascending = sortAscending)
        postLiveData!!.observe(this.viewLifecycleOwner) { posts ->
            posts.let {
                Log.i("ViewPagerFragment", "in observeData, submitting list")
                (binding.viewPager.adapter as ViewPagerAdapter).submitList(it)
                setPosition(navigationArgs.postPosition)

                // Remove observer as soon as have data - don't want to redo as posts are marked read while we read.
                postLiveData?.removeObservers(this.viewLifecycleOwner)
            }
        }
    }

    private fun observePostCount() {
        postCountLiveData?.removeObservers(this.viewLifecycleOwner)
        postCountLiveData = viewModel.countUnreadPostsInFeedsLive(navigationArgs.feedIds)
        postCountLiveData!!.observe(this.viewLifecycleOwner) { count ->
            if (feedName != "") {
                (activity as AppCompatActivity).supportActionBar!!.title = feedName + " ($count)"
            }
        }
    }

    // TODO: Copied from PostListFragment.
    private fun readPref(key: String) : Boolean? {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return null
        return sharedPref.getBoolean(key, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_view_pager, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.mark_unread -> {
                Log.i("ViewPager", "hit mark unread button")
                markCurrentPostUnread()
                this.findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun markCurrentPostRead() {
        viewModel.markPostRead(currentPostId, currentPostFeedId)
    }

    private fun markCurrentPostUnread() {
        viewModel.markPostUnread(currentPostId, currentPostFeedId)
    }

    private fun setPosition(position: Int) {
        Log.i("ViewPager", "setting position in adapter to $position")
        if (position > 0) {
            binding.viewPager.setCurrentItem(position, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        (activity as AppCompatActivity).supportActionBar!!.isHideOnContentScrollEnabled = false
        // binding.viewPager.unregisterOnPageChangeCallback(this)
    }
}
