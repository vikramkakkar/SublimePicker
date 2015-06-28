/*
 * Copyright 2015 Vikram Kakkar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appeaser.sublimepickerlibrary;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.appeaser.sublimepickerlibrary.common.ButtonLayout;
import com.appeaser.sublimepickerlibrary.datepicker.SublimeDatePicker;
import com.appeaser.sublimepickerlibrary.drawables.OverflowDrawable;
import com.appeaser.sublimepickerlibrary.helpers.SublimeListenerAdapter;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.appeaser.sublimepickerlibrary.timepicker.SublimeTimePicker;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A customizable view that provisions picking of a date,
 * time and recurrence option, all from a single user-interface.
 * You can also view 'SublimePicker' as a collection of
 * material-styled (API 22, and _not_ 21) DatePicker, TimePicker
 * and RecurrencePicker, backported to API 16.
 * You can opt for any combination of these three Pickers.
 */
public class SublimePicker extends FrameLayout
        implements SublimeDatePicker.OnDateChangedListener,
        SublimeDatePicker.ValidationCallback,
        SublimeTimePicker.ValidationCallback {
    // Container for 'SublimeDatePicker' & 'SublimeTimePicker'
    LinearLayout llMainContentHolder;

    // For access to 'SublimeRecurrencePicker'
    ImageView ivRecurrenceOptionsDP, ivRecurrenceOptionsTP;

    // Recurrence picker options
    SublimeRecurrencePicker mSublimeRecurrencePicker;
    SublimeRecurrencePicker.RecurrenceOption mCurrentRecurrenceOption
            = SublimeRecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
    String mRecurrenceRule;

    // Keeps track which picker is showing
    private SublimeOptions.Picker mCurrentPicker, mHiddenPicker;

    // Date picker
    private SublimeDatePicker mDatePicker;

    // Time picker
    private SublimeTimePicker mTimePicker;

    // Callback
    private SublimeListenerAdapter mListener;

    // Client-set options
    private SublimeOptions mOptions;

    // Ok, cancel & switch button handler
    private ButtonLayout mButtonLayout;

    // Flags set based on client-set options {SublimeOptions}
    private boolean mDatePickerValid = true, mTimePickerValid = true,
            mDatePickerEnabled, mTimePickerEnabled, mRecurrencePickerEnabled,
            mDatePickerSyncStateCalled;

    // Used if listener returns
    // null/invalid(zero-length, empty) string
    DateFormat mDefaultDateFormatter, mDefaultTimeFormatter;

    // Listener for recurrence picker
    private SublimeRecurrencePicker.OnRepeatOptionSetListener mRepeatOptionSetListener = new SublimeRecurrencePicker.OnRepeatOptionSetListener() {
        @Override
        public void onRepeatOptionSet(SublimeRecurrencePicker.RecurrenceOption option, String recurrenceRule) {
            mCurrentRecurrenceOption = option;
            mRecurrenceRule = recurrenceRule;
            onDone();
        }

        @Override
        public void onDone() {
            if (mDatePickerEnabled || mTimePickerEnabled) {
                updateCurrentPicker();
                updateDisplay();
            } else { /* No other picker is activated. Dismiss. */
                mButtonLayoutCallback.onOkay();
            }
        }
    };

    // Handle ok, cancel & switch button click events
    private ButtonLayout.Callback mButtonLayoutCallback = new ButtonLayout.Callback() {
        @Override
        public void onOkay() {
            int year = -1, month = -1, day = -1;

            if (mDatePickerEnabled) {
                year = mDatePicker.getYear();
                month = mDatePicker.getMonth();
                day = mDatePicker.getDayOfMonth();
            }

            int hour = -1, minute = -1;

            if (mTimePickerEnabled) {
                hour = mTimePicker.getCurrentHour();
                minute = mTimePicker.getCurrentMinute();
            }

            SublimeRecurrencePicker.RecurrenceOption recurrenceOption
                    = SublimeRecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
            String recurrenceRule = null;

            if (mRecurrencePickerEnabled) {
                recurrenceOption = mCurrentRecurrenceOption;

                if (recurrenceOption == SublimeRecurrencePicker.RecurrenceOption.CUSTOM) {
                    recurrenceRule = mRecurrenceRule;
                }
            }

            mListener.onDateTimeRecurrenceSet(SublimePicker.this,
                    // DatePicker
                    year, month, day,
                    // TimePicker
                    hour, minute,
                    // RecurrencePicker
                    recurrenceOption, recurrenceRule);
        }

        @Override
        public void onCancel() {
            mListener.onCancelled();
        }

        @Override
        public void onSwitch() {
            mCurrentPicker = mCurrentPicker == SublimeOptions.Picker.DATE_PICKER ?
                    SublimeOptions.Picker.TIME_PICKER
                    : SublimeOptions.Picker.DATE_PICKER;

            updateDisplay();
        }
    };

    public SublimePicker(Context context) {
        this(context, null);
    }

    public SublimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sublimePickerStyle);
    }

    public SublimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(createThemeWrapper(context), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SublimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(createThemeWrapper(context), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    private static ContextThemeWrapper createThemeWrapper(Context context) {
        final TypedArray forParent = context.obtainStyledAttributes(
                new int[]{R.attr.sublimePickerStyle});
        int parentStyle = forParent.getResourceId(0, R.style.SublimePickerStyleLight);
        forParent.recycle();

        return new ContextThemeWrapper(context, parentStyle);
    }

    private void initializeLayout() {
        Context context = getContext();
        SUtils.initializeResources(context);

        LayoutInflater.from(context).inflate(R.layout.sublime_picker_view_layout,
                this, true);

        mDefaultDateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.getDefault());
        mDefaultTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT,
                Locale.getDefault());
        mDefaultTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        llMainContentHolder = (LinearLayout) findViewById(R.id.llMainContentHolder);
        mButtonLayout = (ButtonLayout) findViewById(R.id.button_layout);

        initializeRecurrencePickerSwitch();

        mDatePicker = (SublimeDatePicker) findViewById(R.id.datePicker);
        mTimePicker = (SublimeTimePicker) findViewById(R.id.timePicker);
        mSublimeRecurrencePicker = (SublimeRecurrencePicker)
                findViewById(R.id.repeat_option_picker);
    }

    public void initializePicker(SublimeOptions options, SublimeListenerAdapter listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }

        if (options != null) {
            options.verifyValidity();
        } else {
            options = new SublimeOptions();
        }

        mOptions = options;
        mListener = listener;

        processOptions();
        updateDisplay();
    }

    // Called before 'RecurrencePicker' is shown
    private void updateHiddenPicker() {
        if (mDatePickerEnabled && mTimePickerEnabled) {
            mHiddenPicker = mDatePicker.getVisibility() == View.VISIBLE ?
                    SublimeOptions.Picker.DATE_PICKER : SublimeOptions.Picker.TIME_PICKER;
        } else if (mDatePickerEnabled) {
            mHiddenPicker = SublimeOptions.Picker.DATE_PICKER;
        } else if (mTimePickerEnabled) {
            mHiddenPicker = SublimeOptions.Picker.TIME_PICKER;
        } else {
            mHiddenPicker = SublimeOptions.Picker.INVALID;
        }
    }

    // 'mHiddenPicker' retains the Picker that was active
    // before 'RecurrencePicker' was shown. On its dismissal,
    // we have an option to show either 'DatePicker' or 'TimePicker'.
    // 'mHiddenPicker' helps identify the correct option.
    private void updateCurrentPicker() {
        if (mHiddenPicker != SublimeOptions.Picker.INVALID) {
            mCurrentPicker = mHiddenPicker;
        } else {
            throw new RuntimeException("Logic issue: No valid option for mCurrentPicker");
        }
    }

    public void updateDisplay() {
        CharSequence switchButtonText;

        if (mCurrentPicker == SublimeOptions.Picker.DATE_PICKER) {

            if (mTimePickerEnabled) {
                mTimePicker.setVisibility(View.GONE);
            }

            if (mRecurrencePickerEnabled) {
                mSublimeRecurrencePicker.setVisibility(View.GONE);
            }

            mDatePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

            if (mButtonLayout.isSwitcherButtonEnabled()) {
                Date toFormat = new Date(mTimePicker.getCurrentHour() * DateUtils.HOUR_IN_MILLIS
                        + mTimePicker.getCurrentMinute() * DateUtils.MINUTE_IN_MILLIS);

                switchButtonText = mListener.formatTime(toFormat);

                if (TextUtils.isEmpty(switchButtonText)) {
                    switchButtonText = mDefaultTimeFormatter.format(toFormat);
                }

                mButtonLayout.updateSwitcherText(switchButtonText);
            }

            mButtonLayout.updateVisiblePicker(SublimeOptions.Picker.DATE_PICKER);

            if (!mDatePickerSyncStateCalled) {
                mDatePicker.syncState();
                mDatePickerSyncStateCalled = true;
            }
        } else if (mCurrentPicker == SublimeOptions.Picker.TIME_PICKER) {
            if (mDatePickerEnabled) {
                mDatePicker.setVisibility(View.GONE);
            }

            if (mRecurrencePickerEnabled) {
                mSublimeRecurrencePicker.setVisibility(View.GONE);
            }

            mTimePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

            if (mButtonLayout.isSwitcherButtonEnabled()) {

                Date toFormat = new Date(mDatePicker.getSelectedDay().getTimeInMillis());

                switchButtonText = mListener.formatDate(toFormat);

                if (TextUtils.isEmpty(switchButtonText)) {
                    switchButtonText = mDefaultDateFormatter.format(toFormat);
                }

                mButtonLayout.updateSwitcherText(switchButtonText);
            }

            mButtonLayout.updateVisiblePicker(SublimeOptions.Picker.TIME_PICKER);
        } else if (mCurrentPicker == SublimeOptions.Picker.REPEAT_OPTION_PICKER) {
            updateHiddenPicker();
            mSublimeRecurrencePicker.updateView();

            if (mDatePickerEnabled || mTimePickerEnabled) {
                llMainContentHolder.setVisibility(View.GONE);
            }

            mSublimeRecurrencePicker.setVisibility(View.VISIBLE);
        }
    }

    void initializeRecurrencePickerSwitch() {
        ivRecurrenceOptionsDP = (ImageView) findViewById(R.id.ivRecurrenceOptionsDP);
        ivRecurrenceOptionsTP = (ImageView) findViewById(R.id.ivRecurrenceOptionsTP);

        int iconColor, pressedStateBgColor;

        TypedArray typedArray = getContext().obtainStyledAttributes(R.styleable.SublimePicker);
        try {
            iconColor = typedArray.getColor(R.styleable.SublimePicker_spOverflowIconColor,
                    SUtils.COLOR_TEXT_PRIMARY_INVERSE);
            pressedStateBgColor = typedArray.getColor(R.styleable.SublimePicker_spOverflowIconPressedBgColor,
                    SUtils.COLOR_TEXT_PRIMARY);
        } finally {
            typedArray.recycle();
        }

        ivRecurrenceOptionsDP.setImageDrawable(
                new OverflowDrawable(getContext(), iconColor));
        SUtils.setViewBackground(ivRecurrenceOptionsDP,
                SUtils.createOverflowButtonBg(pressedStateBgColor));

        ivRecurrenceOptionsTP.setImageDrawable(
                new OverflowDrawable(getContext(), iconColor));
        SUtils.setViewBackground(ivRecurrenceOptionsTP,
                SUtils.createOverflowButtonBg(pressedStateBgColor));

        ivRecurrenceOptionsDP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPicker = SublimeOptions.Picker.REPEAT_OPTION_PICKER;
                updateDisplay();
            }
        });

        ivRecurrenceOptionsTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPicker = SublimeOptions.Picker.REPEAT_OPTION_PICKER;
                updateDisplay();
            }
        });
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mCurrentPicker, mHiddenPicker,
                mCurrentRecurrenceOption, mRecurrenceRule);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentPicker = ss.getCurrentPicker();
        mCurrentRecurrenceOption = ss.getCurrentRepeatOption();
        mRecurrenceRule = ss.getRecurrenceRule();

        mHiddenPicker = ss.getHiddenPicker();
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        updateDisplay();
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final SublimeOptions.Picker sCurrentPicker, sHiddenPicker /*One of DatePicker/TimePicker*/;
        private final SublimeRecurrencePicker.RecurrenceOption sCurrentRecurrenceOption;
        private final String sRecurrenceRule;

        /**
         * Constructor called from {@link SublimePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, SublimeOptions.Picker currentPicker,
                           SublimeOptions.Picker hiddenPicker,
                           SublimeRecurrencePicker.RecurrenceOption recurrenceOption,
                           String recurrenceRule) {
            super(superState);

            sCurrentPicker = currentPicker;
            sHiddenPicker = hiddenPicker;
            sCurrentRecurrenceOption = recurrenceOption;
            sRecurrenceRule = recurrenceRule;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            sCurrentPicker = SublimeOptions.Picker.valueOf(in.readString());
            sHiddenPicker = SublimeOptions.Picker.valueOf(in.readString());
            sCurrentRecurrenceOption = SublimeRecurrencePicker.RecurrenceOption.valueOf(in.readString());
            sRecurrenceRule = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeString(sCurrentPicker.name());
            dest.writeString(sHiddenPicker.name());
            dest.writeString(sCurrentRecurrenceOption.name());
            dest.writeString(sRecurrenceRule);
        }

        public SublimeOptions.Picker getCurrentPicker() {
            return sCurrentPicker;
        }

        public SublimeOptions.Picker getHiddenPicker() {
            return sHiddenPicker;
        }

        public SublimeRecurrencePicker.RecurrenceOption getCurrentRepeatOption() {
            return sCurrentRecurrenceOption;
        }

        public String getRecurrenceRule() {
            return sRecurrenceRule;
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void processOptions() {
        if (mOptions.animateLayoutChanges()) {
            // Basic Layout Change Animation(s)
            LayoutTransition layoutTransition = new LayoutTransition();
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
            setLayoutTransition(layoutTransition);
        } else {
            setLayoutTransition(null);
        }

        mDatePickerEnabled = mOptions.isDatePickerActive();
        mTimePickerEnabled = mOptions.isTimePickerActive();
        mRecurrencePickerEnabled = mOptions.isRecurrencePickerActive();

        if (mDatePickerEnabled) {
            int[] dateParams = mOptions.getDateParams();
            mDatePicker.init(dateParams[0] /* year */,
                    dateParams[1] /* month of year */,
                    dateParams[2] /* day of month */,
                    this);

            long[] dateRange = mOptions.getDateRange();

            if (dateRange[0] /* min date */ != Long.MIN_VALUE) {
                mDatePicker.setMinDate(dateRange[0]);
            }

            if (dateRange[1] /* max date */ != Long.MIN_VALUE) {
                mDatePicker.setMaxDate(dateRange[1]);
            }

            mDatePicker.setValidationCallback(this);

            ivRecurrenceOptionsDP.setVisibility(mRecurrencePickerEnabled ?
                    View.VISIBLE : View.GONE);
        } else {
            llMainContentHolder.removeView(mDatePicker);
            mDatePicker = null;
        }

        if (mTimePickerEnabled) {
            int[] timeParams = mOptions.getTimeParams();
            mTimePicker.setCurrentHour(timeParams[0] /* hour of day */);
            mTimePicker.setCurrentMinute(timeParams[1] /* minute */);
            mTimePicker.setIs24HourView(mOptions.is24HourView());
            mTimePicker.setValidationCallback(this);

            ivRecurrenceOptionsTP.setVisibility(mRecurrencePickerEnabled ?
                    View.VISIBLE : View.GONE);
        } else {
            llMainContentHolder.removeView(mTimePicker);
            mTimePicker = null;
        }

        if (mDatePickerEnabled && mTimePickerEnabled) {
            mButtonLayout.applyOptions(true /* show switch button */,
                    mButtonLayoutCallback, /* STUB */ SublimeOptions.Picker.DATE_PICKER);
        } else {
            mButtonLayout.applyOptions(false /* hide switch button */,
                    mButtonLayoutCallback, /* STUB */ SublimeOptions.Picker.DATE_PICKER);
        }

        if (!mDatePickerEnabled && !mTimePickerEnabled) {
            removeView(llMainContentHolder);
            llMainContentHolder = null;
            mButtonLayout = null;
        }

        if (mRecurrencePickerEnabled) {
            Calendar cal = mDatePickerEnabled ?
                    mDatePicker.getSelectedDay()
                    : SUtils.getCalendarForLocale(null, Locale.getDefault());

            mSublimeRecurrencePicker.initializeData(mRepeatOptionSetListener,
                    mCurrentRecurrenceOption, mRecurrenceRule,
                    cal.getTimeInMillis());
        } else {
            removeView(mSublimeRecurrencePicker);
            mSublimeRecurrencePicker = null;
        }

        mCurrentPicker = mOptions.getPickerToShow();
        // Updated from 'updateDisplay()' when 'RecurrencePicker' is chosen
        mHiddenPicker = SublimeOptions.Picker.INVALID;
    }

    private void reassessValidity() {
        mButtonLayout.updateValidity(mDatePickerValid && mTimePickerValid);
    }

    @Override
    public void onDateChanged(SublimeDatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
    }

    @Override
    public void onDatePickerValidationChanged(boolean valid) {
        mDatePickerValid = valid;
        reassessValidity();
    }

    @Override
    public void onTimePickerValidationChanged(boolean valid) {
        mTimePickerValid = valid;
        reassessValidity();
    }
}
