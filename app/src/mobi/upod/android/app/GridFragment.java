package mobi.upod.android.app;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import mobi.upod.android.widget.HeaderGridView;
import mobi.upod.android.widget.SwipeRefreshLayoutForIndirectListView;
import mobi.upod.app.R;

/**
 * The equivalent to {@link android.app.ListFragment} for GridView based fragments.
 */
public class GridFragment extends Fragment implements SwipeRefreshable {
    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mGrid.focusableViewAvailable(mGrid);
        }
    };

    final private AdapterView.OnItemClickListener mOnClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onGridItemClick((GridView) parent, v, position, id);
        }
    };

    SwipeRefreshLayoutForIndirectListView mSwipeRefreshLayout;
    ListAdapter mAdapter;
    HeaderGridView mGrid;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    TextView mProgressMessageView;
    View mGridContainer;
    View mTopLoadIndicator;
    View mBottomLoadIndicator;
    CharSequence mEmptyText;
    boolean mGridShown;

    public GridFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.grid_content, container, false);
    }

    /**
     * Attach to grid view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureGrid();
    }

    /**
     * Detach from grid view.
     */
    @Override
    public void onDestroyView() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.destroyDrawingCache();
            mSwipeRefreshLayout.clearAnimation();
        }

        mHandler.removeCallbacks(mRequestFocus);
        mGrid = null;
        mGridShown = false;
        mEmptyView = mProgressContainer = mGridContainer = null;
        mProgressMessageView = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    /**
     * This method will be called when an item in the grid is selected.
     * Subclasses should override. Subclasses can call
     * getGridView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param l The GridView where the click happened
     * @param v The view that was clicked within the GridView
     * @param position The position of the view in the list
     * @param id The row id of the item that was clicked
     */
    public void onGridItemClick(GridView l, View v, int position, long id) {
    }

    /**
     * Provide the cursor for the grid view.
     */
    public void setGridAdapter(ListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mGrid != null) {
            mGrid.setAdapter(adapter);
            if (!mGridShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setGridShown(true, getView().getWindowToken() != null);
            }
        }
    }

    /**
     * Set the currently selected grid item to the specified
     * position with the adapter's data
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureGrid();
        mGrid.setSelection(position);
    }

    /**
     * Get the position of the currently selected grid item.
     */
    public int getSelectedItemPosition() {
        ensureGrid();
        return mGrid.getSelectedItemPosition();
    }

    /**
     * Get the cursor row ID of the currently selected grid item.
     */
    public long getSelectedItemId() {
        ensureGrid();
        return mGrid.getSelectedItemId();
    }

    /**
     * Get the activity's grid view widget.
     */
    public HeaderGridView getGridView() {
        ensureGrid();
        return mGrid;
    }

    /**
     * The default content for a GridFragment has a TextView that can
     * be shown when the grid is empty.  If you would like to have it
     * shown, call this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text) {
        ensureGrid();
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        mStandardEmptyView.setText(text);
        if (mEmptyText == null) {
            mGrid.setEmptyView(mStandardEmptyView);
        }
        mEmptyText = text;
    }

    /**
     * Control whether the grid is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * <p>Applications do not normally need to use this themselves.  The default
     * behavior of GridFragment is to start with the grid not being shown, only
     * showing it once an adapter is given with {@link #setGridAdapter(ListAdapter)}.
     * If the grid at that point had not been shown, when it does get shown
     * it will be done without the user ever seeing the hidden state.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    public void setGridShown(boolean shown) {
        setGridShown(shown, true);
    }

    /**
     * Like {@link #setGridShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setGridShownNoAnimation(boolean shown) {
        setGridShown(shown, false);
    }

    /**
     * Control whether the grid is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setGridShown(boolean shown, boolean animate) {
        ensureGrid();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mGridShown == shown) {
            return;
        }
        mGridShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mGridContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressMessageView.setText(getCurrentLoadingMessageId());
            mProgressContainer.setVisibility(View.VISIBLE);
            mGridContainer.setVisibility(View.GONE);
        }
    }

    protected int getCurrentLoadingMessageId() {
        return R.string.loading;
    }

    /**
     * Get the ListAdapter associated with this activity's GridView.
     */
    public ListAdapter getGridAdapter() {
        return mAdapter;
    }

    @Override
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener refreshListener) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setListView(mGrid);
            mSwipeRefreshLayout.setEnabled(refreshListener != null);
            mSwipeRefreshLayout.setOnRefreshListener(refreshListener);
        }
    }

    @Override
    public void setRefreshing(final boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(refreshing);
                }
            });
        }
    }

    @Override
    public void setSwipeRefreshColorScheme(int... colors) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeColors(colors);
        }
    }

    @Override
    public void setSwipeRefreshIndicatorOffset(int offset) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setProgressIndicatorOffset(offset);
        }
    }

    private void ensureGrid() {
        if (mGrid != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof GridView) {
            mGrid = (HeaderGridView)root;
        } else {
            mSwipeRefreshLayout = (SwipeRefreshLayoutForIndirectListView) root;
            mSwipeRefreshLayout.setEnabled(false);

            mStandardEmptyView = (TextView)root.findViewById(R.id.internalEmpty);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(R.id.progressContainer);
            mProgressMessageView = (TextView) mProgressContainer.findViewById(R.id.progressMessage);
            mTopLoadIndicator = root.findViewById(R.id.bottomLoadIndicator);
            mBottomLoadIndicator = root.findViewById(R.id.bottomLoadIndicator);
            mGridContainer = root.findViewById(R.id.gridContainer);
            View rawGridView = root.findViewById(R.id.grid);
            if (!(rawGridView instanceof GridView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'R.id.grid' "
                        + "that is not a GridView class");
            }
            mGrid = (HeaderGridView) rawGridView;
            if (mGrid == null) {
                throw new RuntimeException(
                        "Your content must have a GridView whose id attribute is " +
                        "'R.id.grid'");
            }
            if (mEmptyView != null) {
                mGrid.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mGrid.setEmptyView(mGridContainer.findViewById(R.id.emptyContainer));
            }
        }
        mGridShown = true;
        mGrid.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setGridAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setGridShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    private void showLoadIndicator(View loadIndicator, boolean show) {
        loadIndicator.clearAnimation();
        if (show) {
            loadIndicator.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
        } else {
            loadIndicator.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
        }
        loadIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showTopLoadIndicator(boolean show) {
        showLoadIndicator(mTopLoadIndicator, show);
    }

    public void showBottomLoadIndicator(boolean show) {
        showLoadIndicator(mBottomLoadIndicator, show);
    }
}
