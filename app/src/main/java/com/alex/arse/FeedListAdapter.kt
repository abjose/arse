package com.alex.arse

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import com.alex.arse.data.Feed
import java.util.*
import kotlin.collections.ArrayList

class FeedListAdapter internal constructor(private val context: Context,
                                           private val onTextClick: (position: Int) -> Unit,
                                           private val onIndicatorClick: (position: Int, isExpanded: Boolean) -> Unit,
                                           private val populateUnreadCount: (textview: TextView, feed: Feed) -> Unit) :
    BaseExpandableListAdapter() {

    private var categoryList: List<String> = listOf()
    private var dataList: SortedMap<String, MutableList<Feed>> = sortedMapOf(compareBy<String> {
        it.toLowerCase()
    })

    fun setData(dataList: SortedMap<String, MutableList<Feed>>) {
        // TODO: don't split apart...
        this.dataList = dataList
        this.categoryList = ArrayList(dataList.keys)
        notifyDataSetChanged()
    }

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.categoryList[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val expandedListFeed = getChild(listPosition, expandedListPosition) as Feed
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.feed_list_item, null)
        }
        val expandedListTextView = convertView!!.findViewById<TextView>(R.id.expandedListItem)
        expandedListTextView.text = expandedListFeed.name
        val expandedListUnreadCount = convertView!!.findViewById<TextView>(R.id.feedlistUnreadCount)
        populateUnreadCount(expandedListUnreadCount, expandedListFeed)
        return convertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.categoryList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.categoryList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.categoryList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val listTitle = getGroup(listPosition) as String
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.feed_list_group, null)
        }
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.listTitle)
        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle
        listTitleTextView.setOnClickListener {
            onTextClick(listPosition)
        }

        val indicator = convertView!!.findViewById<ImageView>(R.id.groupIndicatorArea)
        indicator.setOnClickListener {
            onIndicatorClick(listPosition, isExpanded)
        }

        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }
}
