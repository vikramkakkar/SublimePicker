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

package com.appeaser.sublimepickerlibrary.helpers;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Options to initialize 'SublimePicker'
 */
public class SublimeOptions implements Parcelable {
    public enum Picker {DATE_PICKER, TIME_PICKER, REPEAT_OPTION_PICKER, INVALID}

    // make DatePicker available
    public final static int ACTIVATE_DATE_PICKER = 0x01;

    // make TimePicker available
    public final static int ACTIVATE_TIME_PICKER = 0x02;

    // make RecurrencePicker available
    public final static int ACTIVATE_RECURRENCE_PICKER = 0x04;

    private int mDisplayOptions =
            ACTIVATE_DATE_PICKER | ACTIVATE_TIME_PICKER | ACTIVATE_RECURRENCE_PICKER;

    // Date & Time params
    private int mStartYear = -1, mStartMonth = -1, mStartDayOfMonth = -1,
                mEndYear = -1, mEndMonth = -1, mEndDayOfMonth = -1,
                mHourOfDay = -1, mMinute = -1;
    //private int mYear = -1, mMonthOfYear = -1, mDayOfMonth = -1, mHourOfDay = -1, mMinute = -1;
    private long mMinDate = Long.MIN_VALUE, mMaxDate = Long.MIN_VALUE;
    private boolean mAnimateLayoutChanges, mIs24HourView;

    private SublimeRecurrencePicker.RecurrenceOption mRecurrenceOption
            = SublimeRecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
    private String mRecurrenceRule = "";

    // Allow date range selection
    private boolean mCanPickDateRange;

    // Defaults
    private Picker mPickerToShow = Picker.DATE_PICKER;

    public SublimeOptions() {
        // Nothing
    }

    private SublimeOptions(Parcel in) {
        readFromParcel(in);
    }

    // Use 'LayoutTransition'
    @SuppressWarnings("unused")
    public SublimeOptions setAnimateLayoutChanges(boolean animateLayoutChanges) {
        mAnimateLayoutChanges = animateLayoutChanges;
        return this;
    }

    public boolean animateLayoutChanges() {
        return mAnimateLayoutChanges;
    }

    // Set the Picker that will be shown
    // when 'SublimePicker' is displayed
    public SublimeOptions setPickerToShow(Picker picker) {
        mPickerToShow = picker;
        return this;
    }

    private boolean isPickerActive(Picker picker) {
        switch (picker) {
            case DATE_PICKER:
                return isDatePickerActive();
            case TIME_PICKER:
                return isTimePickerActive();
            case REPEAT_OPTION_PICKER:
                return isRecurrencePickerActive();
        }

        return false;
    }

    public Picker getPickerToShow() {
        return mPickerToShow;
    }

    // Activate pickers
    public SublimeOptions setDisplayOptions(int displayOptions) {
        if (!areValidDisplayOptions(displayOptions)) {
            throw new IllegalArgumentException("Invalid display options.");
        }

        mDisplayOptions = displayOptions;
        return this;
    }

    private boolean areValidDisplayOptions(int displayOptions) {
        int flags = ACTIVATE_DATE_PICKER | ACTIVATE_TIME_PICKER | ACTIVATE_RECURRENCE_PICKER;
        return (displayOptions & ~flags) == 0;
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public SublimeOptions setDateParams(int year, int month, int dayOfMonth) {
        return setDateParams(year, month, dayOfMonth, year, month, dayOfMonth);
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public SublimeOptions setDateParams(int startYear, int startMonth, int startDayOfMonth,
                                        int endYear, int endMonth, int endDayOfMonth) {
        mStartYear = startYear;
        mStartMonth = startMonth;
        mStartDayOfMonth = startDayOfMonth;

        mEndYear = endYear;
        mEndMonth = endMonth;
        mEndDayOfMonth = endDayOfMonth;

        return this;
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public SublimeOptions setDateParams(@NonNull Calendar calendar) {
        return setDateParams(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public SublimeOptions setDateParams(@NonNull Calendar startCal, @NonNull Calendar endCal) {
        return setDateParams(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH),
                endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
                endCal.get(Calendar.DAY_OF_MONTH));
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public SublimeOptions setDateParams(@NonNull SelectedDate selectedDate) {
        return setDateParams(selectedDate.getStartDate().get(Calendar.YEAR),
                selectedDate.getStartDate().get(Calendar.MONTH),
                selectedDate.getStartDate().get(Calendar.DAY_OF_MONTH),
                selectedDate.getEndDate().get(Calendar.YEAR),
                selectedDate.getEndDate().get(Calendar.MONTH),
                selectedDate.getEndDate().get(Calendar.DAY_OF_MONTH));
    }

    // Set date range
    // Pass '-1L' for 'minDate'/'maxDate' for default
    @SuppressWarnings("unused")
    public SublimeOptions setDateRange(long minDate, long maxDate) {
        mMinDate = minDate;
        mMaxDate = maxDate;
        return this;
    }

    // Provide initial time parameters
    @SuppressWarnings("unused")
    public SublimeOptions setTimeParams(int hourOfDay, int minute, boolean is24HourView) {
        mHourOfDay = hourOfDay;
        mMinute = minute;
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial Recurrence-rule
    @SuppressWarnings("unused")
    public SublimeOptions setRecurrenceParams(SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {

        // If passed recurrence option is null, take it as the does_not_repeat option.
        // If passed recurrence option is custom, but the passed recurrence rule is null/empty,
        // take it as the does_not_repeat option.
        // If passed recurrence option is not custom, nullify the recurrence rule.
        if (recurrenceOption == null
                || (recurrenceOption == SublimeRecurrencePicker.RecurrenceOption.CUSTOM
                && TextUtils.isEmpty(recurrenceRule))) {
            recurrenceOption = SublimeRecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
            recurrenceRule = null;
        } else if (recurrenceOption != SublimeRecurrencePicker.RecurrenceOption.CUSTOM) {
            recurrenceRule = null;
        }

        mRecurrenceOption = recurrenceOption;
        mRecurrenceRule = recurrenceRule;
        return this;
    }

    @SuppressWarnings("unused")
    public String getRecurrenceRule() {
        return mRecurrenceRule == null ?
                "" : mRecurrenceRule;
    }

    @SuppressWarnings("unused")
    public SublimeRecurrencePicker.RecurrenceOption getRecurrenceOption() {
        return mRecurrenceOption == null ?
                SublimeRecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT : mRecurrenceOption;
    }

    public boolean isDatePickerActive() {
        return (mDisplayOptions & ACTIVATE_DATE_PICKER) == ACTIVATE_DATE_PICKER;
    }

    public boolean isTimePickerActive() {
        return (mDisplayOptions & ACTIVATE_TIME_PICKER) == ACTIVATE_TIME_PICKER;
    }

    public boolean isRecurrencePickerActive() {
        return (mDisplayOptions & ACTIVATE_RECURRENCE_PICKER) == ACTIVATE_RECURRENCE_PICKER;
    }

    /*public int[] getDateParams() {
        if (mYear == -1 || mMonthOfYear == -1 || mDayOfMonth == -1) {
            Calendar cal = SUtils.getCalendarForLocale(null, Locale.getDefault());
            mYear = cal.get(Calendar.YEAR);
            mMonthOfYear = cal.get(Calendar.MONTH);
            mDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        }

        return new int[]{mYear, mMonthOfYear, mDayOfMonth};
    }*/

    public SelectedDate getDateParams() {
        Calendar startCal = SUtils.getCalendarForLocale(null, Locale.getDefault());
        if (mStartYear == -1 || mStartMonth == -1 || mStartDayOfMonth == -1) {
            mStartYear = startCal.get(Calendar.YEAR);
            mStartMonth = startCal.get(Calendar.MONTH);
            mStartDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);
        } else {
            startCal.set(mStartYear, mStartMonth, mStartDayOfMonth);
        }

        Calendar endCal = SUtils.getCalendarForLocale(null, Locale.getDefault());
        if (mEndYear == -1 || mEndMonth == -1 || mEndDayOfMonth == -1) {
            mEndYear = endCal.get(Calendar.YEAR);
            mEndMonth = endCal.get(Calendar.MONTH);
            mEndDayOfMonth = endCal.get(Calendar.DAY_OF_MONTH);
        } else {
            endCal.set(mEndYear, mEndMonth, mEndDayOfMonth);
        }

        return new SelectedDate(startCal, endCal);
    }

    public long[] getDateRange() {
        return new long[]{mMinDate, mMaxDate};
    }

    public int[] getTimeParams() {
        if (mHourOfDay == -1 || mMinute == -1) {
            Calendar cal = SUtils.getCalendarForLocale(null, Locale.getDefault());
            mHourOfDay = cal.get(Calendar.HOUR_OF_DAY);
            mMinute = cal.get(Calendar.MINUTE);
        }

        return new int[]{mHourOfDay, mMinute};
    }

    public boolean is24HourView() {
        return mIs24HourView;
    }

    // Verifies if the supplied options are valid
    public void verifyValidity() {
        if (mPickerToShow == null || mPickerToShow == Picker.INVALID) {
            throw new InvalidOptionsException("The picker set using setPickerToShow(Picker) " +
                    "cannot be null or Picker.INVALID.");
        }

        if (!isPickerActive(mPickerToShow)) {
            throw new InvalidOptionsException("The picker you have " +
                    "requested to show(" + mPickerToShow.name() + ") is not activated. " +
                    "Use setDisplayOptions(int) " +
                    "to activate it, or use an activated Picker with setPickerToShow(Picker).");
        }

        // TODO: Validation? mMinDate < mMaxDate
    }

    public SublimeOptions setCanPickDateRange(boolean canPickDateRange) {
        mCanPickDateRange = canPickDateRange;
        return this;
    }

    public boolean canPickDateRange() {
        return mCanPickDateRange;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void readFromParcel(Parcel in) {
        mAnimateLayoutChanges = in.readByte() != 0;
        mPickerToShow = Picker.valueOf(in.readString());
        mDisplayOptions = in.readInt();
        mStartYear = in.readInt();
        mStartMonth = in.readInt();
        mStartDayOfMonth = in.readInt();
        mEndYear = in.readInt();
        mEndMonth = in.readInt();
        mEndDayOfMonth = in.readInt();
        mHourOfDay = in.readInt();
        mMinute = in.readInt();
        mIs24HourView = in.readByte() != 0;
        mRecurrenceRule = in.readString();
        mCanPickDateRange = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mAnimateLayoutChanges ? 1 : 0));
        dest.writeString(mPickerToShow.name());
        dest.writeInt(mDisplayOptions);
        dest.writeInt(mStartYear);
        dest.writeInt(mStartMonth);
        dest.writeInt(mStartDayOfMonth);
        dest.writeInt(mEndYear);
        dest.writeInt(mEndMonth);
        dest.writeInt(mEndDayOfMonth);
        dest.writeInt(mHourOfDay);
        dest.writeInt(mMinute);
        dest.writeByte((byte) (mIs24HourView ? 1 : 0));
        dest.writeString(mRecurrenceRule);
        dest.writeByte((byte) (mCanPickDateRange ? 1 : 0));
    }

    public static final Parcelable.Creator<SublimeOptions> CREATOR = new Parcelable.Creator<SublimeOptions>() {
        public SublimeOptions createFromParcel(Parcel in) {
            return new SublimeOptions(in);
        }

        public SublimeOptions[] newArray(int size) {
            return new SublimeOptions[size];
        }
    };

    // Thrown if supplied 'SublimeOptions' are not valid
    public class InvalidOptionsException extends RuntimeException {
        public InvalidOptionsException(String detailMessage) {
            super(detailMessage);
        }
    }
}
