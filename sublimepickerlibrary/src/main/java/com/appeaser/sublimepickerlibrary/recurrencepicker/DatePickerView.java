/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.common.ButtonLayout;
import com.appeaser.sublimepickerlibrary.datepicker.DayPickerView;
import com.appeaser.sublimepickerlibrary.datepicker.OnSublimeDateChangedListener;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

/**
 * A stripped-down version of 'DatePicker'.
 * The facility to pick an year has been removed.
 * Header view is also not available.
 * Used to pick an end-date while creating a
 * recurrence-rule with 'RecurrenceOptionCreator'
 */
public class DatePickerView extends LinearLayout {
    // The context
    protected Context mContext;

    // The current locale
    protected Locale mCurrentLocale;

    // Callbacks
    protected OnDateChangedListener mOnDateChangedListener;

    private static final int USE_LOCALE = 0;

    private static final int UNINITIALIZED = -1;
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final int YEAR_VIEW = 1;

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;

    private static final int ANIMATION_DURATION = 300;

    private static final int MONTH_INDEX = 0;
    private static final int DAY_INDEX = 1;
    private static final int YEAR_INDEX = 2;

    private int[] DEFAULT_VIEW_INDICES = {0, 1, 2};

    private SimpleDateFormat mYearFormat = new SimpleDateFormat("y", Locale.getDefault());
    private SimpleDateFormat mDayFormat = new SimpleDateFormat("d", Locale.getDefault());

    private DayPickerView mDayPickerView;

    private boolean mIsEnabled = true;

    // Accessibility strings.
    private String mDayPickerDescription;
    private String mSelectDay;

    private OnDateChangedListener mDateChangedListener;
    private OnDateSetListener mOnDateSetListener;

    private Calendar mCurrentDate;
    private Calendar mTempDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;

    private int mFirstDayOfWeek = USE_LOCALE;

    private HashSet<OnSublimeDateChangedListener> mListeners
            = new HashSet<OnSublimeDateChangedListener>();

    private ButtonLayout mButtonLayout;

    private ButtonLayout.Callback mButtonLayoutCallback = new ButtonLayout.Callback() {
        @Override
        public void onOkay() {
            if (mOnDateSetListener != null) {
                mOnDateSetListener.onDateSet(DatePickerView.this, getYear(),
                        getMonth(), getDayOfMonth());
            }
        }

        @Override
        public void onCancel() {
            if (mOnDateSetListener != null) {
                mOnDateSetListener.onDateOnlyPickerCancelled(DatePickerView.this);
            }
        }

        @Override
        public void onSwitch() {
            // Intentionally left empty
        }
    };

    public DatePickerView(Context context) {
        this(context, null);
    }

    public DatePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spDatePickerStyle);
    }

    public DatePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeLayout(attrs, defStyleAttr, R.style.SublimeDatePickerStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DatePickerView(Context context, AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeLayout(attrs, defStyleAttr, defStyleRes);
    }

    private void initializeLayout(AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        mContext = getContext();

        final Resources res = getResources();

        setOrientation(VERTICAL);

        setCurrentLocale(Locale.getDefault());

        mMinDate = getCalendarForLocale(mMinDate, mCurrentLocale);
        mMaxDate = getCalendarForLocale(mMaxDate, mCurrentLocale);
        mTempDate = getCalendarForLocale(mMaxDate, mCurrentLocale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, mCurrentLocale);

        mMinDate.set(DEFAULT_START_YEAR, 1, 1);
        mMaxDate.set(DEFAULT_END_YEAR, 12, 31);

        LayoutInflater.from(mContext).inflate(R.layout.end_date_picker, this, true);

        mButtonLayout = (ButtonLayout) findViewById(R.id.button_layout);
        mButtonLayout.applyOptions(false, mButtonLayoutCallback,
                SublimeOptions.Picker.INVALID);

        mDayPickerView = (DayPickerView) findViewById(R.id.dayPickerView);
        mDayPickerView.setFirstDayOfWeek(mFirstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
        mDayPickerView.setOnDaySelectedListener(mOnDaySelectedListener);

        final TypedArray typedArray = mContext.obtainStyledAttributes(attrs,
                R.styleable.SublimeDatePicker, defStyleAttr, defStyleRes);
        try {

            final int firstDayOfWeek = typedArray.getInt(R.styleable.SublimeDatePicker_firstDayOfWeek, 0);

            if (firstDayOfWeek != 0) {
                setFirstDayOfWeek(firstDayOfWeek);
            }
        } finally {
            typedArray.recycle();
        }

        mDayPickerDescription = res.getString(R.string.day_picker_description);
        mSelectDay = res.getString(R.string.select_day);

        setCurrentView();
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale      The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    private void announceDateForAccessibility() {
        // Accessibility.
        long millis = mCurrentDate.getTimeInMillis();
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
        String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
        announceForAccessibility(fullDateText);
    }

    private void setCurrentView() {
        long millis = mCurrentDate.getTimeInMillis();
        mDayPickerView.setDate(getSelectedDay().getTimeInMillis());
        final int flags = DateUtils.FORMAT_SHOW_DATE;
        final String dayString = DateUtils.formatDateTime(mContext, millis, flags);
        mDayPickerView.setContentDescription(mDayPickerDescription + ": " + dayString);
        mDayPickerView.announceForAccessibility(mSelectDay);
    }

    public void init(int year, int monthOfYear, int dayOfMonth,
                     //OnDateChangedListener callBack) {
                     OnDateSetListener callBack) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, monthOfYear);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        //mDateChangedListener = callBack;
        mOnDateSetListener = callBack;

        onDateChanged(false, false);
    }

    public void updateDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        onDateChanged(false, true);
    }

    private void onDateChanged(boolean fromUser, boolean callbackToClient) {
        if (callbackToClient && mDateChangedListener != null) {
            final int year = mCurrentDate.get(Calendar.YEAR);
            final int monthOfYear = mCurrentDate.get(Calendar.MONTH);
            final int dayOfMonth = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            mDateChangedListener.onDateChanged(this, year, monthOfYear, dayOfMonth);
        }

        for (OnSublimeDateChangedListener listener : mListeners) {
            listener.onDateChanged();
        }

        mDayPickerView.setDate(getSelectedDay().getTimeInMillis());

        if (fromUser) {
            tryVibrate();
            announceDateForAccessibility();
        }
    }

    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.before(mTempDate)) {
            mCurrentDate.setTimeInMillis(minDate);
            onDateChanged(false, true);
        }
        mMinDate.setTimeInMillis(minDate);
        mDayPickerView.setMinDate(minDate);
    }

    public long getMinDate() {
        return mMinDate.getTimeInMillis();
    }

    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.after(mTempDate)) {
            mCurrentDate.setTimeInMillis(maxDate);
            onDateChanged(false, true);
        }
        mMaxDate.setTimeInMillis(maxDate);
        mDayPickerView.setMaxDate(maxDate);
    }

    public long getMaxDate() {
        return mMaxDate.getTimeInMillis();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }

        mFirstDayOfWeek = firstDayOfWeek;

        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek() {
        if (mFirstDayOfWeek != USE_LOCALE) {
            return mFirstDayOfWeek;
        }
        return mCurrentDate.getFirstDayOfWeek();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDayPickerView.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mYearFormat = new SimpleDateFormat("y", newConfig.locale);
        mDayFormat = new SimpleDateFormat("d", newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        final int year = mCurrentDate.get(Calendar.YEAR);
        final int month = mCurrentDate.get(Calendar.MONTH);
        final int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);

        int listPosition = mDayPickerView.getMostVisiblePosition();

        return new SavedState(superState, year, month, day, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), listPosition);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());

        //announceDateForAccessibility(false);
        setCurrentView();

        final int listPosition = ss.getListPosition();
        if (listPosition != -1) {
            mDayPickerView.postSetSelection(listPosition);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mCurrentDate.getTime().toString());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(DatePicker.class.getName());
    }

    public void registerOnDateChangedListener(OnSublimeDateChangedListener listener) {
        mListeners.add(listener);
    }

    public Calendar getSelectedDay() {
        return mCurrentDate;
    }

    public void tryVibrate() {
        // Using a different haptic feedback constant
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY
                                     /*(5) - HapticFeedbackConstants.CALENDAR_DATE*/);
    }

    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private final DayPickerView.OnDaySelectedListener
            mOnDaySelectedListener = new DayPickerView.OnDaySelectedListener() {
        @Override
        public void onDaySelected(DayPickerView view, Calendar day) {
            mCurrentDate.setTimeInMillis(day.getTimeInMillis());
            onDateChanged(true, true);
        }
    };

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final int mSelectedYear;
        private final int mSelectedMonth;
        private final int mSelectedDay;
        private final long mMinDate;
        private final long mMaxDate;
        private final int mListPosition;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day,
                           long minDate, long maxDate, int listPosition) {
            super(superState);
            mSelectedYear = year;
            mSelectedMonth = month;
            mSelectedDay = day;
            mMinDate = minDate;
            mMaxDate = maxDate;
            mListPosition = listPosition;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSelectedYear = in.readInt();
            mSelectedMonth = in.readInt();
            mSelectedDay = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
            mListPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYear);
            dest.writeInt(mSelectedMonth);
            dest.writeInt(mSelectedDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mListPosition);
        }

        public int getSelectedDay() {
            return mSelectedDay;
        }

        public int getSelectedMonth() {
            return mSelectedMonth;
        }

        public int getSelectedYear() {
            return mSelectedYear;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public int getListPosition() {
            return mListPosition;
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

    protected void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }
        mCurrentLocale = locale;
    }

    protected void onValidationChanged(boolean valid) {
        // Todo: Handle validity change
        mButtonLayout.updateValidity(valid);
    }

    /**
     * The callback used to indicate the user changes\d the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link java.util.Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateChanged(DatePickerView view, int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {

        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link java.util.Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateSet(DatePickerView view, int year, int monthOfYear, int dayOfMonth);

        void onDateOnlyPickerCancelled(DatePickerView view);
    }
}
