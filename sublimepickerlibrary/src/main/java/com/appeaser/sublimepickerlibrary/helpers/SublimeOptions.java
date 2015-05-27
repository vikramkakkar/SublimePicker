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

import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Options to initialize 'SublimePicker'
 */
public class SublimeOptions implements Parcelable {
    public enum Picker {DATE_PICKER, TIME_PICKER, REPEAT_OPTION_PICKER, INVALID}

    // make DatePicker available
    public static int ACTIVATE_DATE_PICKER = 0x01;

    // make TimePicker available
    public static int ACTIVATE_TIME_PICKER = 0x02;

    // make RecurrencePicker available
    public static int ACTIVATE_RECURRENCE_PICKER = 0x04;

    private int mDisplayOptions =
            ACTIVATE_DATE_PICKER | ACTIVATE_TIME_PICKER | ACTIVATE_RECURRENCE_PICKER;

    // Date & Time params
    private int mYear = -1, mMonthOfYear = -1, mDayofMonth = -1, mHourOfDay = -1, mMinute = -1;
    private long mMinDate = Long.MIN_VALUE, mMaxDate = Long.MIN_VALUE;
    private boolean mAnimateLayoutChanges, mIs24HourView;
    private String mRecurrenceRule = "";

    // Defaults
    private Picker mPickerToShow = Picker.DATE_PICKER;

    public SublimeOptions() {
        // Nothing
    }

    public SublimeOptions(Parcel in) {
        readFromParcel(in);
    }

    // Use 'LayoutTransition'
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
    public SublimeOptions setDateParams(int year, int monthOfYear, int dayOfMonth) {
        mYear = year;
        mMonthOfYear = monthOfYear;
        mDayofMonth = dayOfMonth;
        return this;
    }

    // Set date range
    // Pass '-1L' for 'minDate'/'maxDate' for default
    public SublimeOptions setDateRange(long minDate, long maxDate) {
        mMinDate = minDate;
        mMaxDate = maxDate;
        return this;
    }

    // Provide initial time parameters
    public SublimeOptions setTimeParams(int hourOfDay, int minute, boolean is24HourView) {
        mHourOfDay = hourOfDay;
        mMinute = minute;
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial Recurrence-rule
    public SublimeOptions setRecurrenceParams(String recurrenceRule) {
        mRecurrenceRule = recurrenceRule;
        return this;
    }

    public String getRecurrenceRule() {
        return mRecurrenceRule == null ?
                "" : mRecurrenceRule;
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

    public int[] getDateParams() {
        if (mYear == -1 || mMonthOfYear == -1 || mDayofMonth == -1) {
            Calendar cal = SUtils.getCalendarForLocale(null, Locale.getDefault());
            mYear = cal.get(Calendar.YEAR);
            mMonthOfYear = cal.get(Calendar.MONTH);
            mDayofMonth = cal.get(Calendar.DAY_OF_MONTH);
        }

        return new int[]{mYear, mMonthOfYear, mDayofMonth};
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

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        mAnimateLayoutChanges = in.readByte() != 0;
        mPickerToShow = Picker.valueOf(in.readString());
        mDisplayOptions = in.readInt();
        mYear = in.readInt();
        mMonthOfYear = in.readInt();
        mDayofMonth = in.readInt();
        mHourOfDay = in.readInt();
        mMinute = in.readInt();
        mIs24HourView = in.readByte() != 0;
        mRecurrenceRule = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mAnimateLayoutChanges ? 1 : 0));
        dest.writeString(mPickerToShow.name());
        dest.writeInt(mDisplayOptions);
        dest.writeInt(mYear);
        dest.writeInt(mMonthOfYear);
        dest.writeInt(mDayofMonth);
        dest.writeInt(mHourOfDay);
        dest.writeInt(mMinute);
        dest.writeByte((byte) (mIs24HourView ? 1 : 0));
        dest.writeString(mRecurrenceRule);
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
