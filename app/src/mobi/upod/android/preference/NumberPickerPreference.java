package mobi.upod.android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import mobi.upod.app.R;

public class NumberPickerPreference extends DialogPreference {
    private static final String DEFAULT_NUMBER_FORMAT = "%d";
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_STEP_SIZE = 1;
    private static final int DEFAULT_VALUE = 0;

    private String unformattedSummary;

    private int mMinValue;
    private int mMaxValue;
    private int mStepSize;
    private int mValue;
    private String mNumberFormat = null;
    private String mNumberUnit = null;
    private NumberPicker mNumberPicker;

    public NumberPickerPreference(Context context) {
        this(context, null);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
        try {
            setMinValue(a.getInteger(R.styleable.NumberPickerPreference_min, DEFAULT_MIN_VALUE));
            setMaxValue(a.getInteger(R.styleable.NumberPickerPreference_android_max, DEFAULT_MAX_VALUE));
            setStepSize(a.getInteger(R.styleable.NumberPickerPreference_stepSize, DEFAULT_STEP_SIZE));
            setNumberFormat(a.getString(R.styleable.NumberPickerPreference_numberFormat));
            setNumberUnit(a.getString(R.styleable.NumberPickerPreference_numberUnit));
        } finally {
            a.recycle();
        }

        // set layout
        setDialogLayoutResource(R.layout.preference_number_picker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
        unformattedSummary = getSummary().toString();
        updateSummary();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        setValue(restore ? getPersistedInt(DEFAULT_VALUE) : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final TextView dialogMessageText = (TextView) view.findViewById(R.id.text_dialog_message);
        dialogMessageText.setText(getDialogMessage());

        final int min = mMinValue / mStepSize;
        final int max = mMaxValue / mStepSize;
        final int value = mValue / mStepSize;
        final int size = max - min + 1;
        final String[] displayValues = new String[size];
        for (int i = 0; i < size; ++i) {
            final int v = mMinValue + i * mStepSize;
            displayValues[i] = String.format(mNumberFormat, v);
        }

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        mNumberPicker.setMinValue(min);
        mNumberPicker.setMaxValue(max);
        mNumberPicker.setValue(value);
        mNumberPicker.setDisplayedValues(displayValues);

        final TextView unitTextView = (TextView) view.findViewById(R.id.number_unit);
        unitTextView.setText(mNumberUnit != null ? mNumberUnit : "");
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int minValue) {
        mMinValue = minValue;
        setValue(Math.max(mValue, mMinValue));
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
        setValue(Math.min(mValue, mMaxValue));
    }

    public int getStepSize() {
        return mStepSize;
    }

    public void setStepSize(int stepSize) {
        mStepSize = stepSize;
    }

    public String getNumberFormat() {
        return mNumberFormat;
    }

    public void setNumberFormat(String format) {
        mNumberFormat = format != null ? format : DEFAULT_NUMBER_FORMAT;
    }

    public String getNumberUnit() {
        return mNumberUnit;
    }

    public void setNumberUnit(String unit) {
        mNumberUnit = unit;
    }

    public void setNumberUnit(int resId) {
        mNumberUnit = getContext().getString(resId);
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        value = Math.max(Math.min(value, mMaxValue), mMinValue);

        if (value != mValue) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }

    private int calculateValue(int pickerValue) {
        return mStepSize * pickerValue;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // when the user selects "OK", persist the new value
        if (positiveResult) {
            int numberPickerValue = calculateValue(mNumberPicker.getValue());
            if (callChangeListener(numberPickerValue)) {
                setValue(numberPickerValue);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // save the instance state so that it will survive screen orientation changes and other events that may temporarily destroy it
        final Parcelable superState = super.onSaveInstanceState();

        // set the state's value with the class member that holds current setting value
        final SavedState myState = new SavedState(superState);
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
        setMinValue(myState.minValue);
        setMaxValue(myState.maxValue);
        setValue(myState.value);

        super.onRestoreInstanceState(myState.getSuperState());
    }

    private class Formatter implements NumberPicker.Formatter {
        @Override
        public String format(int value) {
            return String.format(mNumberFormat, value);
        }
    }

    private static class SavedState extends BaseSavedState {
        int minValue;
        int maxValue;
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);

            minValue = source.readInt();
            maxValue = source.readInt();
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeInt(minValue);
            dest.writeInt(maxValue);
            dest.writeInt(value);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
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
}