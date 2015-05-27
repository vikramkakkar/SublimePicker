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
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.appeaser.sublimepickerlibrary.R;

/**
 * Material style overflow icon
 */
public class OverflowDrawable extends Drawable {

    Paint mPaintCircle;
    PointF center1, center2, center3;
    float mRadius;
    int mWidthHeight;

    public OverflowDrawable(Context context, int color) {
        Resources res = context.getResources();
        mWidthHeight = res.getDimensionPixelSize(R.dimen.options_size);
        float density = res.getDisplayMetrics().densityDpi / 160f;

        mRadius = 2 * density;

        float centerXY = mWidthHeight / 2f;

        center1 = new PointF(centerXY, centerXY - 6 * density/* 6dp */);
        center2 = new PointF(centerXY, centerXY);
        center3 = new PointF(centerXY, centerXY + 6 * density/* 6dp */);

        mPaintCircle = new Paint();
        mPaintCircle.setColor(color);
        mPaintCircle.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(center1.x, center1.y, mRadius, mPaintCircle);
        canvas.drawCircle(center2.x, center2.y, mRadius, mPaintCircle);
        canvas.drawCircle(center3.x, center3.y, mRadius, mPaintCircle);
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
        mPaintCircle.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaintCircle.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
