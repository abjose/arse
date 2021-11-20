package com.alex.arse

import com.alex.arse.data.Feed
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

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
            if (parser.eventType == XmlPullParser.END_TAG) {
                // Not sure this is the best way to detect end of category...
                val url = parser.getAttributeValue(null, "xmlUrl")
                if (url == null) {
                    category = null
                }
                // val text = parser.getAttributeValue(null, "text")
                // Log.i(TAG, "end tag: $url, $text")
                parser.next()
            }
            else if (parser.eventType != XmlPullParser.START_TAG) {
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
            return Feed(0, url, name, htmlUrl, category as String)
        }
        // parser.require(XmlPullParser.END_TAG, ns, "content")
        return Feed(0, url, name, htmlUrl, "Uncategorized")
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