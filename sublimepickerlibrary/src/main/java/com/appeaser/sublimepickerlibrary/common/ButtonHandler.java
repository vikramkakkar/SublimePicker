package com.appeaser.sublimepickerlibrary.common;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.SublimePicker;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

/**
 * Created by Admin on 15/02/2016.
 */
public class ButtonHandler implements View.OnClickListener {

    private boolean mIsInLandscapeMode;

    private ButtonLayout mPortraitButtonHandler;

    // Can be 'android.widget.Button' or 'android.widget.ImageView'
    View mPositiveButtonDP, mPositiveButtonTP, mNegativeButtonDP, mNegativeButtonTP;
    // 'Button' used for switching between 'SublimeDatePicker'
    // and 'SublimeTimePicker'. Also displays the currently
    // selected date/time depending on the visible picker
    Button mSwitcherButtonDP, mSwitcherButtonTP;

    Callback mCallback;

    int mIconOverlayColor /* color used with the applied 'ColorFilter' */,
            mDisabledAlpha /* android.R.attr.disabledAlpha * 255 */,
            mButtonBarBgColor;

    public ButtonHandler(@NonNull SublimePicker sublimePicker) {
        Context context = sublimePicker.getContext();

        mIsInLandscapeMode = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (mIsInLandscapeMode) {
            initializeForLandscape(sublimePicker);
        } else {
            // Takes care of initialization
            mPortraitButtonHandler = (ButtonLayout) sublimePicker.findViewById(R.id.button_layout);
        }
    }

