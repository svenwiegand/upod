package mobi.upod.android.app;

import android.support.v4.widget.SwipeRefreshLayout;

public interface SwipeRefreshable {

    /**
     * Sets the listener to be informed of swipe refresh events and enables swipe refreshing.
     * @param refreshListener the listener to be informed of swipe refresh events
     */
    void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener refreshListener);

    void setRefreshing(boolean refreshing);

    void setSwipeRefreshColorScheme(int... colors);

    void setSwipeRefreshIndicatorOffset(int offset);
}
