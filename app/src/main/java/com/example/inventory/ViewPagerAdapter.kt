package com.example.inventory

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.data.Item
import com.example.inventory.databinding.PagerDetailBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

// Helpful page for setting up ViewPager: https://g403.co/android-viewpager2/
class ViewPagerAdapter(private val context: Context, private val updateCurrentPost: (postId: Int, feedId: Int) -> Unit) :
    ListAdapter<Item, ViewPagerAdapter.ItemDetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDetailViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PagerDetailBinding.inflate(layoutInflater, parent, false)

        return ItemDetailViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: ItemDetailViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        // holder.itemView.setTag("feedId".hashCode(), current.feedId)
    }

    fun onPostViewed(position: Int) {
        val item = getItem(position)
        Log.i("ViewPager", "onPostViewed: ${item.postId}")
        updateCurrentPost(item.postId, item.feedId)
    }

    override fun getItemViewType(position: Int) = R.layout.pager_detail

    class ItemDetailViewHolder(private var binding: PagerDetailBinding, private val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.itemName.text = item.title
            binding.itemAuthor.text = item.author

            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm")
            binding.itemDate.text = sdf.format(Date(item.timestamp))

            binding.itemName.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                startActivity(context, browserIntent, null)
            }

            // binding.content.text = HtmlCompat.fromHtml(item.content, 0)
            // binding.content.settings.javaScriptEnabled = true
            // binding.content.settings.loadWithOverviewMode = true
            // binding.content.settings.useWideViewPort = true
            val imageCss = "<style>img{display: inline;height: auto;width: auto;max-width: 100%;}</style>"
            binding.content.loadDataWithBaseURL(null, imageCss + item.content,"text/html", "UTF-8", null)

            binding.content.setOnLongClickListener {
                val result = (it as WebView).hitTestResult
                if (result.type == WebView.HitTestResult.ANCHOR_TYPE || result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    // If target is link, try sharing.
                    Log.v("WebView", "it's a link! " + result.extra)

                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.type="text/plain"
                    //shareIntent.type="text/html"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, result.extra)
                    startActivity(context, Intent.createChooser(shareIntent, "Share Link"), null)

                    true
                } else {
                    // Otherwise, handle some other way
                    false
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                // return oldItem.postId == newItem.postId && oldItem.feedUrl == newItem.feedUrl
                return oldItem.postId == newItem.postId
            }
        }
    }
}
