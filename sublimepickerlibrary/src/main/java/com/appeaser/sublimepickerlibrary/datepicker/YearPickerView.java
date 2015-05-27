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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.Calendar;

/**
 * Displays a selectable list of years.
 */
public class YearPickerView extends ListView implements AdapterView.OnItemClickListener,
        OnSublimeDateChangedListener {
    private final Calendar mMinDate = Calendar.getInstance();
    private final Calendar mMaxDate = Calendar.getInstance();

    private YearAdapter mAdapter;
    private int mViewSize;
    private int mChildSize;

    private DatePickerController mController;

    private int mSelectedPosition = -1;
    private int mYearSelectedCircleColor;

    public YearPickerView(Context context) {
        this(context, null);
    }

    public YearPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public YearPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spYearPickerStyle,
                R.style.YearPickerViewStyle), attrs, defStyleAttr);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public YearPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spYearPickerStyle,
                defStyleRes), attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    void initialize() {
        final LayoutParams frame = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(frame);

        final Resources res = getContext().getResources();
        mViewSize = res.getDimensionPixelOffset(R.dimen.datepicker_view_animator_height);
        mChildSize = res.getDimensionPixelOffset(R.dimen.datepicker_year_label_height);

        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(mChildSize / 3);

        final int paddingTop = res.getDimensionPixelSize(
                R.dimen.datepicker_year_picker_padding_top);
        setPadding(0, paddingTop, 0, 0);

        setOnItemClickListener(this);
        setDividerHeight(0);

        TypedArray typedArray = getContext().obtainStyledAttributes(R.styleable.YearPickerView);
        try {
            mYearSelectedCircleColor
                    = typedArray.getColor(R.styleable.YearPickerView_yearListSelectorColor,
                    SUtils.COLOR_CONTROL_ACTIVATED);
            setBackgroundColor(typedArray.getColor(R.styleable.YearPickerView_yearPickerBgColor,
                    Color.TRANSPARENT));
        } finally {
            typedArray.recycle();
        }

        mAdapter = new YearAdapter(getContext(), R.layout.year_label_text_view);
        setAdapter(mAdapter);
    }

    public void setRange(Calendar min, Calendar max) {
        mMinDate.setTimeInMillis(min.getTimeInMillis());
        mMaxDate.setTimeInMillis(max.getTimeInMillis());

        updateAdapterData();
    }

    public void init(DatePickerController controller) {
        mController = controller;
        mController.registerOnDateChangedListener(this);

        updateAdapterData();

        onDateChanged();
    }

    private void updateAdapterData() {
        mAdapter.clear();

        final int maxYear = mMaxDate.get(Calendar.YEAR);
        for (int year = mMinDate.get(Calendar.YEAR); year <= maxYear; year++) {
            mAdapter.add(year);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mController.tryVibrate();
        if (position != mSelectedPosition) {
            mSelectedPosition = position;
            mAdapter.notifyDataSetChanged();
        }
        mController.onYearSelected(mAdapter.getItem(position));
    }

    private class YearAdapter extends ArrayAdapter<Integer> {

        public YearAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CircularIndicatorTextView v = (CircularIndicatorTextView)
                    super.getView(position, convertView, parent);
            int year = getItem(position);
            boolean selected = mController.getSelectedDay().get(Calendar.YEAR) == year;
            v.setDrawIndicator(selected);
            if (selected) {
                v.setCircleColor(mYearSelectedCircleColor);
            }
            return v;
        }
    }

    public void postSetSelectionCentered(final int position) {
        postSetSelectionFromTop(position, mViewSize / 2 - mChildSize / 2);
    }

    public void postSetSelectionFromTop(final int position, final int offset) {
        post(new Runnable() {

            @Override
            public void run() {
                // False warning: method is available since API 1 in ListView.
                // But it was recently moved to AbsListView (API 21).
                // Safe
                setSelectionFromTop(position, offset);
                requestLayout();
            }
        });
    }

    public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    @Override
    public void onDateChanged() {
        updateAdapterData();
        mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(
                mController.getSelectedDay().get(Calendar.YEAR) - mMinDate.get(Calendar.YEAR));
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            event.setFromIndex(0);
            event.setToIndex(0);
        }
    }
}
