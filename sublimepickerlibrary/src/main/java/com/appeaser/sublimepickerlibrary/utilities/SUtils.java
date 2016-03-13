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

package com.appeaser.sublimepickerlibrary.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;

import com.appeaser.sublimepickerlibrary.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utilities
 */
public class SUtils {

    private static final String TAG = SUtils.class.getSimpleName();

    // Frequently used theme-dependent colors
    public static int COLOR_ACCENT,
            COLOR_CONTROL_HIGHLIGHT,
            COLOR_CONTROL_ACTIVATED,
            COLOR_BUTTON_NORMAL,
            COLOR_TEXT_PRIMARY,
            COLOR_TEXT_PRIMARY_INVERSE,
            COLOR_PRIMARY,
            COLOR_PRIMARY_DARK,
            COLOR_TEXT_SECONDARY,
            COLOR_BACKGROUND,
            COLOR_TEXT_SECONDARY_INVERSE;

    // Corner radius for drawables
    public static int CORNER_RADIUS;

    // flags for corners that need to be rounded
    public static final int CORNER_TOP_LEFT = 0x01, CORNER_TOP_RIGHT = 0x02,
            CORNER_BOTTOM_RIGHT = 0x04, CORNER_BOTTOM_LEFT = 0x08, CORNERS_ALL = 0x0f;

    public static void initializeResources(Context context) {
        TypedArray a = context.obtainStyledAttributes(
                new int[]{R.attr.colorAccent, R.attr.colorControlHighlight,
                        R.attr.colorControlActivated,
                        R.attr.colorButtonNormal, android.R.attr.textColorPrimary,
                        android.R.attr.textColorPrimaryInverse,
                        R.attr.colorPrimary,
                        R.attr.colorPrimaryDark,
                        android.R.attr.textColorSecondary,
                        android.R.attr.colorBackground,
                        android.R.attr.textColorSecondaryInverse});

        if (a.hasValue(0))
            COLOR_ACCENT = a.getColor(0, Color.TRANSPARENT);

        if (a.hasValue(1))
            COLOR_CONTROL_HIGHLIGHT = a.getColor(1, Color.TRANSPARENT);

        if (a.hasValue(2))
            COLOR_CONTROL_ACTIVATED = a.getColor(2, Color.TRANSPARENT);

        if (a.hasValue(3))
            COLOR_BUTTON_NORMAL = a.getColor(3, Color.TRANSPARENT);

        if (a.hasValue(4))
            COLOR_TEXT_PRIMARY = a.getColor(4, Color.TRANSPARENT);

        if (a.hasValue(5))
            COLOR_TEXT_PRIMARY_INVERSE = a.getColor(5, Color.TRANSPARENT);

        if (a.hasValue(6))
            COLOR_PRIMARY = a.getColor(6, Color.TRANSPARENT);

        if (a.hasValue(7))
            COLOR_PRIMARY_DARK = a.getColor(7, Color.TRANSPARENT);

        if (a.hasValue(8))
            COLOR_TEXT_SECONDARY = a.getColor(8, Color.TRANSPARENT);

        if (a.hasValue(9))
            COLOR_BACKGROUND = a.getColor(9, Color.TRANSPARENT);

        if (a.hasValue(10))
            COLOR_TEXT_SECONDARY_INVERSE = a.getColor(10, Color.TRANSPARENT);

        a.recycle();

        CORNER_RADIUS = context.getResources()
                .getDimensionPixelSize(R.dimen.control_corner_material);

        if (Config.DEBUG) {
            Log.i(TAG, "COLOR_ACCENT: " + Integer.toHexString(COLOR_ACCENT));
            Log.i(TAG, "COLOR_CONTROL_HIGHLIGHT: " + Integer.toHexString(COLOR_CONTROL_HIGHLIGHT));
            Log.i(TAG, "COLOR_CONTROL_ACTIVATED: " + Integer.toHexString(COLOR_CONTROL_ACTIVATED));
            Log.i(TAG, "COLOR_BUTTON_NORMAL: " + Integer.toHexString(COLOR_BUTTON_NORMAL));
            Log.i(TAG, "COLOR_TEXT_PRIMARY: " + Integer.toHexString(COLOR_TEXT_PRIMARY));
            Log.i(TAG, "COLOR_TEXT_PRIMARY_INVERSE: " + Integer.toHexString(COLOR_TEXT_PRIMARY_INVERSE));
            Log.i(TAG, "COLOR_PRIMARY: " + Integer.toHexString(COLOR_PRIMARY));
            Log.i(TAG, "COLOR_PRIMARY_DARK: " + Integer.toHexString(COLOR_PRIMARY_DARK));
            Log.i(TAG, "COLOR_TEXT_SECONDARY: " + Integer.toHexString(COLOR_TEXT_SECONDARY));
            Log.i(TAG, "COLOR_BACKGROUND: " + Integer.toHexString(COLOR_BACKGROUND));
            Log.i(TAG, "COLOR_TEXT_SECONDARY_INVERSE: " + Integer.toHexString(COLOR_TEXT_SECONDARY_INVERSE));
        }
    }

