/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright 2015 Vikram Kakkar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appeaser.sublimepickerlibrary.recurrencepicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.common.DecisionButtonLayout;
import com.appeaser.sublimepickerlibrary.datepicker.RecurrenceEndDatePicker;
import com.appeaser.sublimepickerlibrary.drawables.CheckableDrawable;
import com.appeaser.sublimepickerlibrary.utilities.RecurrenceUtils;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * Helps create a custom recurrence-rule.
 */
public class RecurrenceOptionCreator extends FrameLayout
        implements AdapterView.OnItemSelectedListener,
        RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, RecurrenceEndDatePicker.OnDateSetListener {

    private static final String TAG = "RecurrenceOptionCreator";

    // Used to keep track of currently visible view - the view that
    // will be restored on screen rotation.
    private enum CurrentView {
        RECURRENCE_PICKER, DATE_ONLY_PICKER
    }

    // Update android:maxLength in EditText as needed
    private static final int INTERVAL_MAX = 99;
    private static final int INTERVAL_DEFAULT = 1;
    // Update android:maxLength in EditText as needed
    private static final int COUNT_MAX = 730;
    private static final int COUNT_DEFAULT = 5;

    // Special cases in monthlyByNthDayOfWeek
    private static final int FIFTH_WEEK_IN_A_MONTH = 5;
    private static final int LAST_NTH_DAY_OF_WEEK = -1;

    // Stripped down version of 'SublimeMaterialDatePicker'
    //private DatePickerView mDateOnlyPicker;
    private RecurrenceEndDatePicker mDateOnlyPicker;
    private View mRecurrencePicker;

    // OK/Cancel buttons
    private DecisionButtonLayout mButtonLayout;

    // Uses either to DateFormat.SHORT or DateFormat.MEDIUM
    // to format the supplied end date. The option can only be
    // set in RecurrenceOptionCreator's style-definition
    private DateFormat mEndDateFormatter;

    private Resources mResources;
    private EventRecurrence mRecurrence = new EventRecurrence();
    private Time mTime = new Time(); // TODO timezone?
    private RecurrenceModel mModel = new RecurrenceModel();
    private Toast mToast;

    private final int[] TIME_DAY_TO_CALENDAR_DAY = new int[]{
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };

    // Call mStringBuilder.setLength(0) before formatting any string or else the
    // formatted text will accumulate.
    // private final StringBuilder mStringBuilder = new StringBuilder();
    // private Formatter mFormatter = new Formatter(mStringBuilder);

    private Spinner mFreqSpinner;
    private static final int[] mFreqModelToEventRecurrence = {
            EventRecurrence.DAILY,
            EventRecurrence.WEEKLY,
            EventRecurrence.MONTHLY,
            EventRecurrence.YEARLY
    };

    private EditText mInterval;
    private TextView mIntervalPreText;
    private TextView mIntervalPostText;

    private int mIntervalResId = -1;

    private Spinner mEndSpinner;
    private TextView mEndDateTextView;
    private EditText mEndCount;
    private TextView mPostEndCount;
    private boolean mHidePostEndCount;

    private ArrayList<CharSequence> mEndSpinnerArray = new ArrayList<>(3);
    private EndSpinnerAdapter mEndSpinnerAdapter;
    private String mEndNeverStr;
    private String mEndDateLabel;
    private String mEndCountLabel;

    /**
     * Hold toggle buttons in the order per user's first day of week preference
     */
    private LinearLayout mWeekGroup;
    private LinearLayout mWeekGroup2;
    // Sun = 0
    private WeekButton[] mWeekByDayButtons = new WeekButton[7];
    /**
     * A double array of Strings to hold the 7x5 list of possible strings of the form:
     * "on every [Nth] [DAY_OF_WEEK]", e.g. "on every second Monday",
     * where [Nth] can be [first, second, third, fourth, last]
     */
    private String[][] mMonthRepeatByDayOfWeekStrs;

    private RadioGroup mMonthRepeatByRadioGroup;
    private RadioButton mRepeatMonthlyByNthDayOfWeek;
    private RadioButton mRepeatMonthlyByNthDayOfMonth;
    private String mMonthRepeatByDayOfWeekStr;

    private OnRecurrenceSetListener mRecurrenceSetListener;
    int mHeaderBackgroundColor;

    private DecisionButtonLayout.Callback mButtonLayoutCallback = new DecisionButtonLayout.Callback() {
        @Override
        public void onOkay() {
            String rrule;
            if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
                rrule = null;
            } else {
                copyModelToEventRecurrence(mModel, mRecurrence);
                rrule = mRecurrence.toString();
            }

            mRecurrenceSetListener.onRecurrenceSet(rrule);
        }

        @Override
        public void onCancel() {
            mRecurrenceSetListener.onCancelled();
        }
    };

    // { WIP: Provide a synonym for chosen custom option. }
    //
    // Eg: if 'freq' is 'WEEKLY' and all seven days of the week
    // are selected, the chosen option is equivalent to 'Repeats Daily'
    // Actual Recurrence Rule string is 'Repeats Weekly every
    // SUN, MON, TUE, WED, THU, FRI, SAT'. More options are possible.
    //
    // Another possible extension is - if 'freq' is 'YEARLY' and
    // 'interval' is set to 1, the custom option 'EVERY YEAR' is
    // already present in the 'SublimeRecurrencePicker' menu. Use
    // that instead of showing 'REPEATS YEARLY' at the top.
    @SuppressWarnings("unused")
    SublimeRecurrencePicker.RecurrenceOption resolveRepeatOption() {
        if (mModel.freq == RecurrenceModel.FREQ_DAILY) {
            if (mModel.interval == INTERVAL_DEFAULT
                    && mModel.end == RecurrenceModel.END_NEVER) {
                return SublimeRecurrencePicker.RecurrenceOption.DAILY;
            }
        } /*else if (mModel.freq == RecurrenceModel.FREQ_WEEKLY) {

        }*/

        return SublimeRecurrencePicker.RecurrenceOption.CUSTOM;
    }

    private class RecurrenceModel implements Parcelable {

        // Should match EventRecurrence.DAILY, etc
        static final int FREQ_DAILY = 0;
        static final int FREQ_WEEKLY = 1;
        static final int FREQ_MONTHLY = 2;
        static final int FREQ_YEARLY = 3;

        static final int END_NEVER = 0;
        static final int END_BY_DATE = 1;
        static final int END_BY_COUNT = 2;

        static final int MONTHLY_BY_DATE = 0;
        static final int MONTHLY_BY_NTH_DAY_OF_WEEK = 1;

        static final int STATE_NO_RECURRENCE = 0;
        static final int STATE_RECURRENCE = 1;

        int recurrenceState;

        /**
         * FREQ: Repeat pattern
         */
        int freq = FREQ_WEEKLY;

        /**
         * INTERVAL: Every n days/weeks/months/years. n >= 1
         */
        int interval = INTERVAL_DEFAULT;

        /**
         * UNTIL and COUNT: How does the the event end?
         */
        int end;

        /**
         * UNTIL: Date of the last recurrence. Used when until == END_BY_DATE
         */
        Time endDate;

        /**
         * COUNT: Times to repeat. Use when until == END_BY_COUNT
         */
        int endCount = COUNT_DEFAULT;

        /**
         * BYDAY: Days of the week to be repeated. Sun = 0, Mon = 1, etc
         */
        boolean[] weeklyByDayOfWeek = new boolean[7];

        /**
         * BYDAY AND BYMONTHDAY: How to repeat monthly events? Same date of the
         * month or Same nth day of week.
         */
        int monthlyRepeat;

        /**
         * Day of the month to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_DATE
         */
        int monthlyByMonthDay;

        /**
         * Day of the week to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_NTH_DAY_OF_WEEK
         */
        int monthlyByDayOfWeek;

        /**
         * Nth day of the week to repeat. Used when monthlyRepeat ==
         * MONTHLY_BY_NTH_DAY_OF_WEEK 0=undefined, -1=Last, 1=1st, 2=2nd, ..., 5=5th
         * <p/>
         * We support 5th, just to handle backwards capabilities with old bug, but it
         * gets converted to -1 once edited.
         */
        int monthlyByNthDayOfWeek;

        /*
         * (generated method)
         */
        @Override
        public String toString() {
            return "Model [freq=" + freq + ", interval=" + interval + ", end=" + end + ", endDate="
                    + endDate + ", endCount=" + endCount + ", weeklyByDayOfWeek="
                    + Arrays.toString(weeklyByDayOfWeek) + ", monthlyRepeat=" + monthlyRepeat
                    + ", monthlyByMonthDay=" + monthlyByMonthDay + ", monthlyByDayOfWeek="
                    + monthlyByDayOfWeek + ", monthlyByNthDayOfWeek=" + monthlyByNthDayOfWeek + "]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public RecurrenceModel() {
        }

        public RecurrenceModel(Parcel in) {
            readFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(freq);
            dest.writeInt(interval);
            dest.writeInt(end);
            dest.writeInt(endDate.year);
            dest.writeInt(endDate.month);
            dest.writeInt(endDate.monthDay);
            dest.writeInt(endCount);
            dest.writeBooleanArray(weeklyByDayOfWeek);
            dest.writeInt(monthlyRepeat);
            dest.writeInt(monthlyByMonthDay);
            dest.writeInt(monthlyByDayOfWeek);
            dest.writeInt(monthlyByNthDayOfWeek);
            dest.writeInt(recurrenceState);
        }

        private void readFromParcel(Parcel in) {
            freq = in.readInt();
            interval = in.readInt();
            end = in.readInt();
            endDate = new Time();
            endDate.year = in.readInt();
            endDate.month = in.readInt();
            endDate.monthDay = in.readInt();
            endCount = in.readInt();
            in.readBooleanArray(weeklyByDayOfWeek);
            monthlyRepeat = in.readInt();
            monthlyByMonthDay = in.readInt();
            monthlyByDayOfWeek = in.readInt();
            monthlyByNthDayOfWeek = in.readInt();
            recurrenceState = in.readInt();
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public final Parcelable.Creator<RecurrenceModel> CREATOR = new Creator<RecurrenceModel>() {

            public RecurrenceModel createFromParcel(Parcel in) {
                return new RecurrenceModel(in);
            }

            public RecurrenceModel[] newArray(int size) {
                return new RecurrenceModel[size];
            }
        };
    }

    class minMaxTextWatcher implements TextWatcher {
        private int mMin;
        private int mMax;
        private int mDefault;

        public minMaxTextWatcher(int min, int defaultInt, int max) {
            mMin = min;
            mMax = max;
            mDefault = defaultInt;
        }

        @Override
        public void afterTextChanged(Editable s) {

            boolean updated = false;
            int value;
            try {
                value = Integer.parseInt(s.toString());
            } catch (NumberFormatException e) {
                value = mDefault;
            }

            if (value < mMin) {
                value = mMin;
                updated = true;
            } else if (value > mMax) {
                updated = true;
                value = mMax;
            }

            // Update UI
            if (updated) {
                s.clear();
                s.append(Integer.toString(value));
            }

            updateDoneButtonState();
            onChange(value);
        }

        /**
         * Override to be called after each key stroke
         */
        void onChange(int value) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    static public boolean isSupportedMonthlyByNthDayOfWeek(int num) {
        // We only support monthlyByNthDayOfWeek when it is greater then 0 but less then 5.
        // Or if -1 when it is the last monthly day of the week.
        return (num > 0 && num <= FIFTH_WEEK_IN_A_MONTH) || num == LAST_NTH_DAY_OF_WEEK;
    }

    static public boolean canHandleRecurrenceRule(EventRecurrence er) {
        switch (er.freq) {
            case EventRecurrence.DAILY:
            case EventRecurrence.MONTHLY:
            case EventRecurrence.YEARLY:
            case EventRecurrence.WEEKLY:
                break;
            default:
                return false;
        }

        if (er.count > 0 && !TextUtils.isEmpty(er.until)) {
            return false;
        }

        // Weekly: For "repeat by day of week", the day of week to repeat is in
        // er.byday[]

        /*
         * Monthly: For "repeat by nth day of week" the day of week to repeat is
         * in er.byday[] and the "nth" is stored in er.bydayNum[]. Currently we
         * can handle only one and only in monthly
         */
        int numOfByDayNum = 0;
        for (int i = 0; i < er.bydayCount; i++) {
            if (isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                ++numOfByDayNum;
            }
        }

        if (numOfByDayNum > 1) {
            return false;
        }

        if (numOfByDayNum > 0 && er.freq != EventRecurrence.MONTHLY) {
            return false;
        }

        // The UI only handle repeat by one day of month i.e. not 9th and 10th
        // of every month
        if (er.bymonthdayCount > 1) {
            return false;
        }

        if (er.freq == EventRecurrence.MONTHLY) {
            if (er.bydayCount > 1) {
                return false;
            }
            if (er.bydayCount > 0 && er.bymonthdayCount > 0) {
                return false;
            }
        }

        return true;
    }

    // TODO don't lose data when getting data that our UI can't handle
    static private void copyEventRecurrenceToModel(final EventRecurrence er,
                                                   RecurrenceModel model) {
        // Freq:
        switch (er.freq) {
            case EventRecurrence.DAILY:
                model.freq = RecurrenceModel.FREQ_DAILY;
                break;
            case EventRecurrence.MONTHLY:
                model.freq = RecurrenceModel.FREQ_MONTHLY;
                break;
            case EventRecurrence.YEARLY:
                model.freq = RecurrenceModel.FREQ_YEARLY;
                break;
            case EventRecurrence.WEEKLY:
                model.freq = RecurrenceModel.FREQ_WEEKLY;
                break;
            default:
                throw new IllegalStateException("freq=" + er.freq);
        }

        // Interval:
        if (er.interval > 0) {
            model.interval = er.interval;
        }

        // End:
        // End by count:
        model.endCount = er.count;
        if (model.endCount > 0) {
            model.end = RecurrenceModel.END_BY_COUNT;
        }

        // End by date:
        if (!TextUtils.isEmpty(er.until)) {
            if (model.endDate == null) {
                model.endDate = new Time();
            }

            try {
                model.endDate.parse(er.until);
            } catch (TimeFormatException e) {
                model.endDate = null;
            }

            // LIMITATION: The UI can only handle END_BY_DATE or END_BY_COUNT
            if (model.end == RecurrenceModel.END_BY_COUNT && model.endDate != null) {
                throw new IllegalStateException("freq=" + er.freq);
            }

            model.end = RecurrenceModel.END_BY_DATE;
        }

        // Weekly: repeat by day of week or Monthly: repeat by nth day of week
        // in the month
        Arrays.fill(model.weeklyByDayOfWeek, false);
        if (er.bydayCount > 0) {
            int count = 0;
            for (int i = 0; i < er.bydayCount; i++) {
                int dayOfWeek = EventRecurrence.day2TimeDay(er.byday[i]);
                model.weeklyByDayOfWeek[dayOfWeek] = true;

                if (model.freq == RecurrenceModel.FREQ_MONTHLY &&
                        isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                    // LIMITATION: Can handle only (one) weekDayNum in nth or last and only
                    // when
                    // monthly
                    model.monthlyByDayOfWeek = dayOfWeek;
                    model.monthlyByNthDayOfWeek = er.bydayNum[i];
                    model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK;
                    count++;
                }
            }

            if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
                if (er.bydayCount != 1) {
                    // Can't handle 1st Monday and 2nd Wed
                    throw new IllegalStateException("Can handle only 1 byDayOfWeek in monthly");
                }
                if (count != 1) {
                    throw new IllegalStateException(
                            "Didn't specify which nth day of week to repeat for a monthly");
                }
            }
        }

        // Monthly by day of month
        if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
            if (er.bymonthdayCount == 1) {
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    throw new IllegalStateException(
                            "Can handle only by monthday or by nth day of week, not both");
                }
                model.monthlyByMonthDay = er.bymonthday[0];
                model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
            } else if (er.bymonthCount > 1) {
                // LIMITATION: Can handle only one month day
                throw new IllegalStateException("Can handle only one bymonthday");
            }
        }
    }

    static private void copyModelToEventRecurrence(final RecurrenceModel model,
                                                   EventRecurrence er) {
        if (model.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            throw new IllegalStateException("There's no recurrence");
        }

        // Freq
        er.freq = mFreqModelToEventRecurrence[model.freq];

        // Interval
        if (model.interval <= 1) {
            er.interval = 0;
        } else {
            er.interval = model.interval;
        }

        // End
        switch (model.end) {
            case RecurrenceModel.END_BY_DATE:
                if (model.endDate != null) {
                    model.endDate.switchTimezone(Time.TIMEZONE_UTC);
                    model.endDate.normalize(false);
                    er.until = model.endDate.format2445();
                    er.count = 0;
                } else {
                    throw new IllegalStateException("end = END_BY_DATE but endDate is null");
                }
                break;
            case RecurrenceModel.END_BY_COUNT:
                er.count = model.endCount;
                er.until = null;
                if (er.count <= 0) {
                    throw new IllegalStateException("count is " + er.count);
                }
                break;
            default:
                er.count = 0;
                er.until = null;
                break;
        }

        // Weekly && monthly repeat patterns
        er.bydayCount = 0;
        er.bymonthdayCount = 0;

        switch (model.freq) {
            case RecurrenceModel.FREQ_MONTHLY:
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
                    if (model.monthlyByMonthDay > 0) {
                        if (er.bymonthday == null || er.bymonthdayCount < 1) {
                            er.bymonthday = new int[1];
                        }
                        er.bymonthday[0] = model.monthlyByMonthDay;
                        er.bymonthdayCount = 1;
                    }
                } else if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    if (!isSupportedMonthlyByNthDayOfWeek(model.monthlyByNthDayOfWeek)) {
                        throw new IllegalStateException("month repeat by nth week but n is "
                                + model.monthlyByNthDayOfWeek);
                    }
                    int count = 1;
                    if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                        er.byday = new int[count];
                        er.bydayNum = new int[count];
                    }
                    er.bydayCount = count;
                    er.byday[0] = EventRecurrence.timeDay2Day(model.monthlyByDayOfWeek);
                    er.bydayNum[0] = model.monthlyByNthDayOfWeek;
                }
                break;
            case RecurrenceModel.FREQ_WEEKLY:
                int count = 0;
                for (int i = 0; i < 7; i++) {
                    if (model.weeklyByDayOfWeek[i]) {
                        count++;
                    }
                }

                if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                }
                er.bydayCount = count;

                for (int i = 6; i >= 0; i--) {
                    if (model.weeklyByDayOfWeek[i]) {
                        er.bydayNum[--count] = 0;
                        er.byday[count] = EventRecurrence.timeDay2Day(i);
                    }
                }
                break;
        }

        if (!canHandleRecurrenceRule(er)) {
            throw new IllegalStateException("UI generated recurrence that it can't handle. ER:"
                    + er.toString() + " Model: " + model.toString());
        }
    }

    public RecurrenceOptionCreator(Context context) {
        this(context, null);
    }

    public RecurrenceOptionCreator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spRecurrenceOptionCreatorStyle);
    }

    public RecurrenceOptionCreator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spRecurrenceOptionCreatorStyle,
                R.style.RecurrenceOptionCreatorStyle), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecurrenceOptionCreator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spRecurrenceOptionCreatorStyle,
                R.style.RecurrenceOptionCreatorStyle), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    // Initialize UI
    void initializeLayout() {
        int weekButtonUnselectedTextColor, weekButtonSelectedTextColor,
                weekButtonSelectedCircleColor;

        final TypedArray a = getContext()
                .obtainStyledAttributes(R.styleable.RecurrenceOptionCreator);
        try {
            mHeaderBackgroundColor = a.getColor(R.styleable.RecurrenceOptionCreator_spHeaderBackground, 0);

            int endDateFormat = a.getInt(R.styleable.RecurrenceOptionCreator_spEndDateFormat, 1);

            mEndDateFormatter = DateFormat.getDateInstance(
                    endDateFormat == 0 ?
                            DateFormat.SHORT : DateFormat.MEDIUM,
                    Locale.getDefault());

            weekButtonUnselectedTextColor =
                    a.getColor(R.styleable.RecurrenceOptionCreator_spWeekButtonUnselectedTextColor,
                            SUtils.COLOR_ACCENT);
            weekButtonSelectedTextColor =
                    a.getColor(R.styleable.RecurrenceOptionCreator_spWeekButtonSelectedTextColor,
                            SUtils.COLOR_TEXT_PRIMARY_INVERSE);
            weekButtonSelectedCircleColor =
                    a.getColor(R.styleable.RecurrenceOptionCreator_spWeekButtonSelectedCircleColor,
                            SUtils.COLOR_ACCENT);
        } finally {
            a.recycle();
        }

        mResources = getResources();

        LayoutInflater.from(getContext()).inflate(R.layout.recurrence_picker, this);

        mRecurrencePicker = findViewById(R.id.recurrence_picker);

        mDateOnlyPicker = (RecurrenceEndDatePicker) findViewById(R.id.date_only_picker);
        mDateOnlyPicker.setVisibility(View.GONE);

        // OK/Cancel buttons
        mButtonLayout = (DecisionButtonLayout) findViewById(R.id.roc_decision_button_layout);
        mButtonLayout.applyOptions(mButtonLayoutCallback);

        SUtils.setViewBackground(findViewById(R.id.freqSpinnerHolder), mHeaderBackgroundColor,
                SUtils.CORNER_TOP_LEFT | SUtils.CORNER_TOP_RIGHT);

        /** EFrequency Spinner {Repeat daily, Repeat weekly, Repeat monthly, Repeat yearly} **/

        mFreqSpinner = (Spinner) findViewById(R.id.freqSpinner);
        mFreqSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.recurrence_freq, R.layout.roc_freq_spinner_item);
        freqAdapter.setDropDownViewResource(R.layout.roc_spinner_dropdown_item);
        mFreqSpinner.setAdapter(freqAdapter);

        Drawable freqSpinnerBg = ContextCompat.getDrawable(getContext(), R.drawable.abc_spinner_mtrl_am_alpha);
        PorterDuffColorFilter cfFreqSpinner
                = new PorterDuffColorFilter(SUtils.COLOR_TEXT_PRIMARY_INVERSE,
                PorterDuff.Mode.SRC_IN);
        if (freqSpinnerBg != null) {
            freqSpinnerBg.setColorFilter(cfFreqSpinner);
            SUtils.setViewBackground(mFreqSpinner, freqSpinnerBg);
        }

        mInterval = (EditText) findViewById(R.id.interval);
        mInterval.addTextChangedListener(new minMaxTextWatcher(1, INTERVAL_DEFAULT, INTERVAL_MAX) {
            @Override
            void onChange(int v) {
                if (mIntervalResId != -1 && mInterval.getText().toString().length() > 0) {
                    mModel.interval = v;
                    updateIntervalText();
                    mInterval.requestLayout();
                }
            }
        });
        mIntervalPreText = (TextView) findViewById(R.id.intervalPreText);
        mIntervalPostText = (TextView) findViewById(R.id.intervalPostText);

        /** End Spinner {Forever, Until a date, For a number of events} **/

        mEndNeverStr = mResources.getString(R.string.recurrence_end_continously);
        mEndDateLabel = mResources.getString(R.string.recurrence_end_date_label);
        mEndCountLabel = mResources.getString(R.string.recurrence_end_count_label);

        mEndSpinnerArray.add(mEndNeverStr);
        mEndSpinnerArray.add(mEndDateLabel);
        mEndSpinnerArray.add(mEndCountLabel);
        mEndSpinner = (Spinner) findViewById(R.id.endSpinner);
        mEndSpinner.setOnItemSelectedListener(this);

        mEndSpinnerAdapter = new EndSpinnerAdapter(getContext(), mEndSpinnerArray,
                R.layout.roc_end_spinner_item, R.id.spinner_item, R.layout.roc_spinner_dropdown_item);
        mEndSpinner.setAdapter(mEndSpinnerAdapter);

        mEndCount = (EditText) findViewById(R.id.endCount);
        mEndCount.addTextChangedListener(new minMaxTextWatcher(1, COUNT_DEFAULT, COUNT_MAX) {
            @Override
            void onChange(int v) {
                if (mModel.endCount != v) {
                    mModel.endCount = v;
                    updateEndCountText();
                    mEndCount.requestLayout();
                }
            }
        });
        mPostEndCount = (TextView) findViewById(R.id.postEndCount);

        mEndDateTextView = (TextView) findViewById(R.id.endDate);
        mEndDateTextView.setOnClickListener(this);

        SUtils.setViewBackground(mEndDateTextView,
                SUtils.createButtonBg(getContext(), SUtils.COLOR_BUTTON_NORMAL,
                        SUtils.COLOR_CONTROL_HIGHLIGHT));

        // set default & checked state colors
        WeekButton.setStateColors(weekButtonUnselectedTextColor, weekButtonSelectedTextColor);

        // AOSP code handled this differently. It has been refactored to
        // let Android decide if we have enough space to show
        // all seven 'WeekButtons' inline. In this case, 'mWeekGroup2'
        // will be null (see @layout-w460dp/week_buttons).
        mWeekGroup = (LinearLayout) findViewById(R.id.weekGroup);
        mWeekGroup2 = (LinearLayout) findViewById(R.id.weekGroup2);

        // Only non-null when available width is < 460dp
        // Used only for positioning 'WeekButtons' in two rows
        // of 4 & 3.
        View eighthWeekDay = findViewById(R.id.week_day_8);
        if (eighthWeekDay != null)
            eighthWeekDay.setVisibility(View.INVISIBLE);

        // In Calendar.java day of week order e.g Sun = 1 ... Sat = 7
        //String[] dayOfWeekString = new DateFormatSymbols().getWeekdays();

        mMonthRepeatByDayOfWeekStrs = new String[7][];
        // from Time.SUNDAY as 0 through Time.SATURDAY as 6
        mMonthRepeatByDayOfWeekStrs[0] = mResources.getStringArray(R.array.repeat_by_nth_sun);
        mMonthRepeatByDayOfWeekStrs[1] = mResources.getStringArray(R.array.repeat_by_nth_mon);
        mMonthRepeatByDayOfWeekStrs[2] = mResources.getStringArray(R.array.repeat_by_nth_tues);
        mMonthRepeatByDayOfWeekStrs[3] = mResources.getStringArray(R.array.repeat_by_nth_wed);
        mMonthRepeatByDayOfWeekStrs[4] = mResources.getStringArray(R.array.repeat_by_nth_thurs);
        mMonthRepeatByDayOfWeekStrs[5] = mResources.getStringArray(R.array.repeat_by_nth_fri);
        mMonthRepeatByDayOfWeekStrs[6] = mResources.getStringArray(R.array.repeat_by_nth_sat);

        // In Time.java day of week order e.g. Sun = 0
        int idx = RecurrenceUtils.getFirstDayOfWeek();

        // In Calendar.java day of week order e.g Sun = 1 ... Sat = 7
        String[] dayOfWeekString = new DateFormatSymbols().getShortWeekdays();

        // CheckableDrawable's width & height
        int expandedWidthHeight = mResources
                .getDimensionPixelSize(R.dimen.week_button_state_on_circle_size);

        WeekButton[] tempWeekButtons = new WeekButton[7];
        tempWeekButtons[0] = (WeekButton) findViewById(R.id.week_day_1);
        tempWeekButtons[1] = (WeekButton) findViewById(R.id.week_day_2);
        tempWeekButtons[2] = (WeekButton) findViewById(R.id.week_day_3);
        tempWeekButtons[3] = (WeekButton) findViewById(R.id.week_day_4);
        tempWeekButtons[4] = (WeekButton) findViewById(R.id.week_day_5);
        tempWeekButtons[5] = (WeekButton) findViewById(R.id.week_day_6);
        tempWeekButtons[6] = (WeekButton) findViewById(R.id.week_day_7);

        for (int i = 0; i < mWeekByDayButtons.length; i++) {
            mWeekByDayButtons[idx] = tempWeekButtons[i];
            SUtils.setViewBackground(mWeekByDayButtons[idx],
                    new CheckableDrawable(weekButtonSelectedCircleColor,
                            false, expandedWidthHeight));
            mWeekByDayButtons[idx].setTextColor(weekButtonUnselectedTextColor);
            mWeekByDayButtons[idx].setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
            mWeekByDayButtons[idx].setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
            mWeekByDayButtons[idx].setOnCheckedChangeListener(this);

            if (++idx >= 7) {
                idx = 0;
            }
        }

        mMonthRepeatByRadioGroup = (RadioGroup) findViewById(R.id.monthGroup);
        mMonthRepeatByRadioGroup.setOnCheckedChangeListener(this);
        mRepeatMonthlyByNthDayOfWeek = (RadioButton)
                findViewById(R.id.repeatMonthlyByNthDayOfTheWeek);
        mRepeatMonthlyByNthDayOfMonth = (RadioButton)
                findViewById(R.id.repeatMonthlyByNthDayOfMonth);
    }

    public void initializeData(long currentlyChosenTime,
                               String timeZone, String recurrenceRule,
                               @NonNull OnRecurrenceSetListener callback) {
        mRecurrence.wkst = EventRecurrence.timeDay2Day(RecurrenceUtils.getFirstDayOfWeek());
        mRecurrenceSetListener = callback;

        mTime.set(currentlyChosenTime);

        if (!TextUtils.isEmpty(timeZone)) {
            mTime.timezone = timeZone;
        }
        mTime.normalize(false);

        // Time days of week: Sun=0, Mon=1, etc
        mModel.weeklyByDayOfWeek[mTime.weekDay] = true;

        if (!TextUtils.isEmpty(recurrenceRule)) {
            mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
            mRecurrence.parse(recurrenceRule);
            copyEventRecurrenceToModel(mRecurrence, mModel);
            // Leave today's day of week as checked by default in weekly view.
            if (mRecurrence.bydayCount == 0) {
                mModel.weeklyByDayOfWeek[mTime.weekDay] = true;
            }
        } else {
            // Default
            mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
        }

        if (mModel.endDate == null) {
            mModel.endDate = new Time(mTime);
            switch (mModel.freq) {
                case RecurrenceModel.FREQ_DAILY:
                case RecurrenceModel.FREQ_WEEKLY:
                    mModel.endDate.month += 1;
                    break;
                case RecurrenceModel.FREQ_MONTHLY:
                    mModel.endDate.month += 3;
                    break;
                case RecurrenceModel.FREQ_YEARLY:
                    mModel.endDate.year += 3;
                    break;
            }
            mModel.endDate.normalize(false);
        }

        togglePickerOptions();
        updateDialog();
        showRecurrencePicker();
    }

    private void togglePickerOptions() {
        if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            mFreqSpinner.setEnabled(false);
            mEndSpinner.setEnabled(false);
            mIntervalPreText.setEnabled(false);
            mInterval.setEnabled(false);
            mIntervalPostText.setEnabled(false);
            mMonthRepeatByRadioGroup.setEnabled(false);
            mEndCount.setEnabled(false);
            mPostEndCount.setEnabled(false);
            mEndDateTextView.setEnabled(false);
            mRepeatMonthlyByNthDayOfWeek.setEnabled(false);
            mRepeatMonthlyByNthDayOfMonth.setEnabled(false);
            for (Button button : mWeekByDayButtons) {
                button.setEnabled(false);
            }
        } else {
            findViewById(R.id.options).setEnabled(true);
            mFreqSpinner.setEnabled(true);
            mEndSpinner.setEnabled(true);
            mIntervalPreText.setEnabled(true);
            mInterval.setEnabled(true);
            mIntervalPostText.setEnabled(true);
            mMonthRepeatByRadioGroup.setEnabled(true);
            mEndCount.setEnabled(true);
            mPostEndCount.setEnabled(true);
            mEndDateTextView.setEnabled(true);
            mRepeatMonthlyByNthDayOfWeek.setEnabled(true);
            mRepeatMonthlyByNthDayOfMonth.setEnabled(true);
            for (Button button : mWeekByDayButtons) {
                button.setEnabled(true);
            }
        }
        updateDoneButtonState();
    }

    private void updateDoneButtonState() {
        if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            mButtonLayout.updateValidity(true);
            return;
        }

        if (mInterval.getText().toString().length() == 0) {
            mButtonLayout.updateValidity(false);
            return;
        }

        if (mEndCount.getVisibility() == View.VISIBLE &&
                mEndCount.getText().toString().length() == 0) {
            mButtonLayout.updateValidity(false);
            return;
        }

        if (mModel.freq == RecurrenceModel.FREQ_WEEKLY) {
            for (CompoundButton b : mWeekByDayButtons) {
                if (b.isChecked()) {
                    mButtonLayout.updateValidity(true);
                    return;
                }
            }
            mButtonLayout.updateValidity(false);
            return;
        }
        mButtonLayout.updateValidity(true);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mModel, mEndCount.hasFocus(),
                mRecurrencePicker.getVisibility() == View.VISIBLE ?
                        CurrentView.RECURRENCE_PICKER : CurrentView.DATE_ONLY_PICKER);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        final boolean endCountHasFocus = ss.getEndCountHasFocus();
        RecurrenceModel m = ss.getRecurrenceModel();
        if (m != null) {
            mModel = m;
        }

        mRecurrence.wkst = EventRecurrence.timeDay2Day(RecurrenceUtils.getFirstDayOfWeek());

        togglePickerOptions();
        updateDialog();

        if (ss.getCurrentView() == CurrentView.RECURRENCE_PICKER) {
            showRecurrencePicker();
            post(new Runnable() {
                @Override
                public void run() {
                    if (mEndCount != null && endCountHasFocus) {
                        mEndCount.requestFocus();
                    }
                }
            });
        } else {
            showDateOnlyPicker();
        }
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final RecurrenceModel mRecurrenceModel;
        private final boolean mEndCountHasFocus;
        private final CurrentView sCurrentView;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState,
                           RecurrenceModel recurrenceModel, boolean endCountHasFocus,
                           CurrentView currentView) {
            super(superState);
            mRecurrenceModel = recurrenceModel;
            mEndCountHasFocus = endCountHasFocus;
            sCurrentView = currentView;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mRecurrenceModel = in.readParcelable(RecurrenceModel.class.getClassLoader());
            mEndCountHasFocus = in.readByte() != 0;
            sCurrentView = CurrentView.valueOf(in.readString());
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mRecurrenceModel, flags);
            dest.writeByte((byte) (mEndCountHasFocus ? 1 : 0));
            dest.writeString(sCurrentView.name());
        }

        public RecurrenceModel getRecurrenceModel() {
            return mRecurrenceModel;
        }

        public boolean getEndCountHasFocus() {
            return mEndCountHasFocus;
        }

        public CurrentView getCurrentView() {
            return sCurrentView;
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

    public void updateDialog() {
        // Interval
        // Checking before setting because this causes infinite recursion
        // in afterTextWatcher
        final String intervalStr = Integer.toString(mModel.interval);
        if (!intervalStr.equals(mInterval.getText().toString())) {
            mInterval.setText(intervalStr);
        }

        mFreqSpinner.setSelection(mModel.freq);
        mWeekGroup.setVisibility(mModel.freq == RecurrenceModel.FREQ_WEEKLY ? View.VISIBLE : View.GONE);

        // mWeekGroup2 will be null when available width >= 460dp
        if (mWeekGroup2 != null) {
            mWeekGroup2.setVisibility(mModel.freq == RecurrenceModel.FREQ_WEEKLY ? View.VISIBLE : View.GONE);
        }

        mMonthRepeatByRadioGroup.setVisibility(mModel.freq == RecurrenceModel.FREQ_MONTHLY ? View.VISIBLE : View.GONE);

        switch (mModel.freq) {
            case RecurrenceModel.FREQ_DAILY:
                mIntervalResId = R.plurals.recurrence_interval_daily;
                break;

            case RecurrenceModel.FREQ_WEEKLY:
                mIntervalResId = R.plurals.recurrence_interval_weekly;
                for (int i = 0; i < 7; i++) {
                    mWeekByDayButtons[i].setCheckedNoAnimate(mModel.weeklyByDayOfWeek[i]);
                }
                break;

            case RecurrenceModel.FREQ_MONTHLY:
                mIntervalResId = R.plurals.recurrence_interval_monthly;

                if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
                    mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfMonth);
                } else if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfTheWeek);
                }

                if (mMonthRepeatByDayOfWeekStr == null) {
                    if (mModel.monthlyByNthDayOfWeek == 0) {
                        mModel.monthlyByNthDayOfWeek = (mTime.monthDay + 6) / 7;
                        // Since not all months have 5 weeks, we convert 5th NthDayOfWeek to
                        // -1 for last monthly day of the week
                        if (mModel.monthlyByNthDayOfWeek >= FIFTH_WEEK_IN_A_MONTH) {
                            mModel.monthlyByNthDayOfWeek = LAST_NTH_DAY_OF_WEEK;
                        }
                        mModel.monthlyByDayOfWeek = mTime.weekDay;
                    }

                    String[] monthlyByNthDayOfWeekStrs =
                            mMonthRepeatByDayOfWeekStrs[mModel.monthlyByDayOfWeek];

                    // TODO(psliwowski): Find a better way handle -1 indexes
                    int msgIndex = mModel.monthlyByNthDayOfWeek < 0 ? FIFTH_WEEK_IN_A_MONTH :
                            mModel.monthlyByNthDayOfWeek;
                    mMonthRepeatByDayOfWeekStr =
                            monthlyByNthDayOfWeekStrs[msgIndex - 1];
                    mRepeatMonthlyByNthDayOfWeek.setText(mMonthRepeatByDayOfWeekStr);
                }
                break;

            case RecurrenceModel.FREQ_YEARLY:
                mIntervalResId = R.plurals.recurrence_interval_yearly;
                break;
        }
        updateIntervalText();
        updateDoneButtonState();

        mEndSpinner.setSelection(mModel.end);
        if (mModel.end == RecurrenceModel.END_BY_DATE) {
            mEndDateTextView.setText(mEndDateFormatter.format(mModel.endDate.toMillis(false)));
        } else {
            if (mModel.end == RecurrenceModel.END_BY_COUNT) {
                // Checking before setting because this causes infinite
                // recursion
                // in afterTextWatcher
                final String countStr = Integer.toString(mModel.endCount);
                if (!countStr.equals(mEndCount.getText().toString())) {
                    mEndCount.setText(countStr);
                }
            }
        }
    }

    /**
     * @param endDateString String for end date option
     *                      displayed in End Spinner
     */
    @SuppressWarnings("unused")
    private void setEndSpinnerEndDateStr(final String endDateString) {
        mEndSpinnerArray.set(1, endDateString);
        mEndSpinnerAdapter.notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
    private void doToast() {
        Log.e(TAG, "Model = " + mModel.toString());
        String rrule;
        if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            rrule = "Not repeating";
        } else {
            copyModelToEventRecurrence(mModel, mRecurrence);
            rrule = mRecurrence.toString();
        }

        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getContext(), rrule,
                Toast.LENGTH_LONG);
        mToast.show();
    }

    // TODO Test and update for Right-to-Left
    private void updateIntervalText() {
        if (mIntervalResId == -1) {
            return;
        }

        final String INTERVAL_COUNT_MARKER = "%d";
        String intervalString = mResources.getQuantityString(mIntervalResId, mModel.interval);
        int markerStart = intervalString.indexOf(INTERVAL_COUNT_MARKER);

        if (markerStart != -1) {
            int postTextStart = markerStart + INTERVAL_COUNT_MARKER.length();
            mIntervalPostText.setText(intervalString.substring(postTextStart,
                    intervalString.length()).trim());
            mIntervalPreText.setText(intervalString.substring(0, markerStart).trim());
        }
    }

    /**
     * Update the "Repeat for N events" end option with the proper string values
     * based on the value that has been entered for N.
     */
    private void updateEndCountText() {
        final String END_COUNT_MARKER = "%d";
        String endString = mResources.getQuantityString(R.plurals.recurrence_end_count,
                mModel.endCount);
        int markerStart = endString.indexOf(END_COUNT_MARKER);

        if (markerStart != -1) {
            if (markerStart == 0) {
                Log.e(TAG, "No text to put in to recurrence's end spinner.");
            } else {
                int postTextStart = markerStart + END_COUNT_MARKER.length();
                mPostEndCount.setText(endString.substring(postTextStart,
                        endString.length()).trim());
            }
        }
    }

    // Implements OnItemSelectedListener interface
    // Freq spinner
    // End spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mFreqSpinner) {
            mModel.freq = position;
        } else if (parent == mEndSpinner) {
            switch (position) {
                case RecurrenceModel.END_NEVER:
                    mModel.end = RecurrenceModel.END_NEVER;
                    break;
                case RecurrenceModel.END_BY_DATE:
                    mModel.end = RecurrenceModel.END_BY_DATE;
                    break;
                case RecurrenceModel.END_BY_COUNT:
                    mModel.end = RecurrenceModel.END_BY_COUNT;

                    if (mModel.endCount <= 1) {
                        mModel.endCount = 1;
                    } else if (mModel.endCount > COUNT_MAX) {
                        mModel.endCount = COUNT_MAX;
                    }
                    updateEndCountText();
                    break;
            }
            mEndCount.setVisibility(mModel.end == RecurrenceModel.END_BY_COUNT ? View.VISIBLE
                    : View.GONE);
            mEndDateTextView.setVisibility(mModel.end == RecurrenceModel.END_BY_DATE ? View.VISIBLE
                    : View.GONE);
            mPostEndCount.setVisibility(
                    mModel.end == RecurrenceModel.END_BY_COUNT && !mHidePostEndCount ?
                            View.VISIBLE : View.GONE);

        }
        updateDialog();
    }

    // Implements OnItemSelectedListener interface
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onDateSet(RecurrenceEndDatePicker view, int year, int monthOfYear, int dayOfMonth) {
        showRecurrencePicker();

        if (mModel.endDate == null) {
            mModel.endDate = new Time(mTime.timezone);
            mModel.endDate.hour = mModel.endDate.minute = mModel.endDate.second = 0;
        }
        mModel.endDate.year = year;
        mModel.endDate.month = monthOfYear;
        mModel.endDate.monthDay = dayOfMonth;
        mModel.endDate.normalize(false);
        updateDialog();
    }

    @Override
    public void onDateOnlyPickerCancelled(RecurrenceEndDatePicker view) {
        showRecurrencePicker();
    }

    // Implements OnCheckedChangeListener interface
    // Week repeat by day of week
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int itemIdx = -1;
        for (int i = 0; i < 7; i++) {
            if (itemIdx == -1 && buttonView == mWeekByDayButtons[i]) {
                itemIdx = i;
                mModel.weeklyByDayOfWeek[i] = isChecked;
            }
        }
        updateDialog();
    }

    // Implements android.widget.RadioGroup.OnCheckedChangeListener interface
    // Month repeat by radio buttons
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.repeatMonthlyByNthDayOfMonth) {
            mModel.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
        } else if (checkedId == R.id.repeatMonthlyByNthDayOfTheWeek) {
            mModel.monthlyRepeat = RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK;
        }
        updateDialog();
    }

    // Implements OnClickListener interface
    // EndDate button
    // Done button
    @Override
    public void onClick(View v) {
        if (mEndDateTextView == v) {
            showDateOnlyPicker();
        }
    }

    private void showRecurrencePicker() {
        mDateOnlyPicker.setVisibility(View.GONE);
        mRecurrencePicker.setVisibility(View.VISIBLE);
    }

    private void showDateOnlyPicker() {
        mDateOnlyPicker.init(mModel.endDate.year,
                mModel.endDate.month, mModel.endDate.monthDay, this);
        mDateOnlyPicker.setFirstDayOfWeek(RecurrenceUtils.getFirstDayOfWeekAsCalendar());

        mRecurrencePicker.setVisibility(View.GONE);
        mDateOnlyPicker.setVisibility(View.VISIBLE);
    }

    public interface OnRecurrenceSetListener {
        void onRecurrenceSet(String rrule);

        void onCancelled();
    }

    private class EndSpinnerAdapter extends ArrayAdapter<CharSequence> {
        final String END_DATE_MARKER = "%s";
        final String END_COUNT_MARKER = "%d";

        private LayoutInflater mInflater;
        private int mItemLayoutId, mDropDownLayoutId, mTextResourceId;
        private ArrayList<CharSequence> mStrings;
        private String mEndDateString;
        private boolean mUseFormStrings;

        /**
         * @param context          Context
         * @param strings          {Forever, Until a date, For a number of events}
         * @param itemLayoutId     @Layout resource used for displaying
         *                         selected option
         * @param textResourceId   ViewID for the 'TextView' in 'itemLayoutId'
         * @param dropDownLayoutId @Layout resource used for displaying
         *                         available options in the dropdown menu
         */
        public EndSpinnerAdapter(Context context, ArrayList<CharSequence> strings,
                                 int itemLayoutId, int textResourceId, int dropDownLayoutId) {
            super(context, itemLayoutId, strings);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mItemLayoutId = itemLayoutId;
            mTextResourceId = textResourceId;
            mDropDownLayoutId = dropDownLayoutId;
            mStrings = strings;
            mEndDateString = getResources().getString(R.string.recurrence_end_date);

            // If either date or count strings don't translate well, such that we aren't assured
            // to have some text available to be placed in the spinner, then we'll have to use
            // the more form-like versions of both strings instead.
            int markerStart = mEndDateString.indexOf(END_DATE_MARKER);
            if (markerStart <= 0) {
                // The date string does not have any text before the "%s" so we'll have to use the
                // more form-like strings instead.
                mUseFormStrings = true;
            } else {
                String countEndStr = getResources().getQuantityString(
                        R.plurals.recurrence_end_count, 1);
                markerStart = countEndStr.indexOf(END_COUNT_MARKER);
                if (markerStart <= 0) {
                    // The count string does not have any text before the "%d" so we'll have to use
                    // the more form-like strings instead.
                    mUseFormStrings = true;
                }
            }

            if (mUseFormStrings) {
                // We'll have to set the layout for the spinner to be weight=0 so it doesn't
                // take up too much space.
                mEndSpinner.setLayoutParams(
                        new TableLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            // Check if we can recycle the view
            if (convertView == null) {
                v = mInflater.inflate(mItemLayoutId, parent, false);
            } else {
                v = convertView;
            }

            TextView item = (TextView) v.findViewById(mTextResourceId);
            int markerStart;
            switch (position) {
                case RecurrenceModel.END_NEVER:
                    item.setText(mStrings.get(RecurrenceModel.END_NEVER));
                    break;
                case RecurrenceModel.END_BY_DATE:
                    markerStart = mEndDateString.indexOf(END_DATE_MARKER);

                    if (markerStart != -1) {
                        if (mUseFormStrings || markerStart == 0) {
                            // If we get here, the translation of "Until" doesn't work correctly,
                            // so we'll just set the whole "Until a date" string.
                            item.setText(mEndDateLabel);
                        } else {
                            item.setText(mEndDateString.substring(0, markerStart).trim());
                        }
                    }
                    break;
                case RecurrenceModel.END_BY_COUNT:
                    String endString = mResources.getQuantityString(R.plurals.recurrence_end_count,
                            mModel.endCount);
                    markerStart = endString.indexOf(END_COUNT_MARKER);

                    if (markerStart != -1) {
                        if (mUseFormStrings || markerStart == 0) {
                            // If we get here, the translation of "For" doesn't work correctly,
                            // so we'll just set the whole "For a number of events" string.
                            item.setText(mEndCountLabel);
                            // Also, we'll hide the " events" that would have been at the end.
                            mPostEndCount.setVisibility(View.GONE);
                            // Use this flag so the onItemSelected knows whether to show it later.
                            mHidePostEndCount = true;
                        } else {
                            int postTextStart = markerStart + END_COUNT_MARKER.length();
                            mPostEndCount.setText(endString.substring(postTextStart,
                                    endString.length()).trim());
                            // In case it's a recycled view that wasn't visible.
                            if (mModel.end == RecurrenceModel.END_BY_COUNT) {
                                mPostEndCount.setVisibility(View.VISIBLE);
                            }
                            if (endString.charAt(markerStart - 1) == ' ') {
                                markerStart--;
                            }
                            item.setText(endString.substring(0, markerStart).trim());
                        }
                    }
                    break;
                default:
                    v = null;
                    break;
            }

            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v;
            // Check if we can recycle the view
            if (convertView == null) {
                v = mInflater.inflate(mDropDownLayoutId, parent, false);
            } else {
                v = convertView;
            }

            TextView item = (TextView) v.findViewById(mTextResourceId);
            item.setText(mStrings.get(position));

            return v;
        }
    }
}

