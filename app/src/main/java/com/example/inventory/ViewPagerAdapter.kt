package com.example.inventory

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.core.content.ContextCompat.startActivity
import androidx.core.text.HtmlCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.inventory.data.Item
// import com.example.inventory.databinding.FragmentItemDetailBinding
import com.example.inventory.databinding.PagerDetailBinding
import com.example.inventory.databinding.ItemListItemBinding
import kotlinx.android.synthetic.main.item_pager.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * [ListAdapter] implementation for the recyclerview.
 */

// Helpful page for setting up ViewPager: https://g403.co/android-viewpager2/
class ViewPagerAdapter(private val context: Context, private val markItemRead: (postId: Int) -> Unit) :
    ListAdapter<Item, ViewPagerAdapter.ItemDetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDetailViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PagerDetailBinding.inflate(layoutInflater, parent, false)

        return ItemDetailViewHolder(binding, context)
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
