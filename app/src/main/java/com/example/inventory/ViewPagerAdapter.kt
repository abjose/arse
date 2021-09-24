package com.example.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.data.Item
// import com.example.inventory.databinding.FragmentItemDetailBinding
import com.example.inventory.databinding.PagerDetailBinding
import com.example.inventory.databinding.ItemListItemBinding

/**
 * [ListAdapter] implementation for the recyclerview.
 */

// Helpful page for setting up ViewPager: https://g403.co/android-viewpager2/
class ViewPagerAdapter(): ListAdapter<Item, ViewPagerAdapter.ItemDetailViewHolder>(DiffCallback) {

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

//    @SuppressLint("NewApi")
//    private openWebView() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
//        } else {
//            webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
//        }
//        String data = "<div> your HTML content </div>";
//        webView.loadDataWithBaseURL("file:///android_asset/", getHtmlData(data), "text/html", "utf-8", null);
//    }
//
//    private String getHtmlData(String bodyHTML) {
//        String head = "<head><style>img{max-width: 100%; width:auto; height: auto;}</style></head>";
//        return "<html>" + head + "<body>" + bodyHTML + "</body></html>";
//    }

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
