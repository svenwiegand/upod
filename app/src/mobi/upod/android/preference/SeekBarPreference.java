package mobi.upod.android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import mobi.upod.app.R;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final float DEFAULT_MIN_VALUE = 0f;
    private static final float DEFAULT_MAX_VALUE = 100f;
    private static final float DEFAULT_VALUE = 0f;
    private static final float DEFAULT_STEP_SIZE = 1f;
    private static final String DEFAULT_FLOAT_FORMAT = "%1f";

    private String unformattedSummary;

    private double mMinValue;
    private double mMaxValue;
    private double mValue;
    private double mStepSize;
    private String mFloatFormat;
    private SeekBar mSeekBar;
    private TextView mMinValueView;
    private TextView mMaxValueView;
    private TextView mValueView;

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, 0, 0);
        try {
            setStepSize(a.getFloat(R.styleable.SeekBarPreference_floatStepSize, DEFAULT_STEP_SIZE));
            setMinValue(a.getFloat(R.styleable.SeekBarPreference_floatMin, DEFAULT_MIN_VALUE));
            setMaxValue(a.getFloat(R.styleable.SeekBarPreference_floatMax, DEFAULT_MAX_VALUE));
            setFloatFormat(a.getString(R.styleable.SeekBarPreference_floatFormat));
        } finally {
            a.recycle();
        }

        // set layout
        setDialogLayoutResource(R.layout.preference_seek_bar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
        unformattedSummary = getSummary().toString();
        updateSummary();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        setValue(restore ? getPersistedFloat(DEFAULT_VALUE) : (Float) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, DEFAULT_VALUE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView dialogMessageText = (TextView) view.findViewById(R.id.text_dialog_message);
        dialogMessageText.setText(getDialogMessage());

        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mMinValueView = (TextView) view.findViewById(R.id.min_value);
        mMaxValueView = (TextView) view.findViewById(R.id.max_value);
        mValueView = (TextView) view.findViewById(R.id.value);
        updateUi();
    }

    public double getMinValue() {
        return mMinValue;
    }

    public void setMinValue(double minValue) {
        mMinValue = minValue;
        updateUi();
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(double maxValue) {
        mMaxValue = maxValue;
        updateUi();
    }

    public String getFloatFormat() {
        return mFloatFormat;
    }

    public void setFloatFormat(String format) {
        mFloatFormat = format != null ? format : DEFAULT_FLOAT_FORMAT;
        updateUi();
    }

    public double getStepSize() {
        return mStepSize;
    }

    public void setStepSize(double stepSize) {
        mStepSize = stepSize;
        updateUi();
    }

    public double getValue() {
        return mValue;
    }

    public void setValue(double value, boolean forcePersistence) {
        value = Math.max(Math.min(value, mMaxValue), mMinValue);

        if (forcePersistence || value != mValue) {
            mValue = value;
            persistFloat((float) value);
            notifyChanged();
            updateUi();
        }
    }

    public void setValue(double value) {
        setValue(value, false);
    }

    private void updateUi(double value) {
        if (mSeekBar != null) {
            mSeekBar.setMax(valueToProgress(mMaxValue));
            mSeekBar.setProgress(valueToProgress(value));
            mValueView.setText(String.format(mFloatFormat, value));
            mMinValueView.setText(String.format(mFloatFormat, mMinValue));
            mMaxValueView.setText(String.format(mFloatFormat, mMaxValue));
        }
    }

    private void updateUi() {
        updateUi(mValue);
    }

    private int valueToProgress(double value) {
        return (int) Math.round(((value - mMinValue) / mStepSize));
    }

    private double progressToValue(int progress) {
        return progress * mStepSize + mMinValue;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // when the user selects "OK", persist the new value
        if (positiveResult) {
            final int seekBarValue = mSeekBar.getProgress();
            final double value = progressToValue(seekBarValue);
            if (callChangeListener(value)) {
                setValue(value, true);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // save the instance state so that it will survive screen orientation changes and other events that may temporarily destroy it
        final Parcelable superState = super.onSaveInstanceState();

        // set the state's value with the class member that holds current setting value
        final SavedState myState = new SavedState(superState);
        myState.stepSize = getStepSize();
        myState.minValue = getMinValue();
        myState.maxValue = getMaxValue();
        myState.value = getValue();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // check whether we saved the state in onSaveInstanceState()
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // restore the state
        SavedState myState = (SavedState) state;
        setStepSize(myState.stepSize);
        setMinValue(myState.minValue);
        setMaxValue(myState.maxValue);
        setValue(myState.value);

        super.onRestoreInstanceState(myState.getSuperState());
    }

    private static class SavedState extends BaseSavedState {
        double minValue;
        double maxValue;
        double value;
        double stepSize;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);

            stepSize = source.readFloat();
            minValue = source.readFloat();
            maxValue = source.readFloat();
            value = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeDouble(stepSize);
            dest.writeDouble(minValue);
            dest.writeDouble(maxValue);
            dest.writeDouble(value);
        }

        @SuppressWarnings("unused")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected void notifyChanged() {
        updateSummary();
        super.notifyChanged();
    }

    @Override
    public void setSummary(CharSequence summary) {
        unformattedSummary = summary.toString();
        super.setSummary(summary);
        updateSummary();
    }

    private void updateSummary() {
        if (unformattedSummary != null) {
            super.setSummary(String.format(unformattedSummary, getValue()));
        }
    }

    //
    // seek bar listener
    //

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            updateUi(progressToValue(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}