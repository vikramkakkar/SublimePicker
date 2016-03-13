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

package com.appeaser.sublimepickerlibrary.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

public class ButtonLayout extends LinearLayout implements View.OnClickListener {
    // Can be 'android.widget.Button' or 'android.widget.ImageView'
    View mPositiveButton, mNegativeButton;

    // 'Button' used for switching between 'SublimeDatePicker'
    // and 'SublimeTimePicker'. Also displays the currently
    // selected date/time depending on the visible picker
    Button mSwitcherButton;
    int mIconOverlayColor /* color used with the applied 'ColorFilter' */,
            mDisabledAlpha /* android.R.attr.disabledAlpha * 255 */,
            mButtonBarBgColor;

    ButtonHandler.Callback mCallback;

    public ButtonLayout(Context context) {
        this(context, null);
    }

    public ButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spButtonLayoutStyle);
    }

    public ButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spButtonLayoutStyle,
                R.style.ButtonLayoutStyle), attrs, defStyleAttr);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ButtonLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spButtonLayoutStyle,
                R.style.ButtonLayoutStyle), attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    void initialize() {
        Context context = getContext();
        final Resources res = getResources();

        final TypedArray a = context.obtainStyledAttributes(R.styleable.ButtonLayout);

        if (SUtils.isApi_17_OrHigher()) {
            setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        }

        setOrientation(HORIZONTAL);
        setGravity(Gravity.BOTTOM);

        setPadding(res.getDimensionPixelSize(R.dimen.sp_button_bar_padding_start),
                res.getDimensionPixelSize(R.dimen.sp_button_bar_padding_top),
                res.getDimensionPixelSize(R.dimen.sp_button_bar_padding_end),
                res.getDimensionPixelSize(R.dimen.sp_button_bar_padding_bottom));

        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sublime_button_panel_layout, this, true);

        mSwitcherButton = (Button) findViewById(R.id.buttonSwitcher);

        Button bPositive = (Button) findViewById(R.id.buttonPositive);
        Button bNegative = (Button) findViewById(R.id.buttonNegative);

        ImageView ivPositive = (ImageView) findViewById(R.id.imageViewPositive);
        ImageView ivNegative = (ImageView) findViewById(R.id.imageViewNegative);

        try {
            // obtain float value held by android.R.attr.disabledAlpha
            TypedValue typedValueDisabledAlpha = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.disabledAlpha,
                    typedValueDisabledAlpha, true);

            // defaults to 0.5 ~ 122/255
            mDisabledAlpha = typedValueDisabledAlpha.type == TypedValue.TYPE_FLOAT ?
                    (int) (typedValueDisabledAlpha.getFloat() * 255)
                    : 122;

            // buttons or icons?
            int presentation = a.getInt(R.styleable.ButtonLayout_spPresentation, 0);

            int bgColor = a.getColor(R.styleable.ButtonLayout_spButtonBgColor,
                    SUtils.COLOR_BUTTON_NORMAL);
            int pressedBgColor = a.getColor(R.styleable.ButtonLayout_spButtonPressedBgColor,
                    SUtils.COLOR_CONTROL_HIGHLIGHT);

            mButtonBarBgColor = a.getColor(R.styleable.ButtonLayout_spButtonBarBgColor,
                    Color.TRANSPARENT);
            SUtils.setViewBackground(mSwitcherButton,
                    SUtils.createButtonBg(context, bgColor,
                            pressedBgColor));
            setBackgroundColor(mButtonBarBgColor);

            if (presentation == 0 /* mode: Button */) {
                bPositive.setVisibility(View.VISIBLE);
                bNegative.setVisibility(View.VISIBLE);

                bPositive.setText(res.getString(R.string.ok));
                bNegative.setText(res.getString(R.string.cancel));

                SUtils.setViewBackground(bPositive,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(bNegative,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));

                mPositiveButton = bPositive;
                mNegativeButton = bNegative;
            } else /* mode: ImageView */ {
                ivPositive.setVisibility(View.VISIBLE);
                ivNegative.setVisibility(View.VISIBLE);

                mIconOverlayColor = a.getColor(R.styleable.ButtonLayout_spIconColor,
                        SUtils.COLOR_ACCENT);

                ivPositive.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);
                ivNegative.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);

                SUtils.setViewBackground(ivPositive,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(ivNegative,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));

                mPositiveButton = ivPositive;
                mNegativeButton = ivNegative;
            }
        } finally {
            a.recycle();
        }

        // set OnClickListeners
        mPositiveButton.setOnClickListener(this);
        mNegativeButton.setOnClickListener(this);
        mSwitcherButton.setOnClickListener(this);
    }

    /**
     * Initializes state for this layout
     *
     * @param switcherRequired Whether the switcher button needs
     *                         to be shown.
     * @param callback         Callback to 'SublimePicker'
     */
    public void applyOptions(boolean switcherRequired, @NonNull ButtonHandler.Callback callback) {
        mSwitcherButton.setVisibility(switcherRequired ? View.VISIBLE : View.GONE);
        mCallback = callback;
    }

    // Returns whether switcher button is being used in this layout
    public boolean isSwitcherButtonEnabled() {
        return mSwitcherButton.getVisibility() == View.VISIBLE;
    }

    // Used when the pickers are switched
    public void updateSwitcherText(CharSequence text) {
        mSwitcherButton.setText(text);
    }

    // Disables the positive button as and when the user selected options
    // become invalid.
    public void updateValidity(boolean valid) {
        mPositiveButton.setEnabled(valid);

        // TODO: Find a better way to do this
        // Disabled state for Icon presentation (only for the positive checkmark icon)
        if (mPositiveButton instanceof ImageView) {
            int color = mIconOverlayColor;

            if (!valid) {
                color = (mDisabledAlpha << 24) | (mIconOverlayColor & 0x00FFFFFF);
            }

            ((ImageView) mPositiveButton).setColorFilter(color,
                    PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mPositiveButton) {
            mCallback.onOkay();
        } else if (v == mNegativeButton) {
            mCallback.onCancel();
        } else if (v == mSwitcherButton) {
            mCallback.onSwitch();
        }
    }
}
