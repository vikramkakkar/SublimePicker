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

package com.appeaser.sublimepickerlibrary.drawables;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.Arrays;

/**
 * Used by 'ButtonLayout' to implement extended (full-height)
 * partition in landscape orientation. Portion of this drawable
 * is painted the same color as the header
 * of {SublimeDatePicker, SublimeTimePicker}. The portion-width
 * varies.
 */
public class ButtonBarBgDrawable extends Drawable {
    // Resective portion-width
    private int mDatePickerPartitionWidth, mTimePickerPartitionWidth;

    // Current picker - can be one of {SublimeDatePicker, SublimeTimePicker}
    private SublimeOptions.Picker mPicker;

    // Rects that are drawn depending on the visible Picker
    private RectF mDatePickerRect, mTimePickerRect;

    private Paint mPaint;

    // For API versions < Lollipop - one rounded corner
    private Path mPathDatePicker, mPathTimePicker;

    // corner radii for rounded rect
    private float[] mOuterRadii;

    public ButtonBarBgDrawable(Context context, int partitionBgColor,
                               SublimeOptions.Picker picker) {
        mPicker = picker;
        Resources res = context.getResources();

        mDatePickerPartitionWidth = res.getDimensionPixelSize(R.dimen.datepicker_component_width);
        mTimePickerPartitionWidth = res.getDimensionPixelSize(R.dimen.timepicker_left_side_width);

        if (!SUtils.isApi_21_OrHigher()) {
            mOuterRadii = new float[8];
            // First 6 items
            Arrays.fill(mOuterRadii, 0, 6, 0f);
            // 7th & 8th item
            Arrays.fill(mOuterRadii, 6, 8, SUtils.CORNER_RADIUS);
        }

        mPaint = new Paint();
        mPaint.setColor(partitionBgColor);
        mPaint.setAntiAlias(true);
    }

    /**
     * Updates this drawable's drawn portion based on
     * the supplied Picker. Valid candidates for the
     * supplied Picker are {SublimeDatePicker, SublimeTimePicker}.
     *
     * @param picker currently visible Picker
     */
    public void setPicker(SublimeOptions.Picker picker) {
        if (picker != SublimeOptions.Picker.DATE_PICKER
                && picker != SublimeOptions.Picker.TIME_PICKER) {
            throw new IllegalArgumentException("ButtonBarBgDrawable only " +
                    "works with Picker.DatePicker & Picker.TimePicker");
        }

        mPicker = picker;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mPicker == SublimeOptions.Picker.DATE_PICKER) {
            if (mDatePickerRect == null) {
                Rect bounds = getBounds();
                mDatePickerRect = new RectF(bounds.left, bounds.top,
                        bounds.left + mDatePickerPartitionWidth, bounds.bottom);
            }

            if (SUtils.isApi_21_OrHigher()) {
                canvas.drawRect(mDatePickerRect, mPaint);
            } else {
                if (mPathDatePicker == null) {
                    mPathDatePicker = new Path();
                    mPathDatePicker.addRoundRect(mDatePickerRect, mOuterRadii, Path.Direction.CW);
                }
                canvas.drawPath(mPathDatePicker, mPaint);
            }
        } else if (mPicker == SublimeOptions.Picker.TIME_PICKER) {
            if (mTimePickerRect == null) {
                Rect bounds = getBounds();
                mTimePickerRect = new RectF(bounds.left, bounds.top,
                        bounds.left + mTimePickerPartitionWidth, bounds.bottom);
            }

            if (SUtils.isApi_21_OrHigher()) {
                canvas.drawRect(mTimePickerRect, mPaint);
            } else {
                if (mPathTimePicker == null) {
                    mPathTimePicker = new Path();
                    mPathTimePicker.addRoundRect(mTimePickerRect, mOuterRadii, Path.Direction.CW);
                }
                canvas.drawPath(mPathTimePicker, mPaint);
            }
        } else { /* should't happen */
            canvas.drawColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
