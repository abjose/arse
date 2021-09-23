package com.example.inventory

import android.app.Activity
import android.util.Log
import com.example.inventory.data.Feed
import com.example.inventory.data.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

// We don't use namespaces
private val ns: String? = null

class OPMLParser() {
    var TAG = "OPMLParser"

    private var category: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<Feed> {
        // Log.i(TAG, "i wanna parse")
        inputStream.use { inputStream ->
            // val parser: XmlPullParser = Xml.newPullParser()
            val parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            //parser.setInput(inputStream, "utf-8")
            parser.nextTag()
            return readOPML(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readOPML(parser: XmlPullParser): List<Feed> {
        val feeds = mutableListOf<Feed>()

        // parser.require(XmlPullParser.START_TAG, ns, "feed")
        // while (parser.next() != XmlPullParser.END_TAG) {
        // var eventType = parser.eventType
        // while (eventType != XmlPullParser.END_DOCUMENT) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                // Log.i(TAG, "continuing, eventType: " + parser.eventType)
                parser.next()
            } else if (parser.name == "body" || parser.name == "opml") {
                // Log.i(TAG, "body")
                parser.nextTag()
            } else if (parser.name == "outline") {
                // Log.i(TAG, "reading outline")
                val maybeFeed: Feed? = readOutline(parser)
                if (maybeFeed != null) {
                    feeds.add(maybeFeed)
                }
                parser.nextTag()
            } else {
                // Log.i(TAG, "skipping, name: " + parser.name)
                skip(parser)
            }
        }
        return feeds
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readOutline(parser: XmlPullParser): Feed? {
        parser.require(XmlPullParser.START_TAG, ns, "outline")

        val url = parser.getAttributeValue(null, "xmlUrl")
        if (url == null) {
            // This is probably a category - for now, only allow one level of hierarchy.
            category = parser.getAttributeValue(null, "text")
            // Log.i("OPMLParser", "category: $category")
            return null
        }

        // Otherwise, this is a child node / actual feed.
        val name = parser.getAttributeValue(null, "text")
        var htmlUrl = parser.getAttributeValue(null, "htmlUrl")
        if (htmlUrl == null) {
            htmlUrl = ""
        }
        if (category != null) {
            // Log.i("OPMLParser", "Adding feed: $url, $name, $category")
            return Feed(url, name, htmlUrl, category as String)
        }
        // parser.require(XmlPullParser.END_TAG, ns, "content")
        return Feed(url, name, htmlUrl, "Uncategorized")
    }


    // Processes link tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        var link = ""
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val tag = parser.name
        // val relType = parser.getAttributeValue(null, "rel")
        if (tag == "link") {
            val href = parser.getAttributeValue(null, "href")
            if (href != null) {
                link = href
                parser.nextTag()
            } else {
                link = readText(parser)
            }
        }

        if (parser.eventType != XmlPullParser.END_TAG) {
            skip(parser)
        }
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            // Log.i(TAG, "in skip: " + parser.name)
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}

class NetworkActivity : Activity() {

    companion object {

        const val WIFI = "Wi-Fi"
        const val ANY = "Any"

//         const val URL = "http://stackoverflow.com/feeds/tag?tagnames=android&sort=newest"
//         const val URL = "https://freethoughtblogs.com/feed/"
//         const val URL = "https://ranprieur.com/feed"
//         const val URL = "https://pbfcomics.com/feed/"
//         const val URL = "https://www.notechmagazine.com/feed"

        // Whether there is a Wi-Fi connection.
        private var wifiConnected = true
        // Whether there is a mobile connection.
        private var mobileConnected = false

        // Whether the display should be refreshed.
        var refreshDisplay = true
        // The user's current network preference setting.
        // var sPref: String? = null
        var sPref: String? = ANY
    }

    val TAG = "NetworkActivity"

    // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
    // Uses AsyncTask to download the XML feed from stackoverflow.com.
    fun loadPage(viewModel: InventoryViewModel, url: String) {
        // Log.i(TAG, "in LoadPage")
        if (sPref.equals(ANY) && (wifiConnected || mobileConnected)) {
            // Log.i(TAG, "running 1st one")
            loadXmlFromNetwork(url, viewModel)
            // DownloadXmlTask().execute(URL)
        } else if (sPref.equals(WIFI) && wifiConnected) {
            // Log.i(TAG, "running 2nd one")
            loadXmlFromNetwork(url, viewModel)
            // DownloadXmlTask().execute(URL)
        } else {
            // show error
            Log.i(TAG, "didn't do any of them")
        }
    }

    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
//    private inner class DownloadXmlTask : AsyncTask<String, Void, String>() {
//        override fun doInBackground(vararg urls: String): String {
//            // return try {
//            try {
//                loadXmlFromNetwork(urls[0])
//            } catch (e: IOException) {
//                resources.getString(R.string.connection_error)
//            } catch (e: XmlPullParserException) {
//                resources.getString(R.string.xml_error)
//            }
//        }
//
//        override fun onPostExecute(result: String) {
//            setContentView(R.layout.main)
//            // Displays the HTML string in the UI via a WebView
//            findViewById<WebView>(R.id.webview)?.apply {
//                loadData(result, "text/html", null)
//            }
//        }
//    }

//    // Uploads XML from stackoverflow.com, parses it, and combines it with
//    // HTML markup. Returns HTML string.
//    @Throws(XmlPullParserException::class, IOException::class)
//    private fun loadXmlFromNetwork(urlString: String): String {
//        // Checks whether the user set the preference to include summary text
//        val pref: Boolean = PreferenceManager.getDefaultSharedPreferences(this)?.run {
//            getBoolean("summaryPref", false)
//        } ?: false
//
//        val entries: List<Entry> = downloadUrl(urlString)?.use { stream ->
//            // Instantiate the parser
//            StackOverflowXmlParser().parse(stream)
//        } ?: emptyList()
//
//        return StringBuilder().apply {
//            append("<h3>${resources.getString(R.string.page_title)}</h3>")
//            append("<em>${resources.getString(R.string.updated)} ")
//            append("${formatter.format(rightNow.time)}</em>")
//            // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
//            // Each Entry object represents a single post in the XML feed.
//            // This section processes the entries list to combine each entry with HTML markup.
//            // Each entry is displayed in the UI as a link that optionally includes
//            // a text summary.
//            entries.forEach { entry ->
//                append("<p><a href='")
//                append(entry.link)
//                append("'>" + entry.title + "</a></p>")
//                // If the user set the preference to include summary text,
//                // adds it to the display.
//                if (pref) {
//                    append(entry.summary)
//                }
//            }
//        }.toString()
//    }

    private fun loadXmlFromNetwork(urlString: String, viewModel: InventoryViewModel) {
        GlobalScope.launch(Dispatchers.IO) {
            val entries: List<Item> = downloadUrl(urlString)?.use { stream ->
                // Instantiate the parser
//                val streamAsString = stream.bufferedReader().use { it.readText() }
//                Log.i(TAG, "stream: " + streamAsString)
                FeedParser(urlString).parse(stream)
            } ?: emptyList()

            Log.i(TAG, "# entries: " + entries.size)
            for (entry in entries) {

                // Log.i(TAG, entry.title + " ... " + entry.description)// + " ... " + entry.link)
                Log.i(TAG, entry.title)
                // blah

                // adapter.addItem(entry)
                viewModel.addNewItem(entry)
            }
//            runOnUiThread {
//                adapter.notifyDataSetChanged()
//                // adapter.notifyItemInserted()
//            }
            //adapter.refresh()
        }
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        return URL(urlString).openConnection().getInputStream()

//        val url = URL(urlString)
//        return (url.openConnection() as? HttpURLConnection)?.run {
//            readTimeout = 10000
//            connectTimeout = 15000
//            requestMethod = "GET"
//            doInput = true
//            // Starts the query
//            connect()
//            inputStream
//        }
    }
}