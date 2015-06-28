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
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.appeaser.sublimepickerlibrary.R;

/**
 * Currently not used.
 * Draws a pair of lines at 45 degrees and 135 degrees
 * that pass through the center.
 */
public class CancelDrawable extends Drawable {
    Paint mPaintLines;
    int mWidthHeight;

    float[] mLines;

    public CancelDrawable(Context context, int color) {
        Resources res = context.getResources();
        mWidthHeight = res.getDimensionPixelSize(R.dimen.close_drawable_size);
        float density = res.getDisplayMetrics().densityDpi / 160f;

        float centerXY = mWidthHeight / 2f;

        mLines = new float[]{centerXY - 4 * density, centerXY - 4 * density,
                centerXY + 4 * density, centerXY + 4 * density,
                centerXY - 4 * density, centerXY + 4 * density,
                centerXY + 4 * density, centerXY - 4 * density};

        float strokeWidth = 2 * density;

        mPaintLines = new Paint();
        mPaintLines.setStyle(Paint.Style.STROKE);
        mPaintLines.setStrokeWidth(strokeWidth);
        mPaintLines.setStrokeCap(Paint.Cap.ROUND);
        mPaintLines.setColor(color);
        mPaintLines.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawLines(mLines, mPaintLines);
    }

    @Override
    public int getMinimumHeight() {
        return mWidthHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidthHeight;
    }

    @Override
    public int getIntrinsicHeight() {
        return mWidthHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidthHeight;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaintLines.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaintLines.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}

