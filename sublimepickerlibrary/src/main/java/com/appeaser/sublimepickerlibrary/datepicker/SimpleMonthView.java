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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.common.DateTimePatternHelper;
import com.appeaser.sublimepickerlibrary.utilities.Config;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers
 * within the specified month.
 */
class SimpleMonthView extends View {
    private static final String TAG = SimpleMonthView.class.getSimpleName();

    private static final int DAYS_IN_WEEK = 7;
    private static final int MAX_WEEKS_IN_MONTH = 6;

    private static final int DEFAULT_SELECTED_DAY = -1;
    private static final int DEFAULT_WEEK_START = Calendar.SUNDAY;

    private static final String DEFAULT_TITLE_FORMAT = "MMMMy";
    private static final String DAY_OF_WEEK_FORMAT;

    @SuppressWarnings("FieldCanBeLocal")
    private final int DRAW_RECT = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private final int DRAW_RECT_WITH_CURVE_ON_LEFT = 1;
    @SuppressWarnings("FieldCanBeLocal")
    private final int DRAW_RECT_WITH_CURVE_ON_RIGHT = 2;

    static {
        // Deals with the change in usage of `EEEEE` pattern.
        // See method `SimpleDateFormat#appendDayOfWeek(...)` for more details.
        if (SUtils.isApi_18_OrHigher()) {
            DAY_OF_WEEK_FORMAT = "EEEEE";
        } else {
            DAY_OF_WEEK_FORMAT = "E";
        }
    }

    private final TextPaint mMonthPaint = new TextPaint();
    private final TextPaint mDayOfWeekPaint = new TextPaint();
    private final TextPaint mDayPaint = new TextPaint();
    private final Paint mDaySelectorPaint = new Paint();
    private final Paint mDayHighlightPaint = new Paint();
    private final Paint mDayRangeSelectorPaint = new Paint();

    private final Calendar mCalendar = Calendar.getInstance();
    private final Calendar mDayOfWeekLabelCalendar = Calendar.getInstance();

    private MonthViewTouchHelper mTouchHelper;

    private SimpleDateFormat mTitleFormatter;
    private SimpleDateFormat mDayOfWeekFormatter;
    private NumberFormat mDayFormatter;

    // Desired dimensions.
    private int mDesiredMonthHeight;
    private int mDesiredDayOfWeekHeight;
    private int mDesiredDayHeight;
    private int mDesiredCellWidth;
    private int mDesiredDaySelectorRadius;

    private CharSequence mTitle;

    private int mMonth;
    private int mYear;

    // Dimensions as laid out.
    private int mMonthHeight;
    private int mDayOfWeekHeight;
    private int mDayHeight;
    private int mCellWidth;
    private int mDaySelectorRadius;

    private int mPaddedWidth;
    private int mPaddedHeight;

    /**
     * The day of month for the selected day, or -1 if no day is selected.
     */
    // private int mActivatedDay = -1;

    private final ActivatedDays mActivatedDays = new ActivatedDays();

    /**
     * The day of month for today, or -1 if the today is not in the current
     * month.
     */
    private int mToday = DEFAULT_SELECTED_DAY;

    /**
     * The first day of the week (ex. Calendar.SUNDAY).
     */
    private int mWeekStart = DEFAULT_WEEK_START;

    /**
     * The number of days (ex. 28) in the current month.
     */
    private int mDaysInMonth;

    /**
     * The day of week (ex. Calendar.SUNDAY) for the first day of the current
     * month.
     */
    private int mDayOfWeekStart;

    /**
     * The day of month for the first (inclusive) enabled day.
     */
    private int mEnabledDayStart = 1;

    /**
     * The day of month for the last (inclusive) enabled day.
     */
    private int mEnabledDayEnd = 31;

    /**
     * Optional listener for handling day click actions.
     */
    private OnDayClickListener mOnDayClickListener;

    private ColorStateList mDayTextColor;

    private int mTouchedItem = -1;

    private Context mContext;

    private int mTouchSlopSquared;

    private float mPaddingRangeIndicator;

    public SimpleMonthView(Context context) {
        this(context, null);
    }