    public static boolean isApi_16_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isApi_17_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean isApi_18_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean isApi_21_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isApi_22_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static boolean isApi_23_OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static void setViewBackground(View view, Drawable bg) {
        int paddingL = view.getPaddingLeft();
        int paddingT = view.getPaddingTop();
        int paddingR = view.getPaddingRight();
        int paddingB = view.getPaddingBottom();

        if (isApi_16_OrHigher()) {
            view.setBackground(bg);
        } else {
            //noinspection deprecation
            view.setBackgroundDrawable(bg);
        }

        view.setPadding(paddingL, paddingT, paddingR, paddingB);
    }

    // Returns material styled button bg
    public static Drawable createButtonBg(Context context,
                                          int colorButtonNormal,
                                          int colorControlHighlight) {
        if (isApi_21_OrHigher()) {
            return createButtonRippleBg(context, colorButtonNormal,
                    colorControlHighlight);
        }

        return createButtonNormalBg(context, colorControlHighlight);
    }

    // Button bg for API versions >= Lollipop
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable createButtonRippleBg(Context context,
                                                 int colorButtonNormal,
                                                 int colorControlHighlight) {
        return new RippleDrawable(ColorStateList.valueOf(colorControlHighlight),
                null, createButtonShape(context, colorButtonNormal));
    }

