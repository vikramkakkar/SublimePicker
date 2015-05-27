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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

public class SublimeDatePicker extends FrameLayout implements
        DatePickerFunctions, View.OnClickListener, DatePickerController {
    // The context
    protected Context mContext;

    // The current locale
    protected Locale mCurrentLocale;

    // Callbacks
    protected OnDateChangedListener mOnDateChangedListener;
    protected ValidationCallback mDateValidationCallback;

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

    private TextView mDayOfWeekView;

    /**
     * Layout that contains the current month, day, and year.
     */
    private LinearLayout mMonthDayYearLayout;

    /**
     * Clickable layout that contains the current day and year.
     */
    private LinearLayout mMonthAndDayLayout;

    private TextView mHeaderMonthTextView;
    private TextView mHeaderDayOfMonthTextView;
    private TextView mHeaderYearTextView;
    private DayPickerView mDayPickerView;
    private YearPickerView mYearPickerView;

    private boolean mIsEnabled = true;

    // Accessibility strings.
    private String mDayPickerDescription;
    private String mSelectDay;
    private String mYearPickerDescription;
    private String mSelectYear;

    private AccessibleDateAnimator mAnimator;

    private OnDateChangedListener mDateChangedListener;

    private int mCurrentView = UNINITIALIZED;

    private Calendar mCurrentDate;
    private Calendar mTempDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;

    private int mFirstDayOfWeek = USE_LOCALE;
    private int mValidVisiblePosition = 0, mValidVisiblePositionOffset = 0;

    private HashSet<OnSublimeDateChangedListener> mListeners
            = new HashSet<OnSublimeDateChangedListener>();

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

    @SuppressWarnings("unused")
    private static ContextThemeWrapper createThemeWrapper(Context context) {
        final TypedArray forParent = context.obtainStyledAttributes(
                new int[]{R.attr.sublimePickerStyle});
        int parentStyle = forParent.getResourceId(0, R.style.SublimePickerStyleLight);
        forParent.recycle();

        TypedArray forDatePicker = context.obtainStyledAttributes(parentStyle,
                new int[]{R.attr.spDatePickerStyle});
        int datePickerStyleId = forDatePicker.getResourceId(0, R.style.SublimeDatePickerStyle);
        forDatePicker.recycle();

        return new ContextThemeWrapper(context, datePickerStyleId);
    }

    private void initializeLayout(AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        mContext = getContext();
        setCurrentLocale(Locale.getDefault());

        mMinDate = SUtils.getCalendarForLocale(mMinDate, mCurrentLocale);
        mMaxDate = SUtils.getCalendarForLocale(mMaxDate, mCurrentLocale);
        mTempDate = SUtils.getCalendarForLocale(mMaxDate, mCurrentLocale);
        mCurrentDate = SUtils.getCalendarForLocale(mCurrentDate, mCurrentLocale);

        mMinDate.set(DEFAULT_START_YEAR, 1, 1);
        mMaxDate.set(DEFAULT_END_YEAR, 12, 31);

        final Resources res = getResources();

        LayoutInflater.from(mContext)
                .inflate(R.layout.date_picker_layout, this, true);

        mDayOfWeekView = (TextView) findViewById(R.id.date_picker_header);

        // Layout that contains the current date and day name header.
        final LinearLayout dateLayout = (LinearLayout) findViewById(
                R.id.day_picker_selector_layout);
        mMonthDayYearLayout = (LinearLayout) findViewById(
                R.id.date_picker_month_day_year_layout);
        mMonthAndDayLayout = (LinearLayout) findViewById(
                R.id.date_picker_month_and_day_layout);
        mMonthAndDayLayout.setOnClickListener(this);
        mHeaderMonthTextView = (TextView) findViewById(R.id.date_picker_month);
        mHeaderDayOfMonthTextView = (TextView) findViewById(R.id.date_picker_day);
        mHeaderYearTextView = (TextView) findViewById(R.id.date_picker_year);
        mHeaderYearTextView.setOnClickListener(this);

        mDayPickerView = new DayPickerView(mContext);
        mDayPickerView.setFirstDayOfWeek(mFirstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
        mDayPickerView.setOnDaySelectedListener(mOnDaySelectedListener);

        mYearPickerView = new YearPickerView(mContext);
        mYearPickerView.init(this);
        mYearPickerView.setRange(mMinDate, mMaxDate);

        final TypedArray typedArray = mContext.obtainStyledAttributes(attrs,
                R.styleable.SublimeDatePicker, defStyleAttr, defStyleRes);
        try {

            // apply style
            final int dayOfWeekTextAppearanceResId = typedArray.getResourceId(
                    R.styleable.SublimeDatePicker_dayOfWeekTextAppearance, -1);
            if (dayOfWeekTextAppearanceResId != -1) {
                mDayOfWeekView.setTextAppearance(mContext, dayOfWeekTextAppearanceResId);
            }

            boolean isInLandscapeMode = res.getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;

            int colorBgDayOfWeekView = typedArray.getColor(R.styleable.SublimeDatePicker_dayOfWeekBackground,
                    Color.parseColor("#10000000"));
            SUtils.setViewBackground(mDayOfWeekView, colorBgDayOfWeekView,
                    isInLandscapeMode ?
                            SUtils.CORNER_TOP_LEFT
                            : SUtils.CORNER_TOP_LEFT | SUtils.CORNER_TOP_RIGHT);

            int colorBgDateLayout = typedArray.getColor(R.styleable.SublimeDatePicker_headerBackground,
                    SUtils.COLOR_ACCENT);
            SUtils.setViewBackground(dateLayout, colorBgDateLayout,
                    isInLandscapeMode ?
                            SUtils.CORNER_TOP_LEFT
                            : SUtils.CORNER_TOP_LEFT | SUtils.CORNER_TOP_RIGHT);

            final int headerSelectedTextColor = typedArray.getColor(
                    R.styleable.SublimeDatePicker_headerSelectedTextColor,
                    SUtils.COLOR_TEXT_PRIMARY_INVERSE);
            final int headerPressedTextColor = typedArray.getColor(
                    R.styleable.SublimeDatePicker_headerPressedTextColor,
                    SUtils.COLOR_TEXT_PRIMARY_INVERSE);

            final int monthTextAppearanceResId = typedArray.getResourceId(
                    R.styleable.SublimeDatePicker_headerMonthTextAppearance, -1);
            if (monthTextAppearanceResId != -1) {
                mHeaderMonthTextView.setTextAppearance(mContext, monthTextAppearanceResId);
            }

            mHeaderMonthTextView.setTextColor(fixTextColorStates(
                    mHeaderMonthTextView.getTextColors().getDefaultColor(),
                    headerSelectedTextColor, headerPressedTextColor));

            final int dayOfMonthTextAppearanceResId = typedArray.getResourceId(
                    R.styleable.SublimeDatePicker_headerDayOfMonthTextAppearance, -1);
            if (dayOfMonthTextAppearanceResId != -1) {
                mHeaderDayOfMonthTextView.setTextAppearance(mContext, dayOfMonthTextAppearanceResId);
            }

            mHeaderDayOfMonthTextView.setTextColor(fixTextColorStates(
                    mHeaderDayOfMonthTextView.getTextColors().getDefaultColor(),
                    headerSelectedTextColor, headerPressedTextColor));

            final int yearTextAppearanceResId = typedArray.getResourceId(
                    R.styleable.SublimeDatePicker_headerYearTextAppearance, -1);
            if (yearTextAppearanceResId != -1) {
                mHeaderYearTextView.setTextAppearance(mContext, yearTextAppearanceResId);
            }

            mHeaderYearTextView.setTextColor(
                    fixTextColorStates(mHeaderYearTextView.getTextColors().getDefaultColor(),
                            headerSelectedTextColor, headerPressedTextColor));

            final int firstDayOfWeek = typedArray.getInt(R.styleable.SublimeDatePicker_firstDayOfWeek, 0);

            if (firstDayOfWeek != 0) {
                setFirstDayOfWeek(firstDayOfWeek);
            }
        } finally {
            typedArray.recycle();
        }

        mDayPickerDescription = res.getString(R.string.day_picker_description);
        mSelectDay = res.getString(R.string.select_day);
        mYearPickerDescription = res.getString(R.string.year_picker_description);
        mSelectYear = res.getString(R.string.select_year);

        mAnimator = (AccessibleDateAnimator) findViewById(R.id.animator);
        mAnimator.addView(mDayPickerView);
        mAnimator.addView(mYearPickerView);
        mAnimator.setDateMillis(mCurrentDate.getTimeInMillis());

        final Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(ANIMATION_DURATION);
        mAnimator.setInAnimation(animation);

        final Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(ANIMATION_DURATION);
        mAnimator.setOutAnimation(animation2);

        updateDisplay(false);
        setCurrentView(MONTH_AND_DAY_VIEW);
    }

    ColorStateList fixTextColorStates(int defaultStateTextColor,
                                      int selectedStateTextColor, int pressedStateTextColor) {
        int[][] states = new int[][]{new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_selected},
                new int[]{}};

        int[] colors = new int[]{pressedStateTextColor,
                selectedStateTextColor, defaultStateTextColor};

        return new ColorStateList(states, colors);
    }

    /**
     * Compute the array representing the order of Month / Day / Year views in their layout.
     * Will be used for I18N purpose as the order of them depends on the Locale.
     */
    private int[] getMonthDayYearIndexes(String pattern) {
        int[] result = new int[3];

        final String filteredPattern = pattern.replaceAll("'.*?'", "");

        final int dayIndex = filteredPattern.indexOf('d');
        final int monthMIndex = filteredPattern.indexOf("M");
        final int monthIndex = (monthMIndex != -1) ? monthMIndex : filteredPattern.indexOf("L");
        final int yearIndex = filteredPattern.indexOf("y");

        if (yearIndex < monthIndex) {
            result[YEAR_INDEX] = 0;

            if (monthIndex < dayIndex) {
                result[MONTH_INDEX] = 1;
                result[DAY_INDEX] = 2;
            } else {
                result[MONTH_INDEX] = 2;
                result[DAY_INDEX] = 1;
            }
        } else {
            result[YEAR_INDEX] = 2;

            if (monthIndex < dayIndex) {
                result[MONTH_INDEX] = 0;
                result[DAY_INDEX] = 1;
            } else {
                result[MONTH_INDEX] = 1;
                result[DAY_INDEX] = 0;
            }
        }
        return result;
    }

    private void updateDisplay(boolean announce) {
        if (mDayOfWeekView != null) {
            mDayOfWeekView.setText(mCurrentDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                    Locale.getDefault()));
        }

        // Compute indices of Month, Day and Year views
        int[] viewIndices = DEFAULT_VIEW_INDICES;

        if (SUtils.isApi_18_OrHigher()) {
            final String bestDateTimePattern =
                    android.text.format.DateFormat.getBestDateTimePattern(mCurrentLocale, "yMMMd");
            viewIndices = getMonthDayYearIndexes(bestDateTimePattern);
        } else {
            // Fallback
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, mCurrentLocale);

            if (df instanceof SimpleDateFormat) {
                String dateTimePattern = ((SimpleDateFormat) df).toPattern();
                viewIndices = getMonthDayYearIndexes(dateTimePattern);
            }
        }

        // Position the Year and MonthAndDay views within the header.
        mMonthDayYearLayout.removeAllViews();
        if (viewIndices[YEAR_INDEX] == 0) {
            mMonthDayYearLayout.addView(mHeaderYearTextView);
            mMonthDayYearLayout.addView(mMonthAndDayLayout);
        } else {
            mMonthDayYearLayout.addView(mMonthAndDayLayout);
            mMonthDayYearLayout.addView(mHeaderYearTextView);
        }

        // Position Day and Month views within the MonthAndDay view.
        mMonthAndDayLayout.removeAllViews();
        if (viewIndices[MONTH_INDEX] > viewIndices[DAY_INDEX]) {
            mMonthAndDayLayout.addView(mHeaderDayOfMonthTextView);
            mMonthAndDayLayout.addView(mHeaderMonthTextView);
        } else {
            mMonthAndDayLayout.addView(mHeaderMonthTextView);
            mMonthAndDayLayout.addView(mHeaderDayOfMonthTextView);
        }

        mHeaderMonthTextView.setText(mCurrentDate.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                Locale.getDefault()).toUpperCase(Locale.getDefault()));
        mHeaderDayOfMonthTextView.setText(mDayFormat.format(mCurrentDate.getTime()));
        mHeaderYearTextView.setText(mYearFormat.format(mCurrentDate.getTime()));

        // Accessibility.
        long millis = mCurrentDate.getTimeInMillis();
        mAnimator.setDateMillis(millis);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        String monthAndDayText = DateUtils.formatDateTime(mContext, millis, flags);
        mMonthAndDayLayout.setContentDescription(monthAndDayText);

        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
            mAnimator.announceForAccessibility(fullDateText);
        }
    }

    private void setCurrentView(final int viewIndex) {
        long millis = mCurrentDate.getTimeInMillis();

        switch (viewIndex) {
            case MONTH_AND_DAY_VIEW:
                mDayPickerView.setDate(getSelectedDay().getTimeInMillis());
                if (mCurrentView != viewIndex) {
                    mMonthAndDayLayout.setSelected(true);
                    mHeaderYearTextView.setSelected(false);
                    mAnimator.setDisplayedChild(MONTH_AND_DAY_VIEW);
                    mCurrentView = viewIndex;
                }

                final int flags = DateUtils.FORMAT_SHOW_DATE;
                final String dayString = DateUtils.formatDateTime(mContext, millis, flags);
                mAnimator.setContentDescription(mDayPickerDescription + ": " + dayString);
                mAnimator.announceForAccessibility(mSelectDay);
                break;
            case YEAR_VIEW:
                mYearPickerView.onDateChanged();
                if (mCurrentView != viewIndex) {
                    mMonthAndDayLayout.setSelected(false);
                    mHeaderYearTextView.setSelected(true);
                    mAnimator.setDisplayedChild(YEAR_VIEW);
                    mCurrentView = viewIndex;
                }

                final CharSequence yearString = mYearFormat.format(millis);
                mAnimator.setContentDescription(mYearPickerDescription + ": " + yearString);
                mAnimator.announceForAccessibility(mSelectYear);
                break;
        }
    }

    @Override
    public void init(int year, int monthOfYear, int dayOfMonth,
                     DatePickerFunctions.OnDateChangedListener callBack) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, monthOfYear);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        mDateChangedListener = callBack;

        //onDateChanged(false, false);
    }

    // Update scroll position - this is needed when the
    // datepicker is initialized in 'View.GONE' visibility state
    // TODO: needs some work. Depending on 'mCurrentView', the other picker does not hold its position
    public void syncState() {
        if (mValidVisiblePosition > 0) {
            if (mCurrentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView.postSetSelection(mValidVisiblePosition);
            } else if (mCurrentView == YEAR_VIEW) {
                mYearPickerView.postSetSelectionFromTop(mValidVisiblePosition,
                        mValidVisiblePositionOffset);
            }
        } else {
            onDateChanged(false, false);
        }
    }

    @Override
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

        updateDisplay(fromUser);

        if (fromUser) {
            tryVibrate();
        }
    }

    @Override
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    @Override
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    @Override
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    @Override
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
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    @Override
    public long getMinDate() {
        return mMinDate.getTimeInMillis();
    }

    @Override
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
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    @Override
    public long getMaxDate() {
        return mMaxDate.getTimeInMillis();
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }

        mFirstDayOfWeek = firstDayOfWeek;

        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    @Override
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
        mMonthAndDayLayout.setEnabled(enabled);
        mHeaderYearTextView.setEnabled(enabled);
        mAnimator.setEnabled(enabled);
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

        int listPosition = -1;
        int listPositionOffset = -1;

        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        } else if (mCurrentView == YEAR_VIEW) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = mYearPickerView.getFirstPositionOffset();
        }

        if (listPosition > 0) {
            mValidVisiblePosition = listPosition;
        }

        if (listPositionOffset > 0) {
            mValidVisiblePositionOffset = listPositionOffset;
        }

        return new SavedState(superState, year, month, day, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), mCurrentView, mValidVisiblePosition, listPositionOffset);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
        int currentView = ss.getCurrentView();
        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());

        updateDisplay(false);
        setCurrentView(currentView);

        mValidVisiblePosition = ss.getListPosition();
        mValidVisiblePositionOffset = ss.getListPositionOffset();

        final int listPosition = ss.getListPosition();
        if (listPosition != -1) {
            if (mCurrentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView.postSetSelection(listPosition);
            } else if (mCurrentView == YEAR_VIEW) {
                mYearPickerView.postSetSelectionFromTop(listPosition, ss.getListPositionOffset());
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

    @Override
    public void onYearSelected(int year) {
        adjustDayInMonthIfNeeded(mCurrentDate.get(Calendar.MONTH), year);
        mCurrentDate.set(Calendar.YEAR, year);
        onDateChanged(true, true);

        // Auto-advance to month and day view.
        setCurrentView(MONTH_AND_DAY_VIEW);
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = getDaysInMonth(month, year);
        if (day > daysInMonth) {
            mCurrentDate.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
    }

    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                return (year % 4 == 0) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    @Override
    public void registerOnDateChangedListener(OnSublimeDateChangedListener listener) {
        mListeners.add(listener);
    }

    @Override
    public Calendar getSelectedDay() {
        return mCurrentDate;
    }

    @Override
    public void tryVibrate() {
        // Using a different haptic feedback constant
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY
                                     /*(5) - HapticFeedbackConstants.CALENDAR_DATE*/);
    }

    @Override
    public void onClick(View v) {
        tryVibrate();
        if (v.getId() == R.id.date_picker_year) {
            setCurrentView(YEAR_VIEW);
        } else if (v.getId() == R.id.date_picker_month_and_day_layout) {
            setCurrentView(MONTH_AND_DAY_VIEW);
        }
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
        private final int mCurrentView;
        private final int mListPosition;
        private final int mListPositionOffset;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day,
                           long minDate, long maxDate, int currentView, int listPosition,
                           int listPositionOffset) {
            super(superState);
            mSelectedYear = year;
            mSelectedMonth = month;
            mSelectedDay = day;
            mMinDate = minDate;
            mMaxDate = maxDate;
            mCurrentView = currentView;
            mListPosition = listPosition;
            mListPositionOffset = listPositionOffset;
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
            mCurrentView = in.readInt();
            mListPosition = in.readInt();
            mListPositionOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYear);
            dest.writeInt(mSelectedMonth);
            dest.writeInt(mSelectedDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mCurrentView);
            dest.writeInt(mListPosition);
            dest.writeInt(mListPositionOffset);
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

        public int getCurrentView() {
            return mCurrentView;
        }

        public int getListPosition() {
            return mListPosition;
        }

        public int getListPositionOffset() {
            return mListPositionOffset;
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

    @Override
    public void setValidationCallback(ValidationCallback callback) {
        mDateValidationCallback = callback;
    }

    protected void onValidationChanged(boolean valid) {
        if (mDateValidationCallback != null) {
            mDateValidationCallback.onDatePickerValidationChanged(valid);
        }
    }
}