    public SimpleMonthView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spMonthViewStyle);
    }

    public SimpleMonthView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SimpleMonthView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    private void init() {
        mContext = getContext();

        mTouchSlopSquared = ViewConfiguration.get(mContext).getScaledTouchSlop()
                * ViewConfiguration.get(mContext).getScaledTouchSlop();

        final Resources res = mContext.getResources();
        mDesiredMonthHeight = res.getDimensionPixelSize(R.dimen.sp_date_picker_month_height);
        mDesiredDayOfWeekHeight = res.getDimensionPixelSize(R.dimen.sp_date_picker_day_of_week_height);
        mDesiredDayHeight = res.getDimensionPixelSize(R.dimen.sp_date_picker_day_height);
        mDesiredCellWidth = res.getDimensionPixelSize(R.dimen.sp_date_picker_day_width);
        mDesiredDaySelectorRadius = res.getDimensionPixelSize(
                R.dimen.sp_date_picker_day_selector_radius);
        mPaddingRangeIndicator = res.getDimensionPixelSize(R.dimen.sp_month_view_range_padding);

        // Set up accessibility components.
        mTouchHelper = new MonthViewTouchHelper(this);

        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        ViewCompat.setImportantForAccessibility(this,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        final Locale locale = res.getConfiguration().locale;

        String titleFormat;

        if (SUtils.isApi_18_OrHigher()) {
            titleFormat = DateFormat.getBestDateTimePattern(locale, DEFAULT_TITLE_FORMAT);
        } else {
            titleFormat = DateTimePatternHelper.getBestDateTimePattern(locale,
                    DateTimePatternHelper.PATTERN_MMMMy);
        }

        mTitleFormatter = new SimpleDateFormat(titleFormat, locale);
        mDayOfWeekFormatter = new SimpleDateFormat(DAY_OF_WEEK_FORMAT, locale);
        mDayFormatter = NumberFormat.getIntegerInstance(locale);

        initPaints(res);
    }

    /**
     * Applies the specified text appearance resource to a paint, returning the
     * text color if one is set in the text appearance.
     *
     * @param p     the paint to modify
     * @param resId the resource ID of the text appearance
     * @return the text color, if available
     */
    private ColorStateList applyTextAppearance(Paint p, int resId) {
        // Workaround for inaccessible R.styleable.TextAppearance_*
        TextView tv = new TextView(mContext);
        if (SUtils.isApi_23_OrHigher()) {
            tv.setTextAppearance(resId);
        } else {
            //noinspection deprecation
            tv.setTextAppearance(mContext, resId);
        }

        p.setTypeface(tv.getTypeface());
        p.setTextSize(tv.getTextSize());

        final ColorStateList textColor = tv.getTextColors();

        if (textColor != null) {
            final int enabledColor = textColor.getColorForState(ENABLED_STATE_SET, 0);
            p.setColor(enabledColor);
        }

        return textColor;
    }

    public int getMonthHeight() {
        return mMonthHeight;
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public void setMonthTextAppearance(int resId) {
        applyTextAppearance(mMonthPaint, resId);

        invalidate();
    }

    public void setDayOfWeekTextAppearance(int resId) {
        applyTextAppearance(mDayOfWeekPaint, resId);
        invalidate();
    }

    public void setDayTextAppearance(int resId) {
        final ColorStateList textColor = applyTextAppearance(mDayPaint, resId);
        if (textColor != null) {
            mDayTextColor = textColor;
        }

        invalidate();
    }

    public CharSequence getTitle() {
        if (mTitle == null) {
            mTitle = mTitleFormatter.format(mCalendar.getTime());
        }
        return mTitle;
    }

    /**
     * Sets up the text and style properties for painting.
     */
    private void initPaints(Resources res) {
        final String monthTypeface = res.getString(R.string.sp_date_picker_month_typeface);
        final String dayOfWeekTypeface = res.getString(R.string.sp_date_picker_day_of_week_typeface);
        final String dayTypeface = res.getString(R.string.sp_date_picker_day_typeface);

        final int monthTextSize = res.getDimensionPixelSize(
                R.dimen.sp_date_picker_month_text_size);
        final int dayOfWeekTextSize = res.getDimensionPixelSize(
                R.dimen.sp_date_picker_day_of_week_text_size);
        final int dayTextSize = res.getDimensionPixelSize(
                R.dimen.sp_date_picker_day_text_size);

        mMonthPaint.setAntiAlias(true);
        mMonthPaint.setTextSize(monthTextSize);
        mMonthPaint.setTypeface(Typeface.create(monthTypeface, 0));
        mMonthPaint.setTextAlign(Paint.Align.CENTER);
        mMonthPaint.setStyle(Paint.Style.FILL);

        mDayOfWeekPaint.setAntiAlias(true);
        mDayOfWeekPaint.setTextSize(dayOfWeekTextSize);
        mDayOfWeekPaint.setTypeface(Typeface.create(dayOfWeekTypeface, 0));
        mDayOfWeekPaint.setTextAlign(Paint.Align.CENTER);
        mDayOfWeekPaint.setStyle(Paint.Style.FILL);

        mDaySelectorPaint.setAntiAlias(true);
        mDaySelectorPaint.setStyle(Paint.Style.FILL);

        mDayHighlightPaint.setAntiAlias(true);
        mDayHighlightPaint.setStyle(Paint.Style.FILL);

        mDayRangeSelectorPaint.setAntiAlias(true);
        mDayRangeSelectorPaint.setStyle(Paint.Style.FILL);

        mDayPaint.setAntiAlias(true);
        mDayPaint.setTextSize(dayTextSize);
        mDayPaint.setTypeface(Typeface.create(dayTypeface, 0));
        mDayPaint.setTextAlign(Paint.Align.CENTER);
        mDayPaint.setStyle(Paint.Style.FILL);
    }

    void setMonthTextColor(ColorStateList monthTextColor) {
        final int enabledColor = monthTextColor.getColorForState(ENABLED_STATE_SET, 0);
        mMonthPaint.setColor(enabledColor);
        invalidate();
    }

    void setDayOfWeekTextColor(ColorStateList dayOfWeekTextColor) {
        final int enabledColor = dayOfWeekTextColor.getColorForState(ENABLED_STATE_SET, 0);
        mDayOfWeekPaint.setColor(enabledColor);
        invalidate();
    }

    void setDayTextColor(ColorStateList dayTextColor) {
        mDayTextColor = dayTextColor;
        invalidate();
    }

    void setDaySelectorColor(ColorStateList dayBackgroundColor) {
        final int activatedColor = dayBackgroundColor.getColorForState(
                SUtils.resolveStateSet(SUtils.STATE_ENABLED | SUtils.STATE_ACTIVATED), 0);
        mDaySelectorPaint.setColor(activatedColor);
        mDayRangeSelectorPaint.setColor(activatedColor);
        // TODO: expose as attr?
        mDayRangeSelectorPaint.setAlpha(150);

        invalidate();
    }

    void setDayHighlightColor(ColorStateList dayHighlightColor) {
        final int pressedColor = dayHighlightColor.getColorForState(
                SUtils.resolveStateSet(SUtils.STATE_ENABLED | SUtils.STATE_PRESSED), 0);
        mDayHighlightPaint.setColor(pressedColor);
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        mOnDayClickListener = listener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // First right-of-refusal goes the touch exploration helper.
        return mTouchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    private CheckForTap mPendingCheckForTap;
    private int mInitialTarget = -1;
    private int mDownX, mDownY;

    private boolean isStillAClick(int x, int y) {
        return (((x - mDownX) * (x - mDownX)) + ((y - mDownY) * (y - mDownY))) <= mTouchSlopSquared;
    }

    private final class CheckForTap implements Runnable {

        @Override
        public void run() {
            mTouchedItem = getDayAtLocation(mDownX, mDownY);
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int x = (int) (event.getX() + 0.5f);
        final int y = (int) (event.getY() + 0.5f);

        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;

                mInitialTarget = getDayAtLocation(mDownX, mDownY);

                if (mInitialTarget < 0) {
                    return false;
                }

                if (mPendingCheckForTap == null) {
                    mPendingCheckForTap = new CheckForTap();
                }
                postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isStillAClick(x, y)) {
                    if (mPendingCheckForTap != null) {
                        removeCallbacks(mPendingCheckForTap);
                    }

                    mInitialTarget = -1;

                    if (mTouchedItem >= 0) {
                        mTouchedItem = -1;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                onDayClicked(mInitialTarget);
                // Fall through.
            case MotionEvent.ACTION_CANCEL:
                if (mPendingCheckForTap != null) {
                    removeCallbacks(mPendingCheckForTap);
                }
                // Reset touched day on stream end.
                mTouchedItem = -1;
                mInitialTarget = -1;
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Config.DEBUG) {
            Log.i(TAG, "onDraw(Canvas)");
        }

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        canvas.translate(paddingLeft, paddingTop);

        drawMonth(canvas);
        drawDaysOfWeek(canvas);
        drawDays(canvas);

        canvas.translate(-paddingLeft, -paddingTop);
    }

    private void drawMonth(Canvas canvas) {
        final float x = mPaddedWidth / 2f;

        // Vertically centered within the month header height.
        final float lineHeight = mMonthPaint.ascent() + mMonthPaint.descent();
        final float y = (mMonthHeight - lineHeight) / 2f;

        canvas.drawText(getTitle().toString(), x, y, mMonthPaint);
    }

    private void drawDaysOfWeek(Canvas canvas) {
        final TextPaint p = mDayOfWeekPaint;
        final int headerHeight = mMonthHeight;
        final int rowHeight = mDayOfWeekHeight;
        final int colWidth = mCellWidth;

        // Text is vertically centered within the day of week height.
        final float halfLineHeight = (p.ascent() + p.descent()) / 2f;
        final int rowCenter = headerHeight + rowHeight / 2;

        for (int col = 0; col < DAYS_IN_WEEK; col++) {
            final int colCenter = colWidth * col + colWidth / 2;
            final int colCenterRtl;
            if (SUtils.isLayoutRtlCompat(this)) {
                colCenterRtl = mPaddedWidth - colCenter;
            } else {
                colCenterRtl = colCenter;
            }

            final int dayOfWeek = (col + mWeekStart) % DAYS_IN_WEEK;
            final String label = getDayOfWeekLabel(dayOfWeek);
            canvas.drawText(label, colCenterRtl, rowCenter - halfLineHeight, p);
        }
    }

    private String getDayOfWeekLabel(int dayOfWeek) {
        mDayOfWeekLabelCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        return mDayOfWeekFormatter.format(mDayOfWeekLabelCalendar.getTime());
    }

    /**
     * Draws the month days.
     */
    @SuppressWarnings("ConstantConditions")
    private void drawDays(Canvas canvas) {
        final TextPaint p = mDayPaint;
        final int headerHeight = mMonthHeight + mDayOfWeekHeight;
        //final int rowHeight = mDayHeight;
        final float rowHeight = mDayHeight;
        //final int colWidth = mCellWidth;
        final float colWidth = mCellWidth;

        // Text is vertically centered within the row height.
        final float halfLineHeight = (p.ascent() + p.descent()) / 2f;
        //int rowCenter = headerHeight + rowHeight / 2;
        float rowCenter = headerHeight + rowHeight / 2f;

        for (int day = 1, col = findDayOffset(); day <= mDaysInMonth; day++) {
            //final int colCenter = colWidth * col + colWidth / 2;
            final float colCenter = colWidth * col + colWidth / 2f;
            //final int colCenterRtl;
            final float colCenterRtl;
            if (SUtils.isLayoutRtlCompat(this)) {
                colCenterRtl = mPaddedWidth - colCenter;
            } else {
                colCenterRtl = colCenter;
            }

            int stateMask = 0;

            final boolean isDayEnabled = isDayEnabled(day);
            if (isDayEnabled) {
                stateMask |= SUtils.STATE_ENABLED;
            }

            final boolean isDayInActivatedRange = mActivatedDays.isValid()
                    && mActivatedDays.isActivated(day);
            final boolean isSelected = mActivatedDays.isSelected(day);

            if (isSelected) {
                stateMask |= SUtils.STATE_ACTIVATED;
                canvas.drawCircle(colCenterRtl, rowCenter, mDaySelectorRadius, mDaySelectorPaint);
            } else if (isDayInActivatedRange) {
                stateMask |= SUtils.STATE_ACTIVATED;

                int bgShape = DRAW_RECT;

                if (mActivatedDays.isSingleDay()) {
                    if (mActivatedDays.isStartOfMonth()) {
                        bgShape = DRAW_RECT_WITH_CURVE_ON_RIGHT;
                    } else {
                        bgShape = DRAW_RECT_WITH_CURVE_ON_LEFT;
                    }
                } else if (mActivatedDays.isStartingDayOfRange(day)) {
                    bgShape = DRAW_RECT_WITH_CURVE_ON_LEFT;
                } else if (mActivatedDays.isEndingDayOfRange(day)) {
                    bgShape = DRAW_RECT_WITH_CURVE_ON_RIGHT;
                }

                // Use height to constrain the protrusion of the arc
                boolean constrainProtrusion = colWidth > (rowHeight - (2 * mPaddingRangeIndicator));
                float horDistFromCenter = constrainProtrusion ?
                        rowHeight / 2f - mPaddingRangeIndicator
                        : colWidth / 2f;

                switch (bgShape) {
                    case DRAW_RECT_WITH_CURVE_ON_LEFT:
                        int leftRectArcLeft = (int)(colCenterRtl - horDistFromCenter) % 2 == 1 ?
                                (int)(colCenterRtl - horDistFromCenter) + 1
                                : (int)(colCenterRtl - horDistFromCenter);

                        int leftRectArcRight = (int)(colCenterRtl + horDistFromCenter) % 2 == 1 ?
                                (int)(colCenterRtl + horDistFromCenter) + 1
                                : (int)(colCenterRtl + horDistFromCenter);

                        RectF leftArcRect = new RectF(leftRectArcLeft,
                                rowCenter - rowHeight / 2f + mPaddingRangeIndicator,
                                leftRectArcRight,
                                rowCenter + rowHeight / 2f - mPaddingRangeIndicator);

                        canvas.drawArc(leftArcRect, 90, 180, true, mDayRangeSelectorPaint);

                        canvas.drawRect(new RectF(leftArcRect.centerX(),
                                        rowCenter - rowHeight / 2f + mPaddingRangeIndicator,
                                        colCenterRtl + colWidth / 2f,
                                        rowCenter + rowHeight / 2f - mPaddingRangeIndicator),
                                mDayRangeSelectorPaint);
                        break;
                    case DRAW_RECT_WITH_CURVE_ON_RIGHT:
                        int rightRectArcLeft = (int)(colCenterRtl - horDistFromCenter) % 2 == 1 ?
                                (int)(colCenterRtl - horDistFromCenter) + 1
                                : (int)(colCenterRtl - horDistFromCenter);

                        int rightRectArcRight = (int)(colCenterRtl + horDistFromCenter) % 2 == 1 ?
                                (int)(colCenterRtl + horDistFromCenter) + 1
                                : (int)(colCenterRtl + horDistFromCenter);

                        RectF rightArcRect = new RectF(rightRectArcLeft,
                                rowCenter - rowHeight / 2f + mPaddingRangeIndicator,
                                rightRectArcRight,
                                rowCenter + rowHeight / 2f - mPaddingRangeIndicator);

                        canvas.drawArc(rightArcRect, 270, 180, true, mDayRangeSelectorPaint);

                        canvas.drawRect(new RectF(colCenterRtl - colWidth / 2f,
                                        rowCenter - rowHeight / 2f + mPaddingRangeIndicator,
                                        rightArcRect.centerX(),
                                        rowCenter + rowHeight / 2f - mPaddingRangeIndicator),
                                mDayRangeSelectorPaint);
                        break;
                    default:
                        canvas.drawRect(new RectF(colCenterRtl - colWidth / 2f,
                                        rowCenter - rowHeight / 2f + mPaddingRangeIndicator,
                                        colCenterRtl + colWidth / 2f,
                                        rowCenter + rowHeight / 2f - mPaddingRangeIndicator),
                                mDayRangeSelectorPaint);
                        break;
                }
            }

            if (mTouchedItem == day) {
                stateMask |= SUtils.STATE_PRESSED;

                if (isDayEnabled) {
                    canvas.drawCircle(colCenterRtl, rowCenter,
                            mDaySelectorRadius, mDayHighlightPaint);
                }
            }

            final boolean isDayToday = mToday == day;
            final int dayTextColor;

            if (isDayToday && !isDayInActivatedRange) {
                dayTextColor = mDaySelectorPaint.getColor();
            } else {
                final int[] stateSet = SUtils.resolveStateSet(stateMask);
                dayTextColor = mDayTextColor.getColorForState(stateSet, 0);
            }
            p.setColor(dayTextColor);

            canvas.drawText(mDayFormatter.format(day), colCenterRtl, rowCenter - halfLineHeight, p);

            col++;

            if (col == DAYS_IN_WEEK) {
                col = 0;
                rowCenter += rowHeight;
            }
        }
    }

    private boolean isDayEnabled(int day) {
        return day >= mEnabledDayStart && day <= mEnabledDayEnd;
    }

    private boolean isValidDayOfMonth(int day) {
        return day >= 1 && day <= mDaysInMonth;
    }

    private static boolean isValidDayOfWeek(int day) {
        return day >= Calendar.SUNDAY && day <= Calendar.SATURDAY;
    }

    private static boolean isValidMonth(int month) {
        return month >= Calendar.JANUARY && month <= Calendar.DECEMBER;
    }

    public void selectAllDays() {
        setSelectedDays(1, SUtils.getDaysInMonth(mMonth, mYear), SelectedDate.Type.RANGE);
    }

    public void setSelectedDays(int selectedDayStart, int selectedDayEnd, SelectedDate.Type selectedDateType) {
        mActivatedDays.startingDay = selectedDayStart;
        mActivatedDays.endingDay = selectedDayEnd;
        mActivatedDays.selectedDateType = selectedDateType;

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
        invalidate();
    }

    /**
     * Sets the first day of the week.
     *
     * @param weekStart which day the week should start on, valid values are
     *                  {@link Calendar#SUNDAY} through {@link Calendar#SATURDAY}
     */
    public void setFirstDayOfWeek(int weekStart) {
        if (isValidDayOfWeek(weekStart)) {
            mWeekStart = weekStart;
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
        invalidate();
    }

    /**
     * Sets all the parameters for displaying this week.
     * <p/>
     * Parameters have a default value and will only update if a new value is
     * included, except for focus month, which will always default to no focus
     * month if no value is passed in. The only required parameter is the week
     * start.
     *
     * @param month            the month
     * @param year             the year
     * @param weekStart        which day the week should start on, valid values are
     *                         {@link Calendar#SUNDAY} through {@link Calendar#SATURDAY}
     * @param enabledDayStart  the first enabled day
     * @param enabledDayEnd    the last enabled day
     * @param selectedDayStart the start of the selected date range, or -1 for no selection
     * @param selectedDayEnd   the end of the selected date range, or -1 for no selection
     * @param selectedDateType RANGE or SINGLE
     */
    void setMonthParams(int month, int year, int weekStart, int enabledDayStart,
                        int enabledDayEnd, int selectedDayStart, int selectedDayEnd,
                        SelectedDate.Type selectedDateType) {
        if (isValidMonth(month)) {
            mMonth = month;
        }
        mYear = year;

        mCalendar.set(Calendar.MONTH, mMonth);
        mCalendar.set(Calendar.YEAR, mYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (isValidDayOfWeek(weekStart)) {
            mWeekStart = weekStart;
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        // Figure out what day today is.
        final Calendar today = Calendar.getInstance();
        mToday = -1;
        mDaysInMonth = SUtils.getDaysInMonth(mMonth, mYear);
        for (int i = 0; i < mDaysInMonth; i++) {
            final int day = i + 1;
            if (sameDay(day, today)) {
                mToday = day;
            }
        }

        mEnabledDayStart = SUtils.constrain(enabledDayStart, 1, mDaysInMonth);
        mEnabledDayEnd = SUtils.constrain(enabledDayEnd, mEnabledDayStart, mDaysInMonth);

        // Invalidate the old title.
        mTitle = null;

        mActivatedDays.startingDay = selectedDayStart;
        mActivatedDays.endingDay = selectedDayEnd;
        mActivatedDays.selectedDateType = selectedDateType;

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
    }

    private boolean sameDay(int day, Calendar today) {
        return mYear == today.get(Calendar.YEAR) && mMonth == today.get(Calendar.MONTH)
                && day == today.get(Calendar.DAY_OF_MONTH);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int preferredHeight = mDesiredDayHeight * MAX_WEEKS_IN_MONTH
                + mDesiredDayOfWeekHeight + mDesiredMonthHeight
                + getPaddingTop() + getPaddingBottom();

        final int preferredWidth = mDesiredCellWidth * DAYS_IN_WEEK
                + (SUtils.isApi_17_OrHigher() ? getPaddingStart() : getPaddingLeft())
                + (SUtils.isApi_17_OrHigher() ? getPaddingEnd() : getPaddingRight());
        final int resolvedWidth = resolveSize(preferredWidth, widthMeasureSpec);
        final int resolvedHeight = resolveSize(preferredHeight, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override
    public void onRtlPropertiesChanged(/*@ResolvedLayoutDir*/ int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!changed) {
            return;
        }

        // Let's initialize a completely reasonable number of variables.
        final int w = right - left;
        final int h = bottom - top;
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();
        final int paddedRight = w - paddingRight;
        final int paddedBottom = h - paddingBottom;
        final int paddedWidth = paddedRight - paddingLeft;
        final int paddedHeight = paddedBottom - paddingTop;
        if (paddedWidth == mPaddedWidth || paddedHeight == mPaddedHeight) {
            return;
        }

        mPaddedWidth = paddedWidth;
        mPaddedHeight = paddedHeight;

        // We may have been laid out smaller than our preferred size. If so,
        // scale all dimensions to fit.
        final int measuredPaddedHeight = getMeasuredHeight() - paddingTop - paddingBottom;
        final float scaleH = paddedHeight / (float) measuredPaddedHeight;
        final int monthHeight = (int) (mDesiredMonthHeight * scaleH);
        final int cellWidth = mPaddedWidth / DAYS_IN_WEEK;
        mMonthHeight = monthHeight;
        mDayOfWeekHeight = (int) (mDesiredDayOfWeekHeight * scaleH);
        mDayHeight = (int) (mDesiredDayHeight * scaleH);
        mCellWidth = cellWidth;

        // Compute the largest day selector radius that's still within the clip
        // bounds and desired selector radius.
        final int maxSelectorWidth = cellWidth / 2 + Math.min(paddingLeft, paddingRight);
        final int maxSelectorHeight = mDayHeight / 2 + paddingBottom;
        mDaySelectorRadius = Math.min(mDesiredDaySelectorRadius,
                Math.min(maxSelectorWidth, maxSelectorHeight));

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
    }

    private int findDayOffset() {
        final int offset = mDayOfWeekStart - mWeekStart;
        if (mDayOfWeekStart < mWeekStart) {
            return offset + DAYS_IN_WEEK;
        }
        return offset;
    }

    /**
     * Calculates the day of the month at the specified touch position. Returns
     * the day of the month or -1 if the position wasn't in a valid day.
     *
     * @param x the x position of the touch event
     * @param y the y position of the touch event
     * @return the day of the month at (x, y), or -1 if the position wasn't in
     * a valid day
     */
    //private int getDayAtLocation(int x, int y) {
    public int getDayAtLocation(int x, int y) {
        final int paddedX = x - getPaddingLeft();
        if (paddedX < 0 || paddedX >= mPaddedWidth) {
            return -1;
        }

        final int headerHeight = mMonthHeight + mDayOfWeekHeight;
        final int paddedY = y - getPaddingTop();
        if (paddedY < headerHeight || paddedY >= mPaddedHeight) {
            return -1;
        }

        // Adjust for RTL after applying padding.
        final int paddedXRtl;
        if (SUtils.isLayoutRtlCompat(this)) {
            paddedXRtl = mPaddedWidth - paddedX;
        } else {
            paddedXRtl = paddedX;
        }

        final int row = (paddedY - headerHeight) / mDayHeight;
        final int col = (paddedXRtl * DAYS_IN_WEEK) / mPaddedWidth;
        final int index = col + row * DAYS_IN_WEEK;
        final int day = index + 1 - findDayOffset();
        if (!isValidDayOfMonth(day)) {
            return -1;
        }

        return day;
    }

    /**
     * Calculates the bounds of the specified day.
     *
     * @param id        the day of the month
     * @param outBounds the rect to populate with bounds
     */
    private boolean getBoundsForDay(int id, Rect outBounds) {
        if (!isValidDayOfMonth(id)) {
            return false;
        }

        final int index = id - 1 + findDayOffset();

        // Compute left edge, taking into account RTL.
        final int col = index % DAYS_IN_WEEK;
        final int colWidth = mCellWidth;
        final int left;
        if (SUtils.isLayoutRtlCompat(this)) {
            left = getWidth() - getPaddingRight() - (col + 1) * colWidth;
        } else {
            left = getPaddingLeft() + col * colWidth;
        }

        // Compute top edge.
        final int row = index / DAYS_IN_WEEK;
        final int rowHeight = mDayHeight;
        final int headerHeight = mMonthHeight + mDayOfWeekHeight;
        final int top = getPaddingTop() + headerHeight + row * rowHeight;

        outBounds.set(left, top, left + colWidth, top + rowHeight);

        return true;
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * {@link OnDayClickListener} if one is set.
     *
     * @param day the day that was clicked
     */
    private boolean onDayClicked(int day) {
        if (!isValidDayOfMonth(day) || !isDayEnabled(day)) {
            return false;
        }

        if (mOnDayClickListener != null) {
            final Calendar date = Calendar.getInstance();
            date.set(mYear, mMonth, day);

            mOnDayClickListener.onDayClick(this, date);
        }

        // This is a no-op if accessibility is turned off.
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED);
        return true;
    }

    private class MonthViewTouchHelper extends ExploreByTouchHelper {

        private static final String DATE_FORMAT = "dd MMMM yyyy";

        private final Rect mTempRect = new Rect();
        private final Calendar mTempCalendar = Calendar.getInstance();

        public MonthViewTouchHelper(View forView) {
            super(forView);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            final int day = getDayAtLocation((int) (x + 0.5f), (int) (y + 0.5f));
            if (day != -1) {
                return day;
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = 1; day <= mDaysInMonth; day++) {
                virtualViewIds.add(day);
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setContentDescription(getDayDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            final boolean hasBounds = getBoundsForDay(virtualViewId, mTempRect);

            if (!hasBounds) {
                // The day is invalid, kill the node.
                mTempRect.setEmpty();
                node.setContentDescription("");
                node.setBoundsInParent(mTempRect);
                node.setVisibleToUser(false);
                return;
            }

            node.setText(getDayText(virtualViewId));
            node.setContentDescription(getDayDescription(virtualViewId));
            node.setBoundsInParent(mTempRect);

            final boolean isDayEnabled = isDayEnabled(virtualViewId);
            if (isDayEnabled) {
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }

            node.setEnabled(isDayEnabled);

            if (mActivatedDays.isValid() && mActivatedDays.isActivated(virtualViewId)) {
                // TODO: This should use activated once that's supported.
                node.setChecked(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    return onDayClicked(virtualViewId);
            }

            return false;
        }

        /**
         * Generates a description for a given virtual view.
         *
         * @param id the day to generate a description for
         * @return a description of the virtual view
         */
        private CharSequence getDayDescription(int id) {
            if (isValidDayOfMonth(id)) {
                mTempCalendar.set(mYear, mMonth, id);
                return DateFormat.format(DATE_FORMAT, mTempCalendar.getTimeInMillis());
            }

            return "";
        }

        /**
         * Generates displayed text for a given virtual view.
         *
         * @param id the day to generate text for
         * @return the visible text of the virtual view
         */
        private CharSequence getDayText(int id) {
            if (isValidDayOfMonth(id)) {
                return mDayFormatter.format(id);
            }

            return null;
        }
    }

    public Calendar composeDate(int day) {
        if (!isValidDayOfMonth(day) || !isDayEnabled(day)) {
            return null;
        }

        final Calendar date = Calendar.getInstance();
        date.set(mYear, mMonth, day);
        return date;
    }

    public class ActivatedDays {
        public int startingDay = -1, endingDay = -1;
        public SelectedDate.Type selectedDateType;

        @SuppressWarnings("unused")
        public void reset() {
            startingDay = endingDay = -1;
        }

        public boolean isValid() {
            return startingDay != -1 && endingDay != -1;
        }

        public boolean isActivated(int day) {
            return day >= startingDay && day <= endingDay;
        }

        public boolean isStartingDayOfRange(int day) {
            return day == startingDay;
        }

        public boolean isEndingDayOfRange(int day) {
            return day == endingDay;
        }

        public boolean isSingleDay() {
            return startingDay == endingDay;
        }

        public boolean isSelected(int day) {
            return selectedDateType == SelectedDate.Type.SINGLE
                    && startingDay == day
                    && endingDay == day;
        }

        // experimental
        @SuppressWarnings("unused")
        public int getSelectedDay() {
            if (selectedDateType == SelectedDate.Type.SINGLE
                    && startingDay == endingDay) {
                return startingDay;
            }

            return -1;
        }

        @SuppressWarnings("unused")
        public boolean hasSelectedDay() {
            return selectedDateType == SelectedDate.Type.SINGLE
                    && startingDay == endingDay && startingDay != -1;
        }

        /**
         * Kind of a hack. Used in conjunction with isSingleDay() to determine
         * the side on which the curved surface will fall.
         * We assume that if this is the starting day of
         * this month, its also the end of selected date range. If this returns false,
         * we consider the selectedDay to be the beginning of selected date range.
         *
         * @return true if startingDay is the first day of the month, false otherwise.
         */
        public boolean isStartOfMonth() {
            return startingDay == 1;
        }
    }

    /**
     * Handles callbacks when the user clicks on a time object.
     */
    public interface OnDayClickListener {
        void onDayClick(SimpleMonthView view, Calendar day);
    }
}
