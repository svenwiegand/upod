package mobi.upod.android.widget.bottomsheet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;

/**
 * An implementation of the bottom sheet component as described in the material design spec at
 * https://www.google.com/design/spec/components/bottom-sheets.html.
 *
 * You can use this component either as a modal or a persistent bottom sheet as described in the spec.
 */
public class BottomSheet extends FrameLayout implements View.OnClickListener, ActionMode.Callback {
    public static final int STATUS_CLOSED = 0;
    public static final int STATUS_PERSISTENT = 1;
    public static final int STATUS_OPEN = 2;

    private static final String LOG_TAG = "BottomSheet";
    private static final int SLIDE_DURATION = 500;
    private View contentView;
    private boolean modal = true;
    private boolean showActionModeWhenOpen = true;
    private int persistentViewId;
    private View persistentView;
    private BottomSheetGestureListener bottomSheetGestureListener;
    private int status = STATUS_CLOSED;

    public BottomSheet(Context context) {
        super(context);
        init(context, null, 0);
    }

    public BottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public BottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.BottomSheet,
                defStyleAttr, 0);
        persistentViewId = a.getResourceId(R.styleable.BottomSheet_persistentView, 0);
        a.getBoolean(R.styleable.BottomSheet_modal, true);
        showActionModeWhenOpen = a.getBoolean(R.styleable.BottomSheet_showActionModeWhenOpen, true);
        a.recycle();
    }

    //
    // properties
    //

    public boolean isModal() {
        return modal;
    }

    public void setModal(boolean modal) {
        this.modal = modal;
    }

    public boolean isShowActionModeWhenOpen() {
        return showActionModeWhenOpen;
    }

    public void setShowActionModeWhenOpen(boolean showActionModeWhenOpen) {
        this.showActionModeWhenOpen = showActionModeWhenOpen;
    }

    //
    // view initialization
    //

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (getChildCount() > 1)
            throw new IllegalStateException("Bottom sheet must have exactly one child representing the sheets content");

        initContent();
    }

    private void initContent() {
        contentView = getChildAt(0);
        persistentView = persistentViewId > 0 ? contentView.findViewById(persistentViewId) : null;
        bottomSheetGestureListener = new BottomSheetGestureListener(this);
        contentView.setOnTouchListener(bottomSheetGestureListener.getTouchListener());
        ViewCompat.setElevation(contentView, 16 * getContext().getResources().getDisplayMetrics().density);
    }

    View getContentView() {
        return contentView;
    }

    //
    // layout
    //

    private final static class Size {
        public final int width;
        public final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    protected Size calculateMaxContentSize(int containerWidth, int containerHeight) {
        // according to bottom sheet spec (https://www.google.com/design/spec/components/bottom-sheets.html#bottom-sheets-specs)
        // sheets on tablets do not spread the full width
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        final float dpiScreenWidth = displayMetrics.widthPixels / displayMetrics.density;
        final float dpiScreenHeight = displayMetrics.heightPixels / displayMetrics.density;
        final boolean isTablet = Math.min(dpiScreenWidth, dpiScreenHeight) >= 600;

        if (!isTablet) {
            return new Size(containerWidth, containerHeight);
        } else {
            final float increment = 64 * displayMetrics.density;

            // min margin rule as in the spec
            final float minXMargin = dpiScreenWidth > 1280 ? 3 * increment : (dpiScreenWidth > 960 ? 2 * increment : (dpiScreenWidth >= 600 ? increment : 0));

            // this gives a maximal width if the bottom sheet container would be as wide as the screen
            final float maxWidthOnScreen = displayMetrics.widthPixels - 2 * minXMargin;

            // the container might be smaller than the full screen.
            // If the max content width is smaller than the container,
            // than ensure that the content will have a margin of at least `increment` on both sides
            final float maxContentWidth = maxWidthOnScreen < containerWidth ? Math.min(maxWidthOnScreen, containerWidth - 2 * increment) : containerWidth;
            final float maxContentHeight = maxContentWidth < containerWidth ? containerHeight - increment : containerHeight;

            return new Size((int) maxContentWidth, (int) maxContentHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // let the framework do the calculations first
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // no lets see whether we further need to restrict the content
        final Size maxContentSize = calculateMaxContentSize(getMeasuredWidth(), getMeasuredHeight());
        final int contentWidth = MeasureSpec.makeMeasureSpec(maxContentSize.width, MeasureSpec.AT_MOST);
        final int contentHeight = MeasureSpec.makeMeasureSpec(maxContentSize.height, MeasureSpec.AT_MOST);
        contentView.measure(contentWidth, contentHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int containerWidth = right - left;
        final int containerHeight = bottom - top;

        // align content view's top with the bottom of the container, so that the content is effectively invisible
        final Size maxContentSize = calculateMaxContentSize(containerWidth, containerHeight);
        final int contentWidth = Math.min(contentView.getMeasuredWidth(), maxContentSize.width);
        final int contentHeight = Math.min(contentView.getMeasuredHeight(), maxContentSize.height);
        final int contentTop = isInEditMode() ? 0 : bottom - top;
        final int contentLeft = (containerWidth - contentWidth) / 2;

        contentView.layout(contentLeft, contentTop, contentLeft + contentWidth, contentTop + contentHeight);

        final int prevTranslation = (int) contentView.getTranslationY();
        if (status == STATUS_OPEN)
            contentView.setTranslationY(-contentHeight);
        else if (status == STATUS_PERSISTENT) {
            final int persistentHeight = persistentView != null ? persistentView.getMeasuredHeight() : 0;
            contentView.setTranslationY(-persistentHeight);
        } else
            contentView.setTranslationY(0);

        final int newTranslation = (int) contentView.getTranslationY();
        if (prevTranslation != newTranslation && heightListener != null) {
            heightListener.onBottomSheetHeightChanged(persistentView != null ? persistentView.getMeasuredHeight() : 0, -newTranslation);
        }
    }

    //
    // bottom sheet animation
    //

    private ObjectAnimator animator = null;

    private synchronized void cancelCurrentAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private synchronized void slideContent(float position) {
        cancelCurrentAnimation();
        final float currentPosition = getContentTranslationY();
        if (position != currentPosition) {
            final float slideDistanceFactor = Math.abs((position - currentPosition) / getHeight());
            final long duration = Math.max((long) slideDistanceFactor * SLIDE_DURATION, SLIDE_DURATION / 2);
            animator = ObjectAnimator.ofFloat(this, "contentTranslationY", getContentTranslationY(), position);
            animator.setInterpolator(new LinearOutSlowInInterpolator());
            animator.setDuration(duration);
            animator.addListener(new AnimationListener());
            animator.start();
        }
    }

    private class AnimationListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            updateStatus();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            updateStatus();
        }
    }

    //
    // persistent content
    //

    private int getPersistentHeight() {
        return persistentView != null ? persistentView.getHeight() : 0;
    }

    //
    // showing hiding the bottom sheet
    //

    public int getStatus() {
        return status;
    }

    public void open() {
        setStatus(STATUS_OPEN);
        slideContent(-contentView.getHeight());
    }

    public void showPersistent() {
        setStatus(STATUS_PERSISTENT);
        slideContent(-getPersistentHeight());
    }

    public void close() {
        setStatus(STATUS_CLOSED);
        slideContent(0);
    }

    private boolean isFullSizeContent() {
        return contentView.getHeight() >= getHeight();
    }

    float getContentTranslationY() {
        return getContentView().getTranslationY();
    }

    void setContentTranslationY(float translationY) {
        if (translationY > 0)
            translationY = 0;
        final float minTranslation = -contentView.getHeight();
        if (translationY < minTranslation)
            translationY = minTranslation;
        getContentView().setTranslationY(translationY);
        fireVisibleHeightChanged();
    }

    void snapByCurrentTranslation() {
        final int maximumTranslation = getHeight() - getPersistentHeight();
        final int snapTranslation = maximumTranslation / 2;
        final int currentTranslation = (int) getContentTranslationY();
        if (currentTranslation < -contentView.getHeight() + snapTranslation)
            open();
        else
            showPersistent();
    }

    private int getStatusByTranslation() {
        final int translation = Math.round(getContentTranslationY());
        if (translation == 0)
            return STATUS_CLOSED;
        else if (translation == -getPersistentHeight())
            return STATUS_PERSISTENT;
        else
            return STATUS_OPEN;
    }

    void updateStatus() {
        setStatus(getStatusByTranslation());
    }

    private void setStatus(int newStatus) {
        if (newStatus != status) {
            final int oldStatus = status;
            status = newStatus;
            fireStatusChanged(oldStatus, newStatus);
        }
    }

    //
    // status listener
    //

    private OnStatusChangedListener statusListener = null;

    public void setStatusListener(OnStatusChangedListener statusListener) {
        this.statusListener = statusListener;
    }

    protected void onStatusChanged(int oldStatus, int newStatus) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "status changed from " + oldStatus + " to " + newStatus);
        }
        interceptClicks(newStatus == STATUS_OPEN && modal);
    }

    protected void fireStatusChanged(int oldStatus, int newStatus) {
        onStatusChanged(oldStatus, newStatus);
        if (statusListener != null) {
            statusListener.onBottomSheetStatusChanged(oldStatus, newStatus);
        }
    }

    public interface OnStatusChangedListener {
        void onBottomSheetStatusChanged(int oldStatus, int newStatus);
    }

    //
    // modal handling
    //

    private void interceptClicks(boolean intercept) {
        if (intercept)
            setOnClickListener(this);
        else
            setClickable(false);
    }

    protected void dimBackgroundIfModal(final int persistentHeight, final int height) {
        if (modal && height > persistentHeight) {
            final int openHeight = getHeight() - persistentHeight;
            final int currentHeight = height - persistentHeight;
            final float heightFactor = (float) currentHeight / openHeight;
            final float alpha = 0.4f * heightFactor;
            final int color = ((byte) (alpha * 255)) << 24;
            setBackgroundColor(color);
        } else {
            setBackgroundColor(0);
        }
    }

    @Override
    public void onClick(View v) {
        showPersistent();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event))
            return true;
        else {
            if (status == STATUS_OPEN && modal && actionMode == null && event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                showPersistent();
                return true;
            } else {
                return false;
            }
        }
    }

    //
    // height listener
    //

    private OnVisibleHeightChangedListener heightListener = null;

    public void setOnVisibleHeightChangedListener(OnVisibleHeightChangedListener listener) {
        heightListener = listener;
    }

    protected void onVisibleHeightChanged(int persistentHeight, int height) {
        if (height == getHeight()) {
            startBottomSheetActionModeIfConfigured();
            updateStatus();
        } else if (height < getHeight()) {
            dimBackgroundIfModal(persistentHeight, height);
            finishBottomSheetActionMode();
        }
    }

    private void fireVisibleHeightChanged() {
        final int persistentHeight = getPersistentHeight();
        final int height = -(int) getContentTranslationY();
        onVisibleHeightChanged(persistentHeight, height);
        if (heightListener != null) {
            heightListener.onBottomSheetHeightChanged(persistentHeight, height);
        }
    }

    public interface OnVisibleHeightChangedListener {
        void onBottomSheetHeightChanged(int persistentHeight, int height);
    }

    //
    // contextual action bar
    //

    private ActionMode actionMode = null;
    private ActionMode.Callback actionModeCallback = null;

    public void setActionModeCallback(ActionMode.Callback callback) {
        actionModeCallback = callback;
    }

    private void startBottomSheetActionModeIfConfigured() {
        if (showActionModeWhenOpen && actionMode == null) {
            actionMode = startActionMode(this);
        }
    }

    private void finishBottomSheetActionMode() {
        if (actionMode != null) {
            final ActionMode am = actionMode;
            actionMode = null;
            am.finish();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return actionModeCallback == null || actionModeCallback.onCreateActionMode(mode, menu);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return actionModeCallback != null && actionModeCallback.onPrepareActionMode(mode, menu);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return actionModeCallback != null && actionModeCallback.onActionItemClicked(mode, item);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (actionMode != null) {
            if (actionModeCallback != null) {
                actionModeCallback.onDestroyActionMode(mode);
            }
            showPersistent();
            actionMode = null;
        }
    }
}
