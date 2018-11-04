package mobi.upod.syndication;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.FetcherEvent;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.FetcherException;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import mobi.upod.net.HttpURLConnector;
import scala.Option;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class ProgressiveFeedFetcher extends AbstractFeedFetcher {
    static final int POLL_EVENT = 1;
    static final int RETRIEVE_EVENT = 2;
    static final int UNCHANGED_EVENT = 3;

    /**
     * Constructor to use HttpURLFeedFetcher without caching of feeds
     *
     */
    public ProgressiveFeedFetcher() {
        super();
    }

    public ProgressiveFeedFetcher(String userAgent) {
        super();
        setUserAgent(userAgent);
    }

    public SyndFeed retrieveFeed(URL feedUrl) throws IllegalArgumentException, IOException, FeedException, FetcherException {
        return this.retrieveFeed(this.getUserAgent(), feedUrl);
    }

    public SyndFeed retrieveFeed(String userAgent, URL url) throws IllegalArgumentException, IOException, FeedException, FetcherException {
        return retrieveFeedIfChanged(userAgent, url, Option.<CacheInfo>empty()).get().feed();
    }

    public Option<FeedWithCacheInfo> retrieveFeedIfChanged(URL feedUrl, CacheInfo cacheInfo) throws Exception {
        return retrieveFeedIfChanged(getUserAgent(), feedUrl, Option.apply(cacheInfo));
    }

    public FeedWithCacheInfo retrieveFeedWithCacheInfo(URL feedUrl) throws Exception {
        return retrieveFeedIfChanged(getUserAgent(), feedUrl, Option.<CacheInfo>empty()).get();
    }

    private Option<FeedWithCacheInfo> retrieveFeedIfChanged(String userAgent, URL feedUrl, Option<CacheInfo> cacheInfo) throws IllegalArgumentException, IOException, FeedException, FetcherException {
        if (feedUrl == null) {
            throw new IllegalArgumentException("null is not a valid URL");
        }

        HttpURLConnector connector = new HttpURLConnector(feedUrl).
                setConnectTimeout(30000).
                setReadTimeout(60000);
        setRequestHeaders(connector, cacheInfo);
        connector.addRequestProperty("User-Agent", userAgent);

        if (cacheInfo.isDefined()) {
            final HttpURLConnection connection = connector.connect();
            try {
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, connection);

                // check the response code
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    // the response code is not 304 NOT MODIFIED
                    // This is either because the feed server
                    // does not support condition gets
                    // or because the feed hasn't changed
                    return Option.apply(retrieveFeed(feedUrl, connection));
                } else {
                    // the feed does not need retrieving
                    fireEvent(FetcherEvent.EVENT_TYPE_FEED_UNCHANGED, connection);
                    return Option.empty();
                }
            } finally {
                connection.disconnect();
            }
        } else {
            InputStream inputStream = null;
            final HttpURLConnection connection = connector.connect();
            connection.connect();
            try {
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, connection);
                inputStream = connection.getInputStream();
                final CacheInfo ci = CacheInfo.apply(connection);
                final SyndFeed feed = getSyndFeedFromStream(inputStream, connection);
                return Option.apply(new FeedWithCacheInfo(feed, false, ci));
            } catch (java.io.IOException e) {
                handleErrorCodes(connection.getResponseCode());
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                connection.disconnect();
            }
            // we will never actually get to this line
            return Option.empty();
        }
    }

    protected FeedWithCacheInfo retrieveFeed(URL feedUrl, HttpURLConnection connection) throws IllegalArgumentException, FeedException, FetcherException, IOException {
        handleErrorCodes(connection.getResponseCode());
        final CacheInfo cacheInfo = CacheInfo.apply(connection);

        // get the contents
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            SyndFeed syndFeed = getSyndFeedFromStream(inputStream, connection);

            String imHeader = connection.getHeaderField("IM");
            boolean onlyNewEpisodes = isUsingDeltaEncoding() && (imHeader!= null && imHeader.indexOf("feed") >= 0) && (connection.getResponseCode() == 226);
            return new FeedWithCacheInfo(syndFeed, onlyNewEpisodes, cacheInfo);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    protected void setRequestHeaders(HttpURLConnector connector, Option<CacheInfo> cacheInfo) {
        if (cacheInfo.isDefined()) {
            cacheInfo.get().applyToConnector(connector);
        }

        // header to retrieve feed gzipped
        connector.setRequestProperty("Accept-Encoding", "gzip");

        if (isUsingDeltaEncoding()) {
            connector.addRequestProperty("A-IM", "feed");
        }
    }

    private SyndFeed readSyndFeedFromStream(InputStream inputStream, URLConnection connection) throws IOException, IllegalArgumentException, FeedException {
        BufferedInputStream is;
        if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
            // handle gzip encoded content
            is = new BufferedInputStream(new GZIPInputStream(inputStream));
        } else {
            is = new BufferedInputStream(inputStream);
        }

        //InputStreamReader reader = new InputStreamReader(is, ResponseHandler.getCharacterEncoding(connection));

        //SyndFeedInput input = new SyndFeedInput();

        XmlReader reader = null;
        if (connection.getHeaderField("Content-Type") != null) {
            reader = new XmlReader(is, connection.getHeaderField("Content-Type"), true, "UTF-8");
        } else {
            reader = new XmlReader(is, true, "UTF-8");
        }

        SyndFeedInput syndFeedInput = new SyndFeedInput();
        syndFeedInput.setPreserveWireFeed(isPreserveWireFeed());

        return syndFeedInput.build(reader);

    }

    private SyndFeed getSyndFeedFromStream(InputStream inputStream, URLConnection connection) throws IOException, IllegalArgumentException, FeedException {
        SyndFeed feed = readSyndFeedFromStream(inputStream, connection);
        fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, connection, feed);
        return feed;
    }

    @Override
    protected void handleErrorCodes(int responseCode) throws FetcherException {
        HttpStatusException$.MODULE$.apply(responseCode);
    }
}
