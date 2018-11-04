package mobi.upod.syndication

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed

case class FeedWithCacheInfo(feed: SyndFeed, newEpisodesOnly: Boolean, cacheInfo: CacheInfo)
