package com.example.inventory

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.inventory.data.Item
// import com.example.inventory.databinding.FragmentItemDetailBinding
import com.example.inventory.databinding.PagerDetailBinding
import com.example.inventory.databinding.ItemListItemBinding
import kotlinx.android.synthetic.main.item_pager.view.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

// Helpful page for setting up ViewPager: https://g403.co/android-viewpager2/
class ViewPagerAdapter(private val markItemRead: (postId: Int) -> Unit) :
    ListAdapter<Item, ViewPagerAdapter.ItemDetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDetailViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PagerDetailBinding.inflate(layoutInflater, parent, false)

        return ItemDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemDetailViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        // holder.itemView.setTag(current.id)
    }

    fun onPostviewed(position: Int) {
        val item = getItem(position)
        Log.i("ViewPager", "onPostViewed: ${item.postId}")
        markItemRead(item.postId)
    }

    override fun getItemViewType(position: Int) = R.layout.pager_detail

    class ItemDetailViewHolder(private var binding: PagerDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.itemName.text = item.title
            // binding.content.text = HtmlCompat.fromHtml(item.content, 0)
            // binding.content.settings.javaScriptEnabled = true
            // binding.content.settings.loadWithOverviewMode = true
            // binding.content.settings.useWideViewPort = true
            val imageCss = "<style>img{display: inline;height: auto;width: auto;max-width: 100%;}</style>"
            binding.content.loadDataWithBaseURL(null, imageCss + item.content,"text/html", "UTF-8", null)
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