    private void initializeForLandscape(SublimePicker sublimeMaterialPicker) {
        Context context = SUtils.createThemeWrapper(sublimeMaterialPicker.getContext(),
                R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight,
                R.attr.spButtonLayoutStyle,
                R.style.ButtonLayoutStyle);
        final Resources res = context.getResources();

        final TypedArray a = context.obtainStyledAttributes(R.styleable.ButtonLayout);

        mSwitcherButtonDP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonSwitcherDP);
        mSwitcherButtonTP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonSwitcherTP);

        Button bPositiveDP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonPositiveDP);
        Button bPositiveTP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonPositiveTP);

        Button bNegativeDP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonNegativeDP);
        Button bNegativeTP = (Button) sublimeMaterialPicker.findViewById(R.id.buttonNegativeTP);

        ImageView ivPositiveDP = (ImageView) sublimeMaterialPicker.findViewById(R.id.imageViewPositiveDP);
        ImageView ivPositiveTP = (ImageView) sublimeMaterialPicker.findViewById(R.id.imageViewPositiveTP);

        ImageView ivNegativeDP = (ImageView) sublimeMaterialPicker.findViewById(R.id.imageViewNegativeDP);
        ImageView ivNegativeTP = (ImageView) sublimeMaterialPicker.findViewById(R.id.imageViewNegativeTP);

        try {
            // obtain float value held by android.R.attr.disabledAlpha
            TypedValue typedValueDisabledAlpha = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha,
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

            int buttonInvertedBgColor =
                    a.getColor(R.styleable.ButtonLayout_spButtonInvertedBgColor,
                            SUtils.COLOR_ACCENT);
            int buttonPressedInvertedBgColor =
                    a.getColor(R.styleable.ButtonLayout_spButtonPressedInvertedBgColor,
                            ContextCompat.getColor(context, R.color.sp_ripple_material_dark));
            SUtils.setViewBackground(mSwitcherButtonDP,
                    SUtils.createButtonBg(context, buttonInvertedBgColor,
                            buttonPressedInvertedBgColor));
            SUtils.setViewBackground(mSwitcherButtonTP,
                    SUtils.createButtonBg(context, buttonInvertedBgColor,
                            buttonPressedInvertedBgColor));

            if (presentation == 0 /* mode: Button */) {
                bPositiveDP.setVisibility(View.VISIBLE);
                bPositiveTP.setVisibility(View.VISIBLE);

                bNegativeDP.setVisibility(View.VISIBLE);
                bNegativeTP.setVisibility(View.VISIBLE);

                bPositiveDP.setText(res.getString(R.string.ok));
                bPositiveTP.setText(res.getString(R.string.ok));

                bNegativeDP.setText(res.getString(R.string.cancel));
                bNegativeTP.setText(res.getString(R.string.cancel));

                SUtils.setViewBackground(bPositiveDP,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(bPositiveTP,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));

                SUtils.setViewBackground(bNegativeDP,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(bNegativeTP,
                        SUtils.createButtonBg(context, bgColor,
                                pressedBgColor));

                mPositiveButtonDP = bPositiveDP;
                mPositiveButtonTP = bPositiveTP;

                mNegativeButtonDP = bNegativeDP;
                mNegativeButtonTP = bNegativeTP;
            } else /* mode: ImageView */ {
                ivPositiveDP.setVisibility(View.VISIBLE);
                ivPositiveTP.setVisibility(View.VISIBLE);

                ivNegativeDP.setVisibility(View.VISIBLE);
                ivNegativeTP.setVisibility(View.VISIBLE);

                mIconOverlayColor = a.getColor(R.styleable.ButtonLayout_spIconColor,
                        SUtils.COLOR_ACCENT);

                ivPositiveDP.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);
                ivPositiveTP.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);

                ivNegativeDP.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);
                ivNegativeTP.setColorFilter(mIconOverlayColor, PorterDuff.Mode.MULTIPLY);

                SUtils.setViewBackground(ivPositiveDP,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(ivPositiveTP,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));

                SUtils.setViewBackground(ivNegativeDP,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));
                SUtils.setViewBackground(ivNegativeTP,
                        SUtils.createImageViewBg(bgColor,
                                pressedBgColor));

                mPositiveButtonDP = ivPositiveDP;
                mPositiveButtonTP = ivPositiveTP;

                mNegativeButtonDP = ivNegativeDP;
                mNegativeButtonTP = ivNegativeTP;
            }
        } finally {
            a.recycle();
        }

        // set OnClickListeners
        mPositiveButtonDP.setOnClickListener(this);
        mPositiveButtonTP.setOnClickListener(this);

        mNegativeButtonDP.setOnClickListener(this);
        mNegativeButtonTP.setOnClickListener(this);

        mSwitcherButtonDP.setOnClickListener(this);
        mSwitcherButtonTP.setOnClickListener(this);
    }

    /**
     * Initializes state for this layout
     *
     * @param switcherRequired Whether the switcher button needs
     *                         to be shown.
     * @param callback         Callback to 'SublimePicker'
     */
    public void applyOptions(boolean switcherRequired, @NonNull Callback callback) {
        mCallback = callback;

        if (mIsInLandscapeMode) {
            mSwitcherButtonDP.setVisibility(switcherRequired ? View.VISIBLE : View.GONE);
            mSwitcherButtonTP.setVisibility(switcherRequired ? View.VISIBLE : View.GONE);
        } else {
            // Let ButtonLayout handle callbacks
            mPortraitButtonHandler.applyOptions(switcherRequired, callback);
        }
    }

    // Returns whether switcher button is being used in this layout
    public boolean isSwitcherButtonEnabled() {
        return mIsInLandscapeMode ?
                (mSwitcherButtonDP.getVisibility() == View.VISIBLE || mSwitcherButtonTP.getVisibility() == View.VISIBLE)
                : (mPortraitButtonHandler.isSwitcherButtonEnabled());
    }

    // Used when the pickers are switched
    public void updateSwitcherText(@NonNull SublimeOptions.Picker displayedPicker, CharSequence text) {
        if (mIsInLandscapeMode) {
            if (displayedPicker == SublimeOptions.Picker.DATE_PICKER) {
                mSwitcherButtonDP.setText(text);
            } else if (displayedPicker == SublimeOptions.Picker.TIME_PICKER) {
                mSwitcherButtonTP.setText(text);
            }
        } else {
            mPortraitButtonHandler.updateSwitcherText(text);
        }
    }

    // Disables the positive button as and when the user selected options
    // become invalid.
    public void updateValidity(boolean valid) {
        if (mIsInLandscapeMode) {
            mPositiveButtonDP.setEnabled(valid);
            mPositiveButtonTP.setEnabled(valid);

            // TODO: Find a better way to do this
            // Disabled state for Icon presentation (only for the positive check-mark icon)
            if (mPositiveButtonDP instanceof ImageView) {
                int color = mIconOverlayColor;

                if (!valid) {
                    color = (mDisabledAlpha << 24) | (mIconOverlayColor & 0x00FFFFFF);
                }

                ((ImageView) mPositiveButtonDP).setColorFilter(color,
                        PorterDuff.Mode.MULTIPLY);
                ((ImageView) mPositiveButtonTP).setColorFilter(color,
                        PorterDuff.Mode.MULTIPLY);
            }
        } else {
            mPortraitButtonHandler.updateValidity(valid);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mPositiveButtonDP || v == mPositiveButtonTP) {
            mCallback.onOkay();
        } else if (v == mNegativeButtonDP || v == mNegativeButtonTP) {
            mCallback.onCancel();
        } else if (v == mSwitcherButtonDP || v == mSwitcherButtonTP) {
            mCallback.onSwitch();
        }
    }

    public interface Callback {
        void onOkay();
        void onCancel();
        void onSwitch();
    }
}
