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

package com.appeaser.sublimepickerlibrary.datepicker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.common.DateTimePatternHelper;
import com.appeaser.sublimepickerlibrary.utilities.AccessibilityUtils;
import com.appeaser.sublimepickerlibrary.utilities.Config;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;
import com.appeaser.sublimepickerlibrary.utilities.TextColorHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SublimeDatePicker extends FrameLayout {
    private static final String TAG = SublimeDatePicker.class.getSimpleName();

    private static final int UNINITIALIZED = -1;
    private static final int VIEW_MONTH_DAY = 0;
    private static final int VIEW_YEAR = 1;

    private static final int RANGE_ACTIVATED_NONE = 0;
    private static final int RANGE_ACTIVATED_START = 1;
    private static final int RANGE_ACTIVATED_END = 2;

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;

    private Context mContext;

    private SimpleDateFormat mYearFormat;
    private SimpleDateFormat mMonthDayFormat;

    // Top-level container.
    private ViewGroup mContainer;

    // Header views.
    private LinearLayout llHeaderDateSingleCont;
    private TextView mHeaderYear;
    private TextView mHeaderMonthDay;
    private LinearLayout llHeaderDateRangeCont;
    private TextView tvHeaderDateStart;
    private TextView tvHeaderDateEnd;
    private ImageView ivHeaderDateReset;

    // Picker views.
    private ViewAnimator mAnimator;
    private DayPickerView mDayPickerView;
    private YearPickerView mYearPickerView;

    // Accessibility strings.
    private String mSelectDay;
    private String mSelectYear;

    private SublimeDatePicker.OnDateChangedListener mDateChangedListener;

    private int mCurrentView = UNINITIALIZED;

    private SelectedDate mCurrentDate;
    private Calendar mTempDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;

    private int mFirstDayOfWeek;

    private Locale mCurrentLocale;

    private DatePickerValidationCallback mValidationCallback;

    private int mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

    private boolean mIsInLandscapeMode;

    public SublimeDatePicker(Context context) {
        this(context, null);
    }

    public SublimeDatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spDatePickerStyle);
    }

    public SublimeDatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeLayout(attrs, defStyleAttr, R.style.SublimeDatePickerStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SublimeDatePicker(Context context, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeLayout(attrs, defStyleAttr, defStyleRes);
    }

    private void initializeLayout(AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        mContext = getContext();
        mIsInLandscapeMode = mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        setCurrentLocale(Locale.getDefault());
        mCurrentDate = new SelectedDate(Calendar.getInstance(mCurrentLocale));
        mTempDate = Calendar.getInstance(mCurrentLocale);
        mMinDate = Calendar.getInstance(mCurrentLocale);
        mMaxDate = Calendar.getInstance(mCurrentLocale);

        mMinDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1);
        mMaxDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);

        final Resources res = getResources();
        final TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.SublimeDatePicker, defStyleAttr, defStyleRes);
        final LayoutInflater inflater
                = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final int layoutResourceId = R.layout.date_picker_layout;

        try {
            // Set up and attach container.
            mContainer = (ViewGroup) inflater.inflate(layoutResourceId, this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addView(mContainer);

        // Set up header views.
        final ViewGroup header = (ViewGroup) mContainer.findViewById(R.id.date_picker_header);
        llHeaderDateSingleCont = (LinearLayout) header.findViewById(R.id.ll_header_date_single_cont);
        mHeaderYear = (TextView) header.findViewById(R.id.date_picker_header_year);
        mHeaderYear.setOnClickListener(mOnHeaderClickListener);
        mHeaderMonthDay = (TextView) header.findViewById(R.id.date_picker_header_date);
        mHeaderMonthDay.setOnClickListener(mOnHeaderClickListener);

        llHeaderDateRangeCont = (LinearLayout) header.findViewById(R.id.ll_header_date_range_cont);
        tvHeaderDateStart = (TextView) header.findViewById(R.id.tv_header_date_start);
        tvHeaderDateStart.setOnClickListener(mOnHeaderClickListener);
        tvHeaderDateEnd = (TextView) header.findViewById(R.id.tv_header_date_end);
        tvHeaderDateEnd.setOnClickListener(mOnHeaderClickListener);
        ivHeaderDateReset = (ImageView) header.findViewById(R.id.iv_header_date_reset);
        ivHeaderDateReset.setOnClickListener(mOnHeaderClickListener);

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

        SUtils.setImageTintList(ivHeaderDateReset, ColorStateList.valueOf(iconColor));
        SUtils.setViewBackground(ivHeaderDateReset, SUtils.createOverflowButtonBg(pressedStateBgColor));

        ColorStateList headerTextColor
                = a.getColorStateList(R.styleable.SublimeDatePicker_spHeaderTextColor);

        if (headerTextColor == null) {
            headerTextColor = TextColorHelper.resolveMaterialHeaderTextColor();
        }

        if (headerTextColor != null) {
            mHeaderYear.setTextColor(headerTextColor);
            mHeaderMonthDay.setTextColor(headerTextColor);
        }

        // Set up header background, if available.
        if (SUtils.isApi_22_OrHigher()) {
            if (a.hasValueOrEmpty(R.styleable.SublimeDatePicker_spHeaderBackground)) {
                SUtils.setViewBackground(header,
                        a.getDrawable(R.styleable.SublimeDatePicker_spHeaderBackground));
            }
        } else {
            if (a.hasValue(R.styleable.SublimeDatePicker_spHeaderBackground)) {
                SUtils.setViewBackground(header, a.getDrawable(R.styleable.SublimeDatePicker_spHeaderBackground));
            }
        }

        int firstDayOfWeek = a.getInt(R.styleable.SublimeDatePicker_spFirstDayOfWeek,
                mCurrentDate.getFirstDate().getFirstDayOfWeek());

        final String minDate = a.getString(R.styleable.SublimeDatePicker_spMinDate);
        final String maxDate = a.getString(R.styleable.SublimeDatePicker_spMaxDate);

        // Set up min and max dates.
        final Calendar tempDate = Calendar.getInstance();

        if (!SUtils.parseDate(minDate, tempDate)) {
            tempDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1);
        }

        final long minDateMillis = tempDate.getTimeInMillis();

        if (!SUtils.parseDate(maxDate, tempDate)) {
            tempDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);
        }

        final long maxDateMillis = tempDate.getTimeInMillis();

        if (maxDateMillis < minDateMillis) {
            throw new IllegalArgumentException("maxDate must be >= minDate");
        }

        final long setDateMillis = SUtils.constrain(
                System.currentTimeMillis(), minDateMillis, maxDateMillis);

        mMinDate.setTimeInMillis(minDateMillis);
        mMaxDate.setTimeInMillis(maxDateMillis);
        mCurrentDate.setTimeInMillis(setDateMillis);

        a.recycle();

        // Set up picker container.
        mAnimator = (ViewAnimator) mContainer.findViewById(R.id.animator);

        // Set up day picker view.
        mDayPickerView = (DayPickerView) mAnimator.findViewById(R.id.date_picker_day_picker);
        setFirstDayOfWeek(firstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate);
        mDayPickerView.setProxyDaySelectionEventListener(mProxyDaySelectionEventListener);

        // Set up year picker view.
        mYearPickerView = (YearPickerView) mAnimator.findViewById(R.id.date_picker_year_picker);
        mYearPickerView.setRange(mMinDate, mMaxDate);
        mYearPickerView.setOnYearSelectedListener(mOnYearSelectedListener);

        // Set up content descriptions.
        mSelectDay = res.getString(R.string.select_day);
        mSelectYear = res.getString(R.string.select_year);

        // Initialize for current locale. This also initializes the date, so no
        // need to call onDateChanged.
        onLocaleChanged(mCurrentLocale);

        setCurrentView(VIEW_MONTH_DAY);
    }

    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private final DayPickerView.ProxyDaySelectionEventListener mProxyDaySelectionEventListener
            = new DayPickerView.ProxyDaySelectionEventListener() {
        @Override
        public void onDaySelected(DayPickerView view, Calendar day) {
            if (Config.DEBUG) {
                Log.i(TAG, "tvHeaderDateStart is activated? " + tvHeaderDateStart.isActivated());
                Log.i(TAG, "tvHeaderDateEnd is activated? " + tvHeaderDateEnd.isActivated());
            }

            boolean goToPosition = true;

            if (llHeaderDateRangeCont.getVisibility() == View.VISIBLE) {
                // We're in Range selection mode
                if (tvHeaderDateStart.isActivated()) {
                    if (SelectedDate.compareDates(day, mCurrentDate.getEndDate()) > 0) {
                        mCurrentDate = new SelectedDate(day);
                    } else {
                        goToPosition = false;
                        mCurrentDate = new SelectedDate(day, mCurrentDate.getEndDate());
                    }
                } else if (tvHeaderDateEnd.isActivated()) {
                    if (SelectedDate.compareDates(day, mCurrentDate.getStartDate()) < 0) {
                        mCurrentDate = new SelectedDate(day);
                    } else {
                        goToPosition = false;
                        mCurrentDate = new SelectedDate(mCurrentDate.getStartDate(), day);
                    }
                } else { // Should never happen
                    if (Config.DEBUG) {
                        Log.i(TAG, "onDaySelected: Neither tvDateStart, nor tvDateEnd is activated");
                    }
                }
            } else {
                mCurrentDate = new SelectedDate(day);
            }

            onDateChanged(true, false, goToPosition);
        }

        @Override
        public void onDateRangeSelectionStarted(@NonNull SelectedDate selectedDate) {
            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false, false);
        }

        @Override
        public void onDateRangeSelectionEnded(@Nullable SelectedDate selectedDate) {
            if (selectedDate != null) {
                mCurrentDate = new SelectedDate(selectedDate);
                onDateChanged(false, false, false);
            }
        }

        @Override
        public void onDateRangeSelectionUpdated(@NonNull SelectedDate selectedDate) {
            if (Config.DEBUG) {
                Log.i(TAG, "onDateRangeSelectionUpdated: " + selectedDate.toString());
            }

            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false, false);
        }
    };

    /**
     * Listener called when the user selects a year in the year picker view.
     */
    private final YearPickerView.OnYearSelectedListener mOnYearSelectedListener
            = new YearPickerView.OnYearSelectedListener() {
        @Override
        public void onYearChanged(YearPickerView view, int year) {
            // If the newly selected month / year does not contain the
            // currently selected day number, change the selected day number
            // to the last day of the selected month or year.
            // e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
            // e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
            final int day = mCurrentDate.getStartDate().get(Calendar.DAY_OF_MONTH);
            final int month = mCurrentDate.getStartDate().get(Calendar.MONTH);
            final int daysInMonth = SUtils.getDaysInMonth(month, year);
            if (day > daysInMonth) {
                mCurrentDate.set(Calendar.DAY_OF_MONTH, daysInMonth);
            }

            mCurrentDate.set(Calendar.YEAR, year);
            onDateChanged(true, true, true);

            // Automatically switch to day picker.
            setCurrentView(VIEW_MONTH_DAY);
        }
    };

    /**
     * Listener called when the user clicks on a header item.
     */
    private final OnClickListener mOnHeaderClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SUtils.vibrateForDatePicker(SublimeDatePicker.this);

            if (v.getId() == R.id.date_picker_header_year) {
                setCurrentView(VIEW_YEAR);
            } else if (v.getId() == R.id.date_picker_header_date) {
                setCurrentView(VIEW_MONTH_DAY);
            } else if (v.getId() == R.id.tv_header_date_start) {
                mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_START;
                tvHeaderDateStart.setActivated(true);
                tvHeaderDateEnd.setActivated(false);
            } else if (v.getId() == R.id.tv_header_date_end) {
                mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_END;
                tvHeaderDateStart.setActivated(false);
                tvHeaderDateEnd.setActivated(true);
            } else if (v.getId() == R.id.iv_header_date_reset) {
                mCurrentDate = new SelectedDate(mCurrentDate.getStartDate());
                onDateChanged(true, false, true);
            }
        }
    };

    private void onLocaleChanged(Locale locale) {
        final TextView headerYear = mHeaderYear;
        if (headerYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        // Update the date formatter.
        String datePattern;

        if (SUtils.isApi_18_OrHigher()) {
            datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EMMMd");
        } else {
            datePattern = DateTimePatternHelper.getBestDateTimePattern(locale, DateTimePatternHelper.PATTERN_EMMMd);
        }

        mMonthDayFormat = new SimpleDateFormat(datePattern, locale);
        mYearFormat = new SimpleDateFormat("y", locale);

        // Update the header text.
        onCurrentDateChanged(false);
    }

    private void onCurrentDateChanged(boolean announce) {
        if (mHeaderYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        final String year = mYearFormat.format(mCurrentDate.getStartDate().getTime());
        mHeaderYear.setText(year);

        final String monthDay = mMonthDayFormat.format(mCurrentDate.getStartDate().getTime());
        mHeaderMonthDay.setText(monthDay);

        final String yearStrStart = mYearFormat.format(mCurrentDate.getStartDate().getTime());
        final String monthDayStrStart = mMonthDayFormat.format(mCurrentDate.getStartDate().getTime());
        final String dateStrStart = yearStrStart + "\n" + monthDayStrStart;

        final String yearStrEnd = mYearFormat.format(mCurrentDate.getEndDate().getTime());
        final String monthDayStrEnd = mMonthDayFormat.format(mCurrentDate.getEndDate().getTime());
        final String dateStrEnd = yearStrEnd + "\n" + monthDayStrEnd;

        SpannableString spDateStart = new SpannableString(dateStrStart);
        // If textSize is 34dp for land, use 0.47f
        //spDateStart.setSpan(new RelativeSizeSpan(mIsInLandscapeMode ? 0.47f : 0.7f),
        spDateStart.setSpan(new RelativeSizeSpan(0.7f),
                0, yearStrStart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString spDateEnd = new SpannableString(dateStrEnd);
        spDateEnd.setSpan(new RelativeSizeSpan(0.7f),
                0, yearStrEnd.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // API <= 16
        if (!mIsInLandscapeMode && !SUtils.isApi_17_OrHigher()) {
            spDateEnd.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                    0, dateStrEnd.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvHeaderDateStart.setText(spDateStart);
        tvHeaderDateEnd.setText(spDateEnd);

        // TODO: This should use live regions.
        if (announce) {
            final long millis = mCurrentDate.getStartDate().getTimeInMillis();
            final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            final String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
            AccessibilityUtils.makeAnnouncement(mAnimator, fullDateText);
        }
    }

    private void setCurrentView(int viewIndex) {
        switch (viewIndex) {
            case VIEW_MONTH_DAY:
                mDayPickerView.setDate(mCurrentDate);

                if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
                    switchToSingleDateView();
                } else if (mCurrentDate.getType() == SelectedDate.Type.RANGE) {
                    switchToDateRangeView();
                }

                if (mCurrentView != viewIndex) {
                    if (mAnimator.getDisplayedChild() != VIEW_MONTH_DAY) {
                        mAnimator.setDisplayedChild(VIEW_MONTH_DAY);
                    }
                    mCurrentView = viewIndex;
                }

                AccessibilityUtils.makeAnnouncement(mAnimator, mSelectDay);
                break;
            case VIEW_YEAR:
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(false);
                    mHeaderYear.setActivated(true);
                    mAnimator.setDisplayedChild(VIEW_YEAR);
                    mCurrentView = viewIndex;
                }

                AccessibilityUtils.makeAnnouncement(mAnimator, mSelectYear);
                break;
        }
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param selectedDate  The initial date or date range.
     * @param canPickRange  Enable/disable date range selection
     * @param callback      How user is notified date is changed by
     *                      user, can be null.
     */
    //public void init(int year, int monthOfYear, int dayOfMonth, boolean canPickRange,
    public void init(SelectedDate selectedDate, boolean canPickRange,
                     SublimeDatePicker.OnDateChangedListener callback) {
        //mCurrentDate.set(Calendar.YEAR, year);
        //mCurrentDate.set(Calendar.MONTH, monthOfYear);
        //mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        mCurrentDate = new SelectedDate(selectedDate);

        mDayPickerView.setCanPickRange(canPickRange);
        mDateChangedListener = callback;

        onDateChanged(false, false, true);
    }

    /**
     * Update the current date.
     *
     * @param year       The year.
     * @param month      The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    @SuppressWarnings("unused")
    public void updateDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        onDateChanged(false, true, true);
    }

    // callbackToClient is useless for now & gives us an unnecessary round-trip
    // by calling init(...)
    private void onDateChanged(boolean fromUser, boolean callbackToClient, boolean goToPosition) {
        final int year = mCurrentDate.getStartDate().get(Calendar.YEAR);

        if (callbackToClient && mDateChangedListener != null) {
            mDateChangedListener.onDateChanged(this, mCurrentDate);
        }

        updateHeaderViews();

        mDayPickerView.setDate(new SelectedDate(mCurrentDate), false, goToPosition);

        if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
            mYearPickerView.setYear(year);
        }

        onCurrentDateChanged(fromUser);

        if (fromUser) {
            SUtils.vibrateForDatePicker(SublimeDatePicker.this);
        }
    }

    private void updateHeaderViews() {
        if (Config.DEBUG) {
            Log.i(TAG, "updateHeaderViews(): First Date: "
                    + mCurrentDate.getFirstDate().getTimeInMillis()
                    + " Second Date: "
                    + mCurrentDate.getSecondDate().getTimeInMillis());
        }

        if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
            switchToSingleDateView();
        } else if (mCurrentDate.getType() == SelectedDate.Type.RANGE) {
            switchToDateRangeView();
        }
    }

    private void switchToSingleDateView() {
        mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

        ivHeaderDateReset.setVisibility(View.GONE);
        llHeaderDateRangeCont.setVisibility(View.INVISIBLE);
        llHeaderDateSingleCont.setVisibility(View.VISIBLE);

        mHeaderMonthDay.setActivated(true);
        mHeaderYear.setActivated(false);
    }

    private void switchToDateRangeView() {
        if (mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_NONE) {
            mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_START;
        }

        llHeaderDateSingleCont.setVisibility(View.INVISIBLE);
        ivHeaderDateReset.setVisibility(View.VISIBLE);
        llHeaderDateRangeCont.setVisibility(View.VISIBLE);

        tvHeaderDateStart.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_START);
        tvHeaderDateEnd.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_END);
    }

    public SelectedDate getSelectedDate() {
        return new SelectedDate(mCurrentDate);
    }

    public long getSelectedDateInMillis() {
        return mCurrentDate.getStartDate().getTimeInMillis();
    }

    /**
     * Sets the minimal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.getStartDate().before(mTempDate)) {
            mCurrentDate.getStartDate().setTimeInMillis(minDate);
            onDateChanged(false, true, true);
        }
        mMinDate.setTimeInMillis(minDate);
        mDayPickerView.setMinDate(minDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    /**
     * Gets the minimal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default minimal date is 01/01/1900.
     *
     * @return The minimal supported date.
     */
    @SuppressWarnings("unused")
    public Calendar getMinDate() {
        return mMinDate;
    }

    /**
     * Sets the maximal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.getEndDate().after(mTempDate)) {
            mCurrentDate.getEndDate().setTimeInMillis(maxDate);
            onDateChanged(false, true, true);
        }
        mMaxDate.setTimeInMillis(maxDate);
        mDayPickerView.setMaxDate(maxDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    /**
     * Gets the maximal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default maximal date is 12/31/2100.
     *
     * @return The maximal supported date.
     */
    @SuppressWarnings("unused")
    public Calendar getMaxDate() {
        return mMaxDate;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            if (Config.DEBUG) {
                Log.e(TAG, "Provided `firstDayOfWeek` is invalid - it must be between 1 and 7. " +
                        "Given value: " + firstDayOfWeek + " Picker will use the default value for the given locale.");
            }

            firstDayOfWeek = mCurrentDate.getFirstDate().getFirstDayOfWeek();
        }

        mFirstDayOfWeek = firstDayOfWeek;
        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() == enabled) {
            return;
        }

        mContainer.setEnabled(enabled);
        mDayPickerView.setEnabled(enabled);
        mYearPickerView.setEnabled(enabled);
        mHeaderYear.setEnabled(enabled);
        mHeaderMonthDay.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mContainer.isEnabled();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    private void setCurrentLocale(Locale locale) {
        if (!locale.equals(mCurrentLocale)) {
            mCurrentLocale = locale;
            onLocaleChanged(locale);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        int listPosition = -1;
        int listPositionOffset = -1;

        if (mCurrentView == VIEW_MONTH_DAY) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        } else if (mCurrentView == VIEW_YEAR) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = mYearPickerView.getFirstPositionOffset();
        }

        return new SavedState(superState, mCurrentDate, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), mCurrentView, listPosition,
                listPositionOffset, mCurrentlyActivatedRangeItem);
    }

    @SuppressLint("NewApi")
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        Calendar startDate = Calendar.getInstance(mCurrentLocale);
        Calendar endDate = Calendar.getInstance(mCurrentLocale);

        startDate.set(ss.getSelectedYearStart(), ss.getSelectedMonthStart(), ss.getSelectedDayStart());
        endDate.set(ss.getSelectedYearEnd(), ss.getSelectedMonthEnd(), ss.getSelectedDayEnd());

        mCurrentDate.setFirstDate(startDate);
        mCurrentDate.setSecondDate(endDate);

        int currentView = ss.getCurrentView();
        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());

        mCurrentlyActivatedRangeItem = ss.getCurrentlyActivatedRangeItem();

        onCurrentDateChanged(false);
        setCurrentView(currentView);

        final int listPosition = ss.getListPosition();

        if (listPosition != -1) {
            if (currentView == VIEW_MONTH_DAY) {
                mDayPickerView.setPosition(listPosition);
            } else if (currentView == VIEW_YEAR) {
                final int listPositionOffset = ss.getListPositionOffset();
                mYearPickerView.setSelectionFromTop(listPosition, listPositionOffset);
            }
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
        event.getText().add(mCurrentDate.getStartDate().getTime().toString());
    }

    public CharSequence getAccessibilityClassName() {
        return SublimeDatePicker.class.getName();
    }

    public void setValidationCallback(DatePickerValidationCallback callback) {
        mValidationCallback = callback;
    }

    @SuppressWarnings("unused")
    protected void onValidationChanged(boolean valid) {
        if (mValidationCallback != null) {
            mValidationCallback.onDatePickerValidationChanged(valid);
        }
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     */
    public interface DatePickerValidationCallback {
        void onDatePickerValidationChanged(boolean valid);
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final int mSelectedYearStart;
        private final int mSelectedMonthStart;
        private final int mSelectedDayStart;
        private final int mSelectedYearEnd;
        private final int mSelectedMonthEnd;
        private final int mSelectedDayEnd;
        private final long mMinDate;
        private final long mMaxDate;
        private final int mCurrentView;
        private final int mListPosition;
        private final int mListPositionOffset;
        private final int ssCurrentlyActivatedRangeItem;

        /**
         * Constructor called from {@link SublimeDatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, SelectedDate selectedDate,
                           long minDate, long maxDate, int currentView, int listPosition,
                           int listPositionOffset, int currentlyActivatedRangeItem) {
            super(superState);
            mSelectedYearStart = selectedDate.getStartDate().get(Calendar.YEAR);
            mSelectedMonthStart = selectedDate.getStartDate().get(Calendar.MONTH);
            mSelectedDayStart = selectedDate.getStartDate().get(Calendar.DAY_OF_MONTH);
            mSelectedYearEnd = selectedDate.getEndDate().get(Calendar.YEAR);
            mSelectedMonthEnd = selectedDate.getEndDate().get(Calendar.MONTH);
            mSelectedDayEnd = selectedDate.getEndDate().get(Calendar.DAY_OF_MONTH);
            mMinDate = minDate;
            mMaxDate = maxDate;
            mCurrentView = currentView;
            mListPosition = listPosition;
            mListPositionOffset = listPositionOffset;
            ssCurrentlyActivatedRangeItem = currentlyActivatedRangeItem;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSelectedYearStart = in.readInt();
            mSelectedMonthStart = in.readInt();
            mSelectedDayStart = in.readInt();
            mSelectedYearEnd = in.readInt();
            mSelectedMonthEnd = in.readInt();
            mSelectedDayEnd = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
            mCurrentView = in.readInt();
            mListPosition = in.readInt();
            mListPositionOffset = in.readInt();
            ssCurrentlyActivatedRangeItem = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYearStart);
            dest.writeInt(mSelectedMonthStart);
            dest.writeInt(mSelectedDayStart);
            dest.writeInt(mSelectedYearEnd);
            dest.writeInt(mSelectedMonthEnd);
            dest.writeInt(mSelectedDayEnd);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mCurrentView);
            dest.writeInt(mListPosition);
            dest.writeInt(mListPositionOffset);
            dest.writeInt(ssCurrentlyActivatedRangeItem);
        }

        public int getSelectedDayStart() {
            return mSelectedDayStart;
        }

        public int getSelectedMonthStart() {
            return mSelectedMonthStart;
        }

        public int getSelectedYearStart() {
            return mSelectedYearStart;
        }

        public int getSelectedDayEnd() {
            return mSelectedDayEnd;
        }

        public int getSelectedMonthEnd() {
            return mSelectedMonthEnd;
        }

        public int getSelectedYearEnd() {
            return mSelectedYearEnd;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public int getCurrentView() {
            return mCurrentView;
        }

        public int getListPosition() {
            return mListPosition;
        }

        public int getListPositionOffset() {
            return mListPositionOffset;
        }

        public int getCurrentlyActivatedRangeItem() {
            return ssCurrentlyActivatedRangeItem;
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

    /**
     * The callback used to indicate the user changed the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view         The view associated with this listener.
         * @param selectedDate The date that was set.
         */
        void onDateChanged(SublimeDatePicker view, SelectedDate selectedDate);
    }
}
