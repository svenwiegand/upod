package mobi.upod.net;

import android.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpURLConnector {
    private static final int MAX_REDIRECT_COUNT = 5;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final List<Pair<String, String>> setRequestProperties = new ArrayList<>();
    private final List<Pair<String, String>> addedRequestProperties = new ArrayList<>();
    private URL url = null;
    private Integer connectionTimeout = null;
    private Integer readTimeout = null;
    private Long ifModifiedSince = null;

    public HttpURLConnector(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public HttpURLConnector setConnectTimeout(int timeoutMillis) {
        connectionTimeout = timeoutMillis;
        return this;
    }

    public HttpURLConnector setReadTimeout(int timeoutMillis) {
        readTimeout = timeoutMillis;
        return this;
    }

    public HttpURLConnector setIfModifiedSince(long newValue) {
        this.ifModifiedSince = newValue;
        return this;
    }

    public HttpURLConnector setRequestProperty(String field, String newValue) {
        setRequestProperties.add(new Pair<>(field, newValue));
        return this;
    }

    public HttpURLConnector addRequestProperty(String field, String newValue) {
        addedRequestProperties.add(new Pair<>(field, newValue));
        return this;
    }

    public HttpURLConnection connect() throws IOException {
        return connect(0);
    }

    private HttpURLConnection connect(int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECT_COUNT)
            throw new IOException("Maximum number of redirects exceeded");

        final HttpURLConnection connection = buildConnection();
        log.debug("Connecting to {}", url);
        connection.connect();
        final int responseCode = connection.getResponseCode();
        switch (responseCode) {
            case HttpURLConnection.HTTP_MULT_CHOICE:
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
            case HttpURLConnection.HTTP_USE_PROXY:
            case 307:
            case 308:
                url = new URL(connection.getHeaderField("Location"));
                log.debug("Redirected ({}) to {}", responseCode, url);
                connection.disconnect();
                return connect(redirectCount + 1);
            default:
                return connection;
        }
    }

    private HttpURLConnection buildConnection() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connectionTimeout != null) connection.setConnectTimeout(connectionTimeout);
        if (readTimeout != null) connection.setReadTimeout(readTimeout);
        if (ifModifiedSince != null) connection.setIfModifiedSince(ifModifiedSince);
        for (Pair<String, String> property : setRequestProperties) connection.setRequestProperty(property.first, property.second);
        for (Pair<String, String> property : addedRequestProperties) connection.setRequestProperty(property.first, property.second);
        return connection;
    }
}
