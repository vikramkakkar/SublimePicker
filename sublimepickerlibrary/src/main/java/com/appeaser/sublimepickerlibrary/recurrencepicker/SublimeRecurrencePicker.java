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

package com.appeaser.sublimepickerlibrary.recurrencepicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.ArrayList;
import java.util.TimeZone;

public class SublimeRecurrencePicker extends FrameLayout
        implements View.OnClickListener {

    // Pre-defined recurrence options that are shown in a menu
    // format. Choosing 'CUSTOM' takes the user
    // to 'RecurrenceOptionCreator'.
    public enum RecurrenceOption {
        DOES_NOT_REPEAT("DOES NOT REPEAT"),
        DAILY("DAILY"), WEEKLY("WEEKLY"), MONTHLY("MONTHLY"),
        YEARLY("YEARLY"), CUSTOM("CUSTOM...");

        private final String optionName;

        RecurrenceOption(String name) {
            optionName = name;
        }

        public String toString() {
            return optionName;
        }
    }

    // Used to keep track of currently visible view
    private enum CurrentView {
        RECURRENCE_OPTIONS_MENU, RECURRENCE_CREATOR
    }

    RecurrenceOption mCurrentRecurrenceOption;
    CurrentView mCurrentView = CurrentView.RECURRENCE_OPTIONS_MENU;

    int mSelectedStateTextColor, mUnselectedStateTextColor,
            mPressedStateColor, mSelectedOptionDrawablePadding;

    // Holds pre-defined options {DAILY, WEEKLY etc.} in a menu format
    LinearLayout llRecurrenceOptionsMenu;

    // Callback to communicate with SublimePicker
    OnRepeatOptionSetListener mCallback;

    // This holds the recurrence rule provided by SublimePicker.
    // If the user creates a new recurrence rule (using CUSTOM option),
    // this is updated. `null` is a valid value -> a recurrence rule was
    // not provided by SublimePicker & user did not choose CUSTOM
    // option. If at some point, user creates a CUSTOM rule, and then
    // proceeds to choose a preset {DAILY, WEEKLY, MONTHLY, YEARLY},
    // the CUSTOM rule is kept around and user can switch back to it.
    String mRecurrenceRule;

    // Used to indicate the chosen option
    Drawable mCheckmarkDrawable;

    RecurrenceOptionCreator mRecurrenceOptionCreator;

    // Currently selected date from
    // date-picker to use with RecurrenceOptionCreator
    long mCurrentlyChosenTime;

    // For easy traversal through 7 options/views.
    ArrayList<TextView> mRepeatOptionTextViews;

    public SublimeRecurrencePicker(Context context) {
        this(context, null);
    }

    public SublimeRecurrencePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spRecurrencePickerStyle);
    }

    public SublimeRecurrencePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spRecurrencePickerStyle,
                R.style.SublimeRecurrencePickerStyle), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SublimeRecurrencePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spRecurrencePickerStyle,
                R.style.SublimeRecurrencePickerStyle), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    // Initialize UI
    void initializeLayout() {
        Context context = getContext();

        LayoutInflater.from(context).inflate(R.layout.sublime_recurrence_picker, this);

        llRecurrenceOptionsMenu = (LinearLayout) findViewById(R.id.llRecurrenceOptionsMenu);
        mRecurrenceOptionCreator
                = (RecurrenceOptionCreator) findViewById(R.id.recurrenceOptionCreator);
        TextView tvRecurrenceHeading = (TextView) findViewById(R.id.tvHeading);

        // 'mSelectedOptionDrawablePadding' equals left-padding
        // for option TextViews
        mSelectedOptionDrawablePadding = context.getResources()
                .getDimensionPixelSize(R.dimen.selected_recurrence_option_drawable_padding);

        final TypedArray a
                = context.obtainStyledAttributes(R.styleable.SublimeRecurrencePicker);
        try {
            int headingBgColor = a.getColor(
                    R.styleable.SublimeRecurrencePicker_spHeaderBackground,
                    SUtils.COLOR_ACCENT);
            int pickerBgColor = a.getColor(
                    R.styleable.SublimeRecurrencePicker_spPickerBackground,
                    SUtils.COLOR_BACKGROUND);

            // Sets background color for API versions >= Lollipop
            // Sets background drawable with rounded corners on
            // API versions < Lollipop
            if (pickerBgColor != Color.TRANSPARENT)
                SUtils.setViewBackground(this, pickerBgColor, SUtils.CORNERS_ALL);

            SUtils.setViewBackground(tvRecurrenceHeading, headingBgColor,
                    SUtils.CORNER_TOP_LEFT | SUtils.CORNER_TOP_RIGHT);

            // State colors
            mSelectedStateTextColor = a.getColor(
                    R.styleable.SublimeRecurrencePicker_spSelectedOptionTextColor,
                    SUtils.COLOR_ACCENT);
            mUnselectedStateTextColor = a.getColor(
                    R.styleable.SublimeRecurrencePicker_spUnselectedOptionsTextColor,
                    SUtils.COLOR_TEXT_PRIMARY);
            mPressedStateColor = a.getColor(
                    R.styleable.SublimeRecurrencePicker_spPressedOptionBgColor,
                    SUtils.COLOR_CONTROL_HIGHLIGHT);

            // Defaults to the included checkmark drawable
            mCheckmarkDrawable
                    = a.getDrawable(R.styleable.SublimeRecurrencePicker_spSelectedOptionDrawable);
            if (mCheckmarkDrawable == null) {
                mCheckmarkDrawable = context.getResources()
                        .getDrawable(R.drawable.checkmark_medium_ff);
            }

            // Android Studio recommends this check :-/
            // Apply color filter to match selected option text color
            if (mCheckmarkDrawable != null)
                mCheckmarkDrawable.setColorFilter(mSelectedStateTextColor, PorterDuff.Mode.MULTIPLY);
        } finally {
            a.recycle();
        }

        // Options/Views
        mRepeatOptionTextViews = new ArrayList<>();
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvChosenCustomOption));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvDoesNotRepeat));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvDaily));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvWeekly));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvMonthly));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvYearly));
        mRepeatOptionTextViews.add(
                (TextView) findViewById(R.id.tvCustom));

        // Set bg StateListDrawables
        for (View v : mRepeatOptionTextViews) {
            SUtils.setViewBackground(v,
                    createOptionBg(mPressedStateColor));
        }
    }

    // Called by SublimePicker to initialize state & provide callback
    public void initializeData(OnRepeatOptionSetListener callback,
                               RecurrenceOption initialOption, String recurrenceRule,
                               long currentlyChosenTime) {
        mCallback = callback;
        mRecurrenceRule = recurrenceRule;

        mCurrentlyChosenTime = currentlyChosenTime;
        mCurrentRecurrenceOption = initialOption;

        // Initialize state for RecurrenceOptionCreator
        mRecurrenceOptionCreator.initializeData(mCurrentlyChosenTime, null,
                mRecurrenceRule, mOnRecurrenceSetListener);
    }

    // Controls the visibility of recurrence options menu
    // & recurrence option creator
    public void updateView() {
        if (mCurrentView == CurrentView.RECURRENCE_OPTIONS_MENU) {
            mRecurrenceOptionCreator.setVisibility(View.GONE);
            llRecurrenceOptionsMenu.setVisibility(View.VISIBLE);

            // Current repeat option may have changed
            updateFlowLayout(mCurrentRecurrenceOption);

            // reset `scrollY` to 0
            final ScrollView scrollView
                    = (ScrollView) llRecurrenceOptionsMenu.findViewById(R.id.svRecurrenceOptionsMenu);
            llRecurrenceOptionsMenu.post(new Runnable() {
                @Override
                public void run() {
                    if (scrollView.getScrollY() != 0)
                        scrollView.fullScroll(ScrollView.FOCUS_UP);
                }
            });
        } else if (mCurrentView == CurrentView.RECURRENCE_CREATOR) {
            llRecurrenceOptionsMenu.setVisibility(View.GONE);
            mRecurrenceOptionCreator.setVisibility(View.VISIBLE);
        }
    }

    void updateFlowLayout(RecurrenceOption recurrenceOption) {
        // Currently selected recurrence option
        int viewIdToSelect;

        switch (recurrenceOption) {
            case DOES_NOT_REPEAT:
                viewIdToSelect = R.id.tvDoesNotRepeat;
                break;
            case DAILY:
                viewIdToSelect = R.id.tvDaily;
                break;
            case WEEKLY:
                viewIdToSelect = R.id.tvWeekly;
                break;
            case MONTHLY:
                viewIdToSelect = R.id.tvMonthly;
                break;
            case YEARLY:
                viewIdToSelect = R.id.tvYearly;
                break;
            case CUSTOM:
                viewIdToSelect = R.id.tvChosenCustomOption;
                break;
            default:
                viewIdToSelect = R.id.tvDoesNotRepeat;
        }

        for (TextView tv : mRepeatOptionTextViews) {
            tv.setOnClickListener(this);

            // If we have a non-empty recurrence rule,
            // display it for easy re-selection
            if (tv.getId() == R.id.tvChosenCustomOption) {
                if (!TextUtils.isEmpty(mRecurrenceRule)) {
                    EventRecurrence eventRecurrence = new EventRecurrence();
                    eventRecurrence.parse(mRecurrenceRule);
                    Time startDate = new Time(TimeZone.getDefault().getID());
                    startDate.set(mCurrentlyChosenTime);
                    eventRecurrence.setStartDate(startDate);

                    tv.setVisibility(View.VISIBLE);

                    tv.setText(EventRecurrenceFormatter.getRepeatString(
                            getContext(), getContext().getResources(),
                            eventRecurrence, true));
                } else { // hide this TextView since 'mRecurrenceRule' is not available
                    tv.setVisibility(View.GONE);
                    continue;
                }
            }

            // Selected option
            if (tv.getId() == viewIdToSelect) {
                // Set checkmark drawable & drawable-padding
                tv.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        mCheckmarkDrawable, null);
                tv.setCompoundDrawablePadding(mSelectedOptionDrawablePadding);
                tv.setTextColor(mSelectedStateTextColor);

                continue;
            }

            // Unselected options
            tv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            tv.setTextColor(mUnselectedStateTextColor);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        // Can't use 'switch' here since this is a library module

        if (viewId == R.id.tvChosenCustomOption) {
            // Exit
            // Previously set custom option
            mCurrentRecurrenceOption = RecurrenceOption.CUSTOM;

            if (mCallback != null) {
                mCallback.onRepeatOptionSet(RecurrenceOption.CUSTOM, mRecurrenceRule);
            }

            return;
        } else if (v.getId() == R.id.tvDoesNotRepeat) {
            mCurrentRecurrenceOption = RecurrenceOption.DOES_NOT_REPEAT;
        } else if (v.getId() == R.id.tvDaily) {
            mCurrentRecurrenceOption = RecurrenceOption.DAILY;
        } else if (v.getId() == R.id.tvWeekly) {
            mCurrentRecurrenceOption = RecurrenceOption.WEEKLY;
        } else if (v.getId() == R.id.tvMonthly) {
            mCurrentRecurrenceOption = RecurrenceOption.MONTHLY;
        } else if (v.getId() == R.id.tvYearly) {
            mCurrentRecurrenceOption = RecurrenceOption.YEARLY;
        } else if (v.getId() == R.id.tvCustom) {
            // Show RecurrenceOptionCreator
            mCurrentView = CurrentView.RECURRENCE_CREATOR;
            updateView();
            return;
        } else {
            // Default
            mCurrentRecurrenceOption = RecurrenceOption.DOES_NOT_REPEAT;
        }

        if (mCallback != null) {
            // A preset value has been picked.
            mCallback.onRepeatOptionSet(mCurrentRecurrenceOption, null);
        }
    }

    // Listener for RecurrenceOptionCreator
    RecurrenceOptionCreator.OnRecurrenceSetListener mOnRecurrenceSetListener
            = new RecurrenceOptionCreator.OnRecurrenceSetListener() {
        @Override
        public void onRecurrenceSet(String rrule) {
            // Update options
            mRecurrenceRule = rrule;
            mCurrentRecurrenceOption = RecurrenceOption.CUSTOM;
            mCurrentView = CurrentView.RECURRENCE_OPTIONS_MENU;

            // If user has created a RecurrenceRule, bypass this
            // picker and show the previously shown picker (DatePicker
            // or TimePicker).
            if (mCallback != null) {
                mCallback.onRepeatOptionSet(RecurrenceOption.CUSTOM, rrule);
            }
        }

        @Override
        public void onCancelled() {
            // If cancelled, bring user back to recurrence options menu
            mCurrentView = CurrentView.RECURRENCE_OPTIONS_MENU;
            updateView();
        }
    };

    // Utility for creating API-specific bg drawables
    Drawable createOptionBg(int pressedBgColor) {
        if (SUtils.isApi_21_OrHigher()) {
            return createRippleDrawableForOption(pressedBgColor);
        } else {
            return createStateListDrawableForOption(pressedBgColor);
        }
    }

    private Drawable createStateListDrawableForOption(int pressedBgColor) {
        StateListDrawable sld = new StateListDrawable();

        sld.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(pressedBgColor));
        sld.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));

        return sld;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Drawable createRippleDrawableForOption(int pressedBgColor) {
        return new RippleDrawable(ColorStateList.valueOf(pressedBgColor), null,
                /* mask */new ColorDrawable(Color.BLACK));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mCurrentView,
                mCurrentRecurrenceOption, mRecurrenceRule);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentView = ss.getCurrentView();
        mCurrentRecurrenceOption = ss.getCurrentRepeatOption();
        mRecurrenceRule = ss.getRecurrenceRule();
        updateView();
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final CurrentView sCurrentView;
        private final RecurrenceOption sCurrentRecurrenceOption;
        private final String sRecurrenceRule;

        /**
         * Constructor called from {@link SublimeRecurrencePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, CurrentView currentView,
                           RecurrenceOption currentRecurrenceOption, String recurrenceRule) {
            super(superState);

            sCurrentView = currentView;
            sCurrentRecurrenceOption = currentRecurrenceOption;
            sRecurrenceRule = recurrenceRule;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            sCurrentView = CurrentView.valueOf(in.readString());
            sCurrentRecurrenceOption = RecurrenceOption.valueOf(in.readString());
            sRecurrenceRule = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeString(sCurrentView.name());
            dest.writeString(sCurrentRecurrenceOption.name());
            dest.writeString(sRecurrenceRule);
        }

        public CurrentView getCurrentView() {
            return sCurrentView;
        }

        public RecurrenceOption getCurrentRepeatOption() {
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

    public interface OnRepeatOptionSetListener {
        /**
         * User has either selected one of the pre-defined
         * recurrence options or used RecurrenceOptionCreator
         * to create a RecurrenceRule
         *
         * @param option         chosen repeat option
         * @param recurrenceRule user-created recurrence-rule
         *                       if chosen 'option' is 'RepeatOption.CUSTOM',
         *                       'null' otherwise.
         */
        void onRepeatOptionSet(RecurrenceOption option, String recurrenceRule);

        /**
         * Currently not used.
         */
        void onDone();
    }
}
