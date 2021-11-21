package com.alex.arse

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.alex.arse.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.lang.Math.min
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

// We don't use namespaces
private val ns: String? = null

// Likely a good reference:
// https://github.com/prof18/RSS-Parser/blob/master/rssparser/src/main/java/com/prof/rssparser/core/CoreXMLParser.kt

// Probably better way to pass in url?
class FeedParser(private val feedId: Int) {
    var TAG = "FeedParser"

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<Post> {
        inputStream.use { inputStream ->
            // get rid of leading whitespace :'(
            var inputAsString = inputStream.bufferedReader().use { it.readText() }
            inputAsString = inputAsString.trim()
            // Log.i(TAG, inputAsString)
            val newStream: InputStream = inputAsString.byteInputStream()

            // val parser: XmlPullParser = Xml.newPullParser()
            val parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            // parser.setInput(inputStream, null)
            parser.setInput(newStream, "UTF-8")
            //parser.setInput(inputStream, "utf-8")
            parser.nextTag()
            return readFeed(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<Post> {
        val entries = mutableListOf<Post>()

        // parser.require(XmlPullParser.START_TAG, ns, "feed")
        // while (parser.next() != XmlPullParser.END_TAG) {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                Log.d(TAG, "continuing, eventType: " + parser.eventType)
                continue
            }
            // Starts by looking for the entry tag
            // if (parser.name == "entry") {
            if (parser.name == "channel") {
                Log.d(TAG, "channel; continuing")
                parser.next()
            } else if (parser.name == "item" || parser.name == "entry") {
                Log.d(TAG, "reading item/entry")
                entries.add(readEntry(parser))
            } else {
                Log.d(TAG, "skipping, name: " + parser.name)
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): Post {
        // parser.require(XmlPullParser.START_TAG, ns, "item")
        var title: String? = null
        var author: String? = null
        var description: String? = null
        var link: String? = null
        var postId: Int? = null
        var timestamp: Long? = 0
        var content: String? = null

        val startDepth = parser.depth
        while (parser.next() != XmlPullParser.END_TAG || parser.depth > startDepth) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (simplifyTag(parser.name)) {
                "id" -> postId = readId(parser)
                "title" -> title = readTitle(parser)
                "author" -> author = readAuthor(parser)
                "description" -> description = readDescription(parser)
                "link" -> link = readLink(parser)
                "pubDate" -> timestamp = readTimestamp(parser)
                "content" -> content = readContent(parser, content)
                else -> skip(parser)
            }
        }

        // where to get this from? title of site?
        // should be able to pass it in somehow I think

        var TAG = "PARSER"
        if (postId != null) {
            Log.i(TAG, "postId: " + postId!!.toString())
        } else {
            Log.i(TAG, "No postId found")
        }

        if (title != null) {
            Log.i(TAG, "title: " + title!!)
        } else {
            Log.i(TAG, "No title found")
        }

        if (author != null) {
            Log.i(TAG, "author: " + author!!)
        } else {
            Log.i(TAG, "No author found")
        }

        if (link != null) {
            Log.i(TAG, "link: " + link!!)
        } else {
            Log.i(TAG, "No link found")
        }

        if (timestamp != null) {
            Log.i(TAG, "timestamp: " + timestamp!!.toString())
        } else {
            Log.i(TAG, "No timestamp found")
        }

        if (description != null) {
            val len = min(description.length, 100)
            Log.i(TAG, "description: " + description!!.substring(0, len))
        } else {
            Log.i(TAG, "No description found")
        }

        if (content != null) {
            val len = min(content.length, 100)
            Log.i(TAG, "content: " + content!!.substring(0, len))
        } else {
            Log.i(TAG, "No content found")
        }

        // Populate postId if still null.
        if (postId == null) {
            postId = (title + timestamp.toString()).hashCode()
        }
        if (content == null && description != null) {
            Log.i(TAG, "Overwriting content")
            content = description
        }

        return Post(feedId = feedId, postId = postId!!, title = title ?: "(no title)", author = author ?: "",
            link = link ?: "", timestamp = timestamp!!, content = content ?: "", read = false)
    }

    private fun simplifyTag(tag: String): String {
        return when {
            tag.contains("id") -> "id"
            tag.contains("creator") -> "author"
            tag.contains("date") -> "pubDate"
            tag.contains("content") && !tag.contains("media") -> "content"
            else -> tag
        }
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "title")
        return title
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

    // Processes summary tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAuthor(parser: XmlPullParser): String {
        // parser.require(XmlPullParser.START_TAG, ns, "description")
        val startDepth = parser.depth
        var author = readText(parser)

        // Handle feeds with tags nested inside Author
        while (parser.depth > startDepth) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            if (parser.name == "name") {
                author = readText(parser)
                break
            }

        }
        // parser.require(XmlPullParser.END_TAG, ns, "description")
        return author
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readDescription(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "description")
        val description = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "description")
        return description
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readContent(parser: XmlPullParser, oldContent: String?): String {
        // parser.require(XmlPullParser.START_TAG, ns, "content")
        val content = readText(parser)
        // parser.require(XmlPullParser.END_TAG, ns, "content")
        if (content.isBlank() && oldContent != null) {
            return oldContent
        }
        return content
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readId(parser: XmlPullParser): Int {
        // parser.require(XmlPullParser.START_TAG, ns, "description")
        val idText = readText(parser)
        // parser.require(XmlPullParser.END_TAG, ns, "description")
        return idText.hashCode()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTimestamp(parser: XmlPullParser): Long {
        // parser.require(XmlPullParser.START_TAG, ns, "description")
        val dateString = readText(parser)
        val timestamp = parseDate(dateString)
        // parser.require(XmlPullParser.END_TAG, ns, "description")
        Log.i("PARSER", "datestring: " + dateString + " -> timestamp: " + timestamp)
        return timestamp
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

fun parseDate(dateString: String): Long {
    val formats: List<SimpleDateFormat> = listOf(
        // SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:Z", Locale.ENGLISH),
        // SimpleDateFormat("MM-dd-yyyy"),
        // SimpleDateFormat("yyyyMMdd"),
        // SimpleDateFormat("MM/dd/yyyy"),
    )

    for (format in formats) {
        try {
            val parsedDate = format.parse(dateString)
            return parsedDate.time
        } catch (e: ParseException) {
            continue
        }
    }

    return -1
}

class NetworkActivity : Activity() {

    companion object {

        const val WIFI = "Wi-Fi"
        const val ANY = "Any"

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
    fun loadFeed(feedIds: IntArray, feedUrls: Array<String>, context: Context, viewModel: ArseViewModel, doneCallback: () -> Unit) {
        assert(feedIds.size == feedUrls.size)
        if (sPref.equals(ANY) && (wifiConnected || mobileConnected)) {
            Log.i(TAG, "loading $1{feedIds.size} feed(s)")
            for (i in feedIds.indices) {
                loadXmlFromNetwork(feedIds[i], feedUrls[i], context, viewModel, doneCallback)
            }
        }
//        else if (sPref.equals(WIFI) && wifiConnected) {
//            // Log.i(TAG, "running 2nd one")
//            loadXmlFromNetwork(feedId, feedUrl, context, viewModel)
//            // DownloadXmlTask().execute(URL)
//        } else {
//            // show error
//            Log.i(TAG, "didn't do any of them")
//        }
    }

    private fun loadXmlFromNetwork(feedId: Int, feedUrl: String, context: Context, viewModel: ArseViewModel, doneCallback: () -> Unit) {
        Log.i(TAG, "in loadXmlFromNetwork, loading $feedUrl")
        GlobalScope.launch(Dispatchers.IO) {
            var entries: List<Post> = emptyList()

//            entries = downloadUrl(feedUrl)?.use { stream ->
//                FeedParser(feedId).parse(stream)
//            } ?: emptyList()

            try {
                entries = downloadUrl(feedUrl)?.use { stream ->
                    FeedParser(feedId).parse(stream)
                } ?: emptyList()
            } catch (e: XmlPullParserException) {
                runOnUiThread {
                    Toast.makeText(context, "Failed to parse feed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                runOnUiThread {
                    Toast.makeText(context, "Couldn't load feed", Toast.LENGTH_SHORT).show()
                }
            }

            Log.i(TAG, "# entries: " + entries.size)
            for (entry in entries) {
                Log.i(TAG, entry.title)
                viewModel.addNewPost(entry)
            }

            runOnUiThread {
                doneCallback()
            }
        }
    }

    @Throws(IOException::class)
    private fun openConnection(initialUrl: String): HttpURLConnection? {
        var url = initialUrl
        for (i in 1..10) {
            val connection = URL(url).openConnection() as HttpURLConnection

            var code: Int
            try {
                // For some reason this is crashing if page doesn't exist.
                code = connection.responseCode
            } catch (e: Exception) {
                Log.i(TAG, "Can't read responseCode? Page probably doesn't exist.")
                return null
            }

            if (code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER) {
                url = connection.getHeaderField("Location")
                Log.i(TAG, "redirecting to $url")
                connection.disconnect()
            } else {
                Log.i(TAG, "returning connection to $url")
                return connection
            }
        }

        return null
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val connection = openConnection(urlString)
        if (connection != null) {
            return connection.inputStream
        }
        return null
        // val connection = URL(urlString).openConnection() as HttpURLConnection
        // connection.instanceFollowRedirects = true
    }
}