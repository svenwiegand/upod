package mobi.upod.android.widget.bottomsheet;

import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

class BottomSheetGestureListener extends GestureDetector.SimpleOnGestureListener {
    private GestureDetectorCompat gestureDetectorCompat;
    private final BottomSheet bottomSheet;
    private float eventStartContentTranslation = 0f;
    private VelocityTracker velocityTracker = VelocityTracker.obtain();

    public BottomSheetGestureListener(BottomSheet bottomSheet) {
        this.bottomSheet = bottomSheet;
        this.gestureDetectorCompat = new GestureDetectorCompat(bottomSheet.getContext(), this);
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {
        final float overallDistanceY = motionEvent2.getRawY() - motionEvent.getRawY();
        bottomSheet.setContentTranslationY(eventStartContentTranslation + overallDistanceY);
        velocityTracker.addMovement(motionEvent2);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float velocityX, float velocityY) {
        if (motionEvent.getRawY() < motionEvent2.getRawY()) {
            bottomSheet.showPersistent();
        } else {
            bottomSheet.open();
        }
        return true;
    }

    private void onTouchDown() {
        eventStartContentTranslation = bottomSheet.getContentTranslationY();
        velocityTracker.clear();
    }

    private void onTouchUp() {
        if (velocityTracker.getYVelocity() == 0) {
            bottomSheet.snapByCurrentTranslation();
        }
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (gestureDetectorCompat.onTouchEvent(motionEvent)) {
                return true;
            } else {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    onTouchUp();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    onTouchDown();
                }
                return true;
            }
        }
    };

    public View.OnTouchListener getTouchListener() {
        return touchListener;
    }
}
