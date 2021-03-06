package com.alex.arse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebViewFeature
import com.alex.arse.data.Post
import com.alex.arse.databinding.PagerDetailBinding
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

// Helpful page for setting up ViewPager: https://g403.co/android-viewpager2/
class ViewPagerAdapter(private val context: Context, private val updateCurrentPost: (post: Post) -> Unit) :
    ListAdapter<Post, ViewPagerAdapter.ItemDetailViewHolder>(DiffCallback) {

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
        val post = getItem(position)
        Log.i("ViewPager", "onPostViewed: ${post.postId}")
        updateCurrentPost(post)
    }

    override fun getItemViewType(position: Int) = R.layout.pager_detail

    class ItemDetailViewHolder(private var binding: PagerDetailBinding, private val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.postName.text = post.title

            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm")
            val tab = "????????"
            val dateAndAuthor = post.author + tab + tab + tab + sdf.format(Date(post.timestamp))
            binding.postAuthorAndDate.text = dateAndAuthor

            binding.postName.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                startActivity(context, browserIntent, null)
            }

            // TODO: support light mode.
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(binding.content.settings, FORCE_DARK_ON)
            }

            // binding.content.text = HtmlCompat.fromHtml(item.content, 0)
            // binding.content.settings.javaScriptEnabled = true
            // binding.content.settings.loadWithOverviewMode = true
            // binding.content.settings.useWideViewPort = true

            // Hack to try to keep webview from flashing white in darkmode.
            binding.content.setBackgroundColor(Color.argb(1, 0, 0, 0));

            var url = URL(post.link)
            // Log.v("Woof", "${url.protocol}://${url.host}")
            val imageCss = "<style>img{display: inline;height: auto;width: auto;max-width: 100%;}</style>"
            binding.content.loadDataWithBaseURL("${url.protocol}://${url.host}", imageCss + post.content,"text/html", "UTF-8", null)

            binding.content.setOnLongClickListener {
                val result = (it as WebView).hitTestResult
                if (result.type == WebView.HitTestResult.ANCHOR_TYPE || result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    // If target is link, try sharing.
                    // Log.v("WebView", "it's a link! " + result.extra)

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

            // Try to fix issue where ScrollViews will be partially scrolled (due to recycling?)
            // binding.scrollView.fullScroll(View.FOCUS_UP)
            // Still not quite working, another one to try: https://stackoverflow.com/a/48621014
            binding.scrollView.fling(0);
            binding.scrollView.smoothScrollTo(0, 0);
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldPost: Post, newPost: Post): Boolean {
                return oldPost.postId == newPost.postId
            }

            override fun areContentsTheSame(oldPost: Post, newPost: Post): Boolean {
                // return oldItem.postId == newItem.postId && oldItem.feedUrl == newItem.feedUrl
                return oldPost.postId == newPost.postId
            }
        }
    }
}
