package com.appeaser.sublimepickerlibrary.timepicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Implementation of {@link android.widget.Space} that uses normal View drawing
 * rather than a no-op. Useful for dialogs and other places where the base View
 * class is too greedy when measured with AT_MOST.
 */
public final class PickerDrawingSpace extends View {

    public PickerDrawingSpace(Context context) {
        this(context, null);
    }

    public PickerDrawingSpace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PickerDrawingSpace(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PickerDrawingSpace(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Compare to: {@link View#getDefaultSize(int, int)}
     * <p/>
     * If mode is AT_MOST, return the child size instead of the parent size
     * (unless it is too big).
     */
    private static int getDefaultSizeNonGreedy(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSizeNonGreedy(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSizeNonGreedy(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
}