    // Button bg for API version < Lollipop
    private static Drawable createButtonNormalBg(Context context, int colorControlHighlight) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                createButtonShape(context, colorControlHighlight));
        sld.addState(new int[]{},
                new ColorDrawable(Color.TRANSPARENT));
        return sld;
    }

    // Base button shape
    private static Drawable createButtonShape(Context context, int color) {
        // Translation of Lollipop's xml button-bg definition to Java
        int paddingH = context.getResources()
                .getDimensionPixelSize(R.dimen.button_padding_horizontal_material);
        int paddingV = context.getResources()
                .getDimensionPixelSize(R.dimen.button_padding_vertical_material);
        int insetH = context.getResources()
                .getDimensionPixelSize(R.dimen.button_inset_horizontal_material);
        int insetV = context.getResources()
                .getDimensionPixelSize(R.dimen.button_inset_vertical_material);

        float[] outerRadii = new float[8];
        Arrays.fill(outerRadii, CORNER_RADIUS);

        RoundRectShape r = new RoundRectShape(outerRadii, null, null);

        ShapeDrawable shapeDrawable = new ShapeDrawable(r);
        shapeDrawable.getPaint().setColor(color);
        shapeDrawable.setPadding(paddingH, paddingV, paddingH, paddingV);

        return new InsetDrawable(shapeDrawable,
                insetH, insetV, insetH, insetV);
    }

    // Drawable for icons in 'ButtonLayout'
    public static Drawable createImageViewBg(int colorButtonNormal, int colorControlHighlight) {
        if (isApi_21_OrHigher()) {
            return createImageViewRippleBg(colorButtonNormal, colorControlHighlight);
        }

        return createImageViewNormalBg(colorControlHighlight);
    }

    // Icon bg for API versions >= Lollipop
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable createImageViewRippleBg(int colorButtonNormal, int colorControlHighlight) {
        return new RippleDrawable(ColorStateList.valueOf(colorControlHighlight),
                null, createImageViewShape(colorButtonNormal));
    }

    // Icon bg for API versions < Lollipop
    private static Drawable createImageViewNormalBg(int colorControlHighlight) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                createImageViewShape(colorControlHighlight));
        sld.addState(new int[]{},
                new ColorDrawable(Color.TRANSPARENT));
        return sld;
    }

    // Base icon bg shape
    private static Drawable createImageViewShape(int color) {
        OvalShape ovalShape = new OvalShape();

        ShapeDrawable shapeDrawable = new ShapeDrawable(ovalShape);
        shapeDrawable.getPaint().setColor(color);

        return shapeDrawable;
    }

    // Borrowed from MathUtils
    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    // Borrowed from MathUtils
    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static boolean isLayoutRtlCompat(@NonNull View view) {
        return (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL);
    }

    // Creates a drawable with the supplied color and corner radii
    public static Drawable createBgDrawable(int color, int rTopLeft,
                                            int rTopRight, int rBottomRight,
                                            int rBottomLeft) {
        float[] outerRadii = new float[8];
        outerRadii[0] = rTopLeft;
        outerRadii[1] = rTopLeft;
        outerRadii[2] = rTopRight;
        outerRadii[3] = rTopRight;
        outerRadii[4] = rBottomRight;
        outerRadii[5] = rBottomRight;
        outerRadii[6] = rBottomLeft;
        outerRadii[7] = rBottomLeft;

        RoundRectShape r = new RoundRectShape(outerRadii, null, null);

        ShapeDrawable shapeDrawable = new ShapeDrawable(r);
        shapeDrawable.getPaint().setColor(color);

        return shapeDrawable;
    }

    public static Drawable createOverflowButtonBg(int pressedStateColor) {
        if (SUtils.isApi_21_OrHigher()) {
            return createOverflowButtonBgLollipop(pressedStateColor);
        }

        return createOverflowButtonBgBC(pressedStateColor);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable createOverflowButtonBgLollipop(int pressedStateColor) {
        return new RippleDrawable(
                ColorStateList.valueOf(pressedStateColor),
                null, null);
    }

    private static Drawable createOverflowButtonBgBC(int pressedStateColor) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                createBgDrawable(pressedStateColor,
                        0, CORNER_RADIUS, 0, 0));
        sld.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return sld;
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale      The locale.
     */
    public static Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    public static ContextThemeWrapper createThemeWrapper(Context context,
                                                         int parentStyleAttr, int parentDefaultStyle, int childStyleAttr,
                                                         int childDefaultStyle) {
        final TypedArray forParent = context.obtainStyledAttributes(
                new int[]{parentStyleAttr});
        int parentStyle = forParent.getResourceId(0, parentDefaultStyle);
        forParent.recycle();

        TypedArray forChild = context.obtainStyledAttributes(parentStyle,
                new int[]{childStyleAttr});
        int childStyleId = forChild.getResourceId(0, childDefaultStyle);
        forChild.recycle();

        return new ContextThemeWrapper(context, childStyleId);
    }

    public static void setViewBackground(View view, int bgColor, int corners) {
        if (SUtils.isApi_21_OrHigher()) {
            view.setBackgroundColor(bgColor);
        } else {
            SUtils.setViewBackground(view,
                    SUtils.createBgDrawable(bgColor,
                            (corners & CORNER_TOP_LEFT) != 0 ? CORNER_RADIUS : 0,
                            (corners & CORNER_TOP_RIGHT) != 0 ? CORNER_RADIUS : 0,
                            (corners & CORNER_BOTTOM_RIGHT) != 0 ? CORNER_RADIUS : 0,
                            (corners & CORNER_BOTTOM_LEFT) != 0 ? CORNER_RADIUS : 0));
        }
    }

    public static void setImageTintList(ImageView imageView, ColorStateList colorStateList) {
        if (isApi_21_OrHigher()) {
            imageView.setImageTintList(colorStateList);
        } else {
            Drawable drawable = imageView.getDrawable();

            if (drawable != null) {
                Drawable wrapped = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(wrapped, colorStateList);
                imageView.setImageDrawable(wrapped);
            }
        }
    }

    public static final int STATE_ENABLED = 1, STATE_ACTIVATED = 1 << 1, STATE_PRESSED = 1 << 2;

    private static final int[][] STATE_SETS = new int[8][];

    static {
        STATE_SETS[0] = new int[]{0};
        STATE_SETS[1] = new int[]{android.R.attr.state_enabled};
        STATE_SETS[2] = new int[]{android.R.attr.state_activated};
        STATE_SETS[3] = new int[]{android.R.attr.state_enabled, android.R.attr.state_activated};
        STATE_SETS[4] = new int[]{android.R.attr.state_pressed};
        STATE_SETS[5] = new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed};
        STATE_SETS[6] = new int[]{android.R.attr.state_activated, android.R.attr.state_pressed};
        STATE_SETS[7] = new int[]{android.R.attr.state_enabled, android.R.attr.state_activated, android.R.attr.state_pressed};
    }

    public static int[] resolveStateSet(int mask) {
        return STATE_SETS[mask];
    }

    /**
     * String for parsing dates.
     */
    private static final String DATE_FORMAT = "MM/dd/yyyy";

    /**
     * Date format for parsing dates.
     */
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    public static boolean parseDate(String date, Calendar outDate) {
        if (date == null || date.isEmpty()) {
            return false;
        }

        try {
            final Date parsedDate = DATE_FORMATTER.parse(date);
            outDate.setTime(parsedDate);
            return true;
        } catch (ParseException e) {
            Log.w(TAG, "Date: " + date + " not in format: " + DATE_FORMAT);
            return false;
        }
    }

    private static final int CHANGE_YEAR = 1582;

    /**
     * Borrowed from {@link java.util.GregorianCalendar}
     *
     * @param year Year to check
     * @return true if given `year` is a leap year, false otherwise
     */
    private static boolean isLeapYear(int year) {
        if (year > CHANGE_YEAR) {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        }

        return year % 4 == 0;
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
                // This is not correct. See isLeapYear(int) above
                //return (year % 4 == 0) ? 29 : 28;
                return isLeapYear(year) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void vibrateForDatePicker(View view) {
        // Using a different haptic feedback constant
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY
                                     /*(5) - HapticFeedbackConstants.CALENDAR_DATE*/);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void vibrateForTimePicker(View view) {
        view.performHapticFeedback(isApi_21_OrHigher() ?
                        HapticFeedbackConstants.CLOCK_TICK
                        : HapticFeedbackConstants.VIRTUAL_KEY
        );
    }
}
