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

package com.appeaser.sublimepicker;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;

public class Sampler extends AppCompatActivity {
    // Launches SublimePicker
    ImageView ivLaunchPicker;

    // SublimePicker options
    CheckBox cbDatePicker, cbTimePicker, cbRecurrencePicker,
            cbShowExtendedBg, cbShowSingleMonthPerPosition;
    RadioButton rbDatePicker, rbTimePicker, rbRecurrencePicker;

    // Labels
    TextView tvPickerToShow, tvActivatedPickers;

    ScrollView svMainContainer;

    // Views to display the chosen Date, Time & Recurrence options
    TextView tvYear, tvMonth, tvDay, tvHour,
            tvMinute, tvRecurrenceOption, tvRecurrenceRule;
    RelativeLayout rlDateTimeRecurrenceInfo;

    // Chosen values
    int mYear, mMonth, mDay, mHour, mMinute;
    String mRecurrenceOption, mRecurrenceRule;

    SublimePickerFragment.Callback mFragmentCallback = new SublimePickerFragment.Callback() {
        @Override
        public void onCancelled() {
            rlDateTimeRecurrenceInfo.setVisibility(View.GONE);
        }

        @Override
        public void onDateTimeRecurrenceSet(int year, int monthOfYear, int dayOfMonth,
                                            int hourOfDay, int minute,
                                            SublimeRecurrencePicker.RecurrenceOption recurrenceOption,
                                            String recurrenceRule) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            mHour = hourOfDay;
            mMinute = minute;
            mRecurrenceOption = recurrenceOption != null ?
                    recurrenceOption.name() : "n/a";
            mRecurrenceRule = recurrenceRule != null ?
                    recurrenceRule : "n/a";

            updateInfoView();

            svMainContainer.post(new Runnable() {
                @Override
                public void run() {
                    svMainContainer.scrollTo(svMainContainer.getScrollX(),
                            cbShowSingleMonthPerPosition.getBottom());
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sampler);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Finish on navigation icon click
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivLaunchPicker = (ImageView) findViewById(R.id.ivLaunchPicker);
        cbDatePicker = (CheckBox) findViewById(R.id.cbDatePicker);
        cbTimePicker = (CheckBox) findViewById(R.id.cbTimePicker);
        cbRecurrencePicker = (CheckBox) findViewById(R.id.cbRecurrencePicker);
        cbShowExtendedBg = (CheckBox) findViewById(R.id.cbShowExtendedBg);
        cbShowSingleMonthPerPosition
                = (CheckBox) findViewById(R.id.cbShowSingleMonthPerPosition);
        rbDatePicker = (RadioButton) findViewById(R.id.rbDatePicker);
        rbTimePicker = (RadioButton) findViewById(R.id.rbTimePicker);
        rbRecurrencePicker = (RadioButton) findViewById(R.id.rbRecurrencePicker);
        tvPickerToShow = (TextView) findViewById(R.id.tvPickerToShow);
        tvActivatedPickers = (TextView) findViewById(R.id.tvActivatedPickers);
        svMainContainer = (ScrollView) findViewById(R.id.svMainContainer);

        // Initialize views to display the chosen Date, Time & Recurrence options
        tvYear = ((TextView) findViewById(R.id.tvYear));
        tvMonth = ((TextView) findViewById(R.id.tvMonth));
        tvDay = ((TextView) findViewById(R.id.tvDay));

        tvHour = ((TextView) findViewById(R.id.tvHour));
        tvMinute = ((TextView) findViewById(R.id.tvMinute));

        tvRecurrenceOption = ((TextView) findViewById(R.id.tvRecurrenceOption));
        tvRecurrenceRule = ((TextView) findViewById(R.id.tvRecurrenceRule));

        rlDateTimeRecurrenceInfo
                = (RelativeLayout) findViewById(R.id.rlDateTimeRecurrenceInfo);

        ivLaunchPicker.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        ivLaunchPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DialogFragment to host SublimePicker
                SublimePickerFragment pickerFrag = new SublimePickerFragment();
                pickerFrag.setCallback(mFragmentCallback);

                // Options
                Pair<Boolean, SublimeOptions> optionsPair = getOptions();

                if (!optionsPair.first) { // If options are not valid
                    Toast.makeText(Sampler.this, "No pickers activated",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Valid options
                Bundle bundle = new Bundle();
                bundle.putParcelable("SUBLIME_OPTIONS", optionsPair.second);
                pickerFrag.setArguments(bundle);

                pickerFrag.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                pickerFrag.show(getSupportFragmentManager(), "SUBLIME_PICKER");
            }
        });

        // De/activates Date Picker
        cbDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbDatePicker.setVisibility(cbDatePicker.isChecked() ?
                        View.VISIBLE : View.GONE);
                onActivatedPickersChanged();
            }
        });

        // De/activates Time Picker
        cbTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbTimePicker.setVisibility(cbTimePicker.isChecked() ?
                        View.VISIBLE : View.GONE);
                onActivatedPickersChanged();
            }
        });

        // De/activates Recurrence Picker
        cbRecurrencePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbRecurrencePicker.setVisibility(cbRecurrencePicker.isChecked() ?
                        View.VISIBLE : View.GONE);
                onActivatedPickersChanged();
            }
        });

        // Extends header bg to full-height in landscape orientation
        cbShowExtendedBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateThemeOptions();
            }
        });

        // Shows a single month per position of
        // DayPickerView - even if that month's days do
        // not completely fill the available space (eg: February)
        cbShowSingleMonthPerPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateThemeOptions();
            }
        });

        // restore state
        dealWithSavedInstanceState(savedInstanceState);
    }

    void dealWithSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) { // Default
            cbDatePicker.setChecked(true);
            cbTimePicker.setChecked(true);
            cbRecurrencePicker.setChecked(true);
            cbShowExtendedBg.setChecked(false);
            cbShowSingleMonthPerPosition.setChecked(false);

            rbDatePicker.setChecked(true);
        } else { // Restore
            cbDatePicker.setChecked(savedInstanceState.getBoolean(SS_DATE_PICKER_CHECKED));
            cbTimePicker.setChecked(savedInstanceState.getBoolean(SS_TIME_PICKER_CHECKED));
            cbRecurrencePicker
                    .setChecked(savedInstanceState.getBoolean(SS_RECURRENCE_PICKER_CHECKED));
            cbShowExtendedBg
                    .setChecked(savedInstanceState.getBoolean(SS_SHOW_EXTENDED_BG_CHECKED));
            cbShowSingleMonthPerPosition
                    .setChecked(savedInstanceState.getBoolean(SS_SHOW_SINGLE_MONTH_CHECKED));

            rbDatePicker.setVisibility(cbDatePicker.isChecked() ?
                    View.VISIBLE : View.GONE);
            rbTimePicker.setVisibility(cbTimePicker.isChecked() ?
                    View.VISIBLE : View.GONE);
            rbRecurrencePicker.setVisibility(cbRecurrencePicker.isChecked() ?
                    View.VISIBLE : View.GONE);

            updateThemeOptions();
            onActivatedPickersChanged();

            if (savedInstanceState.getBoolean(SS_INFO_VIEW_VISIBILITY)) {
                mYear = savedInstanceState.getInt(SS_YEAR);
                mMonth = savedInstanceState.getInt(SS_MONTH);
                mDay = savedInstanceState.getInt(SS_DAY);
                mHour = savedInstanceState.getInt(SS_HOUR);
                mMinute = savedInstanceState.getInt(SS_MINUTE);
                mRecurrenceOption = savedInstanceState.getString(SS_RECURRENCE_OPTION);
                mRecurrenceRule = savedInstanceState.getString(SS_RECURRENCE_RULE);

                updateInfoView();
            }

            final int scrollY = savedInstanceState.getInt(SS_SCROLL_Y);

            if (scrollY != 0) {
                svMainContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        svMainContainer.scrollTo(svMainContainer.getScrollX(),
                                scrollY);
                    }
                });
            }

            // Set callback
            SublimePickerFragment restoredFragment = (SublimePickerFragment)
                    getSupportFragmentManager().findFragmentByTag("SUBLIME_PICKER");
            if (restoredFragment != null) {
                restoredFragment.setCallback(mFragmentCallback);
            }
        }
    }

    // Most of the styling has been kept in styles.xml and
    // made accessible though attributes defined in attrs.xml.
    // For this reason, the sample app has to rely on
    // 'getTheme().applyStyle(....)' when 'cbShowExtendedBg'
    // & 'cbShowSingleMonthPerPosition' are checked/unchecked.
    // In practical usage, this will not be required since the options
    // that are set inside this method will already be defined
    // in xml. You can skip reading into this method and
    // the styles it makes use of.
    private void updateThemeOptions() {
        int styleToApply;

        if (cbShowExtendedBg.isChecked()
                && cbShowSingleMonthPerPosition.isChecked()) {
            styleToApply = R.style.ShowExtendedBgAndSingleMonthPerPosition;
        } else if (cbShowExtendedBg.isChecked()) {
            styleToApply = R.style.ShowExtendedBg;
        } else if (cbShowSingleMonthPerPosition.isChecked()) {
            styleToApply = R.style.ShowSingleMonthPerPosition;
        } else {
            styleToApply = R.style.SublimePickerDefault;
        }

        getTheme().applyStyle(styleToApply, true);
    }

    // Validates & returns SublimePicker options
    Pair<Boolean, SublimeOptions> getOptions() {
        SublimeOptions options = new SublimeOptions();
        int displayOptions = 0;

        if (cbDatePicker.isChecked()) {
            displayOptions |= SublimeOptions.ACTIVATE_DATE_PICKER;
        }

        if (cbTimePicker.isChecked()) {
            displayOptions |= SublimeOptions.ACTIVATE_TIME_PICKER;
        }

        if (cbRecurrencePicker.isChecked()) {
            displayOptions |= SublimeOptions.ACTIVATE_RECURRENCE_PICKER;
        }

        if (rbDatePicker.getVisibility() == View.VISIBLE && rbDatePicker.isChecked()) {
            options.setPickerToShow(SublimeOptions.Picker.DATE_PICKER);
        } else if (rbTimePicker.getVisibility() == View.VISIBLE && rbTimePicker.isChecked()) {
            options.setPickerToShow(SublimeOptions.Picker.TIME_PICKER);
        } else if (rbRecurrencePicker.getVisibility() == View.VISIBLE && rbRecurrencePicker.isChecked()) {
            options.setPickerToShow(SublimeOptions.Picker.REPEAT_OPTION_PICKER);
        }

        options.setDisplayOptions(displayOptions);

        // If 'displayOptions' is zero, the chosen options are not valid
        return new Pair<>(displayOptions != 0 ? Boolean.TRUE : Boolean.FALSE, options);
    }

    // Re-evaluates the state based on checked/unchecked
    // options - toggles visibility of RadioButtons based
    // on activated/deactivated pickers - updates TextView
    // labels accordingly.
    // This is also a sample app only method & can be skipped.
    void onActivatedPickersChanged() {
        if (!cbDatePicker.isChecked()
                && !cbTimePicker.isChecked()
                && !cbRecurrencePicker.isChecked()) {

            // None of the pickers have been activated
            tvActivatedPickers.setText("Pickers to activate (choose at least one):");
            tvPickerToShow.setText("Picker to show on dialog creation: N/A");
        } else {
            // At least one picker is active
            tvActivatedPickers.setText("Pickers to activate:");
            tvPickerToShow.setText("Picker to show on dialog creation:");

            if ((rbDatePicker.isChecked() && rbDatePicker.getVisibility() != View.VISIBLE)
                    || (rbTimePicker.isChecked() && rbTimePicker.getVisibility() != View.VISIBLE)
                    || (rbRecurrencePicker.isChecked() && rbRecurrencePicker.getVisibility() != View.VISIBLE)) {
                if (rbDatePicker.getVisibility() == View.VISIBLE) {
                    rbDatePicker.setChecked(true);
                    return;
                }

                if (rbTimePicker.getVisibility() == View.VISIBLE) {
                    rbTimePicker.setChecked(true);
                    return;
                }

                if (rbRecurrencePicker.getVisibility() == View.VISIBLE) {
                    rbRecurrencePicker.setChecked(true);
                }
            }
        }
    }

    // Show date, time & recurrence options that have been selected
    private void updateInfoView() {
        tvYear.setText(applyBoldStyle("YEAR: ").append(String.valueOf(mYear)));
        tvMonth.setText(applyBoldStyle("MONTH: ").append(String.valueOf(mMonth)));
        tvDay.setText(applyBoldStyle("DAY: ").append(String.valueOf(mDay)));

        tvHour.setText(applyBoldStyle("HOUR: ").append(String.valueOf(mHour)));
        tvMinute.setText(applyBoldStyle("MINUTE: ").append(String.valueOf(mMinute)));

        tvRecurrenceOption.setText(applyBoldStyle("RECURRENCE OPTION: ")
                .append(mRecurrenceOption));
        tvRecurrenceRule.setText(applyBoldStyle("RECURRENCE RULE: ").append(
                mRecurrenceRule));

        rlDateTimeRecurrenceInfo.setVisibility(View.VISIBLE);
    }

    // Applies a StyleSpan to the supplied text
    private SpannableStringBuilder applyBoldStyle(String text) {
        SpannableStringBuilder ss = new SpannableStringBuilder(text);
        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    // Keys for saving state
    final String SS_DATE_PICKER_CHECKED = "saved.state.date.picker.checked";
    final String SS_TIME_PICKER_CHECKED = "saved.state.time.picker.checked";
    final String SS_RECURRENCE_PICKER_CHECKED = "saved.state.recurrence.picker.checked";
    final String SS_SHOW_EXTENDED_BG_CHECKED = "saved.state.show.extended.bg.checked";
    final String SS_SHOW_SINGLE_MONTH_CHECKED = "saved.state.show.single.month.checked";
    final String SS_YEAR = "saved.state.year";
    final String SS_MONTH = "saved.state.month";
    final String SS_DAY = "saved.state.day";
    final String SS_HOUR = "saved.state.hour";
    final String SS_MINUTE = "saved.state.minute";
    final String SS_RECURRENCE_OPTION = "saved.state.recurrence.option";
    final String SS_RECURRENCE_RULE = "saved.state.recurrence.rule";
    final String SS_INFO_VIEW_VISIBILITY = "saved.state.info.view.visibility";
    final String SS_SCROLL_Y = "saved.state.scroll.y";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save state of CheckBoxes
        // State of RadioButtons can be evaluated
        outState.putBoolean(SS_DATE_PICKER_CHECKED, cbDatePicker.isChecked());
        outState.putBoolean(SS_TIME_PICKER_CHECKED, cbTimePicker.isChecked());
        outState.putBoolean(SS_RECURRENCE_PICKER_CHECKED,
                cbRecurrencePicker.isChecked());
        outState.putBoolean(SS_SHOW_EXTENDED_BG_CHECKED,
                cbShowExtendedBg.isChecked());
        outState.putBoolean(SS_SHOW_SINGLE_MONTH_CHECKED,
                cbShowSingleMonthPerPosition.isChecked());

        // Save data
        outState.putInt(SS_YEAR, mYear);
        outState.putInt(SS_MONTH, mMonth);
        outState.putInt(SS_DAY, mDay);
        outState.putInt(SS_HOUR, mHour);
        outState.putInt(SS_MINUTE, mMinute);
        outState.putString(SS_RECURRENCE_OPTION, mRecurrenceOption);
        outState.putString(SS_RECURRENCE_RULE, mRecurrenceRule);
        outState.putBoolean(SS_INFO_VIEW_VISIBILITY,
                rlDateTimeRecurrenceInfo.getVisibility() == View.VISIBLE);
        outState.putInt(SS_SCROLL_Y, svMainContainer.getScrollY());

        super.onSaveInstanceState(outState);
    }
}
