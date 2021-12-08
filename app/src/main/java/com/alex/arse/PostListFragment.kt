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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.arse.data.Post
import com.alex.arse.databinding.FragmentPostListBinding
import kotlinx.android.synthetic.main.fragment_post_list.*


class PostListFragment : Fragment() {
    private val viewModel: ArseViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as ArseApplication).database.postDao(),
            (activity?.application as ArseApplication).database.feedDao()
        )
    }

    private var _binding: FragmentPostListBinding? = null
    private val binding get() = _binding!!
    private val feedParserActivity = FeedParserActivity()
    private val navigationArgs: PostListFragmentArgs by navArgs()

    private var viewRead: Boolean = false
    private var sortAscending: Boolean = false

    // State for handling context menu choices.
    private var longClickedPostId: Int = 0
    private var longClickedFeedId: Int = 0
    private var longClickedPosition: Int = -1

    private var feedName: String = ""

    private var currentList: List<Post> = listOf()

    var postLiveData: LiveData<List<Post>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // registerForContextMenu(recyclerView)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun refresh() {
        // Toast.makeText(this.requireContext(), "Failed to load feed URL" , Toast.LENGTH_SHORT).show()

        swipe_refresh.isRefreshing = true
        Log.d("PostListFragment", "entering FeedParser")
        feedParserActivity.loadFeed(navigationArgs.feedIds, navigationArgs.feedUrls, requireContext(), viewModel, false) {
            Log.d("PostListFragment", "refresh is done")
            if (swipe_refresh != null) {
                swipe_refresh.isRefreshing = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update local preferencers. This should happen before update icons.
        viewRead = readPref(getString(R.string.viewread_pref)) ?: false
        sortAscending = readPref(getString(R.string.sortascending_pref)) ?: false

        // RecyclerView
        val adapter = PostListAdapter(navigationArgs.feedIds.size > 1, viewModel, { position: Int ->
            (activity as AppCompatActivity).supportActionBar!!.title = feedName
            val action = PostListFragmentDirections.actionPostListFragmentToViewPagerFragment(position, navigationArgs.feedIds, currentList.toTypedArray())
            this.findNavController().navigate(action)

        }, { postId: Int, feedId: Int, position: Int ->
            longClickedPostId = postId
            longClickedFeedId = feedId
            longClickedPosition = position

            // Can't get it to work the "real" way for some reason.
            requireActivity().openContextMenu(recyclerView)
        })
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.recyclerView.adapter = adapter

        registerForContextMenu(binding.recyclerView)

        // Move this stuff somewhere else
        // https://stackoverflow.com/questions/49827752/how-to-implement-drag-and-drop-and-swipe-to-delete-in-recyclerview
        val itemTouchHelperCallback =
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val postId = viewHolder.itemView.getTag(R.id.postId) as Int
                    val feedId = viewHolder.itemView.getTag(R.id.feedId) as Int
                    Log.v("SWIPED", postId.toString() + ", " + feedId);
                    viewModel.togglePostRead(postId, feedId)

                    if (viewRead) {
                        // TODO: be less lazy - can you just use notifyItemChanged?
                        adapter.notifyDataSetChanged()
                    }

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
                feedName = feed.name
            } else {
                feedName = feed.category
            }
            (activity as AppCompatActivity).supportActionBar!!.title = feedName
        }

        // Observe posts.
        observeData()

        // Observe unread count.
        viewModel.countUnreadPostsInFeedsLive(navigationArgs.feedIds).observe(this.viewLifecycleOwner) { count ->
            (activity as AppCompatActivity).supportActionBar!!.title = feedName + " ($count)"
        }

        binding.floatingActionButton.setOnClickListener {
            refresh()
        }
    }

    private fun observeData() {
        Log.i("PostListFragment", "calling observeData")

        postLiveData?.removeObservers(this.viewLifecycleOwner)
        postLiveData = viewModel.retrievePostsInFeeds(navigationArgs.feedIds, include_read = viewRead, ascending = sortAscending)
        postLiveData!!.observe(this.viewLifecycleOwner) { posts ->
            posts.let {
                Log.i("PostListFragment", "in observeData, submitting list")
                (binding.recyclerView.adapter as PostListAdapter).submitList(it)
                currentList = it
            }
        }
    }

    private fun writePref(key: String, value: Boolean) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    private fun readPref(key: String) : Boolean? {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return null
        return sharedPref.getBoolean(key, false)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_post_list_options, menu)

        // Ehhh
        if (viewRead) {
            menu.getItem(0).icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_visibility_24)
        } else {
            menu.getItem(0).icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_visibility_off_24)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toggle_view_read -> {
                Log.i("onOptionsItemSelected", "toggle_view_read")
                viewRead = !viewRead
                writePref(getString(R.string.viewread_pref), viewRead)
                if (viewRead) {
                    item.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_visibility_24)
                } else {
                    item.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_visibility_off_24)
                }
                observeData()
                true
            }
            R.id.toggle_ascending -> {
                sortAscending = !sortAscending
                writePref(getString(R.string.sortascending_pref), sortAscending)
                observeData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = requireActivity().menuInflater
        inflater.inflate(R.menu.fragment_post_list_context, menu)
    }

    // If above is true, will go backwards; otherwise forwards.
    private fun bulkMarkRead(above: Boolean) {
        if (currentList.isNotEmpty() && longClickedPosition >= 0) {
            // Log.i("PostListFragment", "$longClickedPosition, ${currentList.size}")
            var range = longClickedPosition+1 until currentList.size
            if (above) {
                range = 0 until longClickedPosition
            }
            for (i in range) {
                viewModel.markPostRead(currentList[i].postId, currentList[i].feedId)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toggle_read -> {
                viewModel.togglePostRead(longClickedPostId, longClickedFeedId)
                true
            }

            R.id.mark_above_read -> {
                bulkMarkRead(true)
                observeData()
                true
            }
            R.id.mark_below_read -> {
                bulkMarkRead(false)
                observeData()
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }
}
