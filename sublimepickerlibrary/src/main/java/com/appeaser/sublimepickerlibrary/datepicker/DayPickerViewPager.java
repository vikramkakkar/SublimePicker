/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.utilities.Config;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * This displays a list of months in a calendar format with selectable days.
 */
class DayPickerViewPager extends ViewPager {

    private static final String TAG = DayPickerViewPager.class.getSimpleName();

    private final int MONTH_SCROLL_THRESHOLD;
    private final int TOUCH_SLOP_SQUARED;

    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    private Method mPopulateMethod;
    private boolean mAlreadyTriedAccessingMethod;

    private boolean mCanPickRange;
    private DayPickerPagerAdapter mDayPickerPagerAdapter;

    private float mInitialDownX, mInitialDownY;
    private boolean mIsLongPressed = false;

    private CheckForLongPress mCheckForLongPress;
    private SelectedDate mTempSelectedDate;

    // Scrolling support
    private static final int SCROLLING_LEFT = -1;
    private static final int NOT_SCROLLING = 0;
    private static final int SCROLLING_RIGHT = 1;
    private ScrollerRunnable mScrollerRunnable;
    private int mScrollingDirection = NOT_SCROLLING;

    public DayPickerViewPager(Context context) {
        this(context, null);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        TOUCH_SLOP_SQUARED = ViewConfiguration.get(context).getScaledTouchSlop()
                * ViewConfiguration.get(context).getScaledTouchSlop();
        MONTH_SCROLL_THRESHOLD = context.getResources()
                .getDimensionPixelSize(R.dimen.sp_month_scroll_threshold);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //populate();
        // Use reflection
        callPopulate();

        // Everything below is mostly copied from FrameLayout.
        int count = getChildCount();

        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                            lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
        if (SUtils.isApi_23_OrHigher()) {
            final Drawable drawable = getForeground();
            if (drawable != null) {
                maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
                maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
            }
        }

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        count = mMatchParentChildren.size();
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int childWidthMeasureSpec;
                final int childHeightMeasureSpec;

                if (lp.width == LayoutParams.MATCH_PARENT) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight(),
                            lp.width);
                }

                if (lp.height == LayoutParams.MATCH_PARENT) {
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            getPaddingTop() + getPaddingBottom(),
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        mMatchParentChildren.clear();
    }

    private void initializePopulateMethod() {
        try {
            mPopulateMethod = ViewPager.class.getDeclaredMethod("populate", (Class[]) null);
            mPopulateMethod.setAccessible(true);
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        }

        mAlreadyTriedAccessingMethod = true;
    }

    private void callPopulate() {
        if (!mAlreadyTriedAccessingMethod) {
            initializePopulateMethod();
        }

        if (mPopulateMethod != null) {
            // Multi-catch block cannot be used before API 19
            //noinspection TryWithIdenticalCatches
            try {
                mPopulateMethod.invoke(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Could not call `ViewPager.populate()`");
        }
    }

    protected void setCanPickRange(boolean canPickRange) {
        mCanPickRange = canPickRange;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mCanPickRange) {
            return super.onInterceptTouchEvent(ev);
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (Config.DEBUG) {
                Log.i(TAG, "OITE: DOWN");
            }

            mInitialDownX = ev.getX();
            mInitialDownY = ev.getY();

            if (mCheckForLongPress == null) {
                mCheckForLongPress = new CheckForLongPress();
            }

            postDelayed(mCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        } else if (ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (Config.DEBUG) {
                Log.i(TAG, "OITE: (UP || CANCEL)");
            }

            if (mCheckForLongPress != null) {
                removeCallbacks(mCheckForLongPress);
            }

            mIsLongPressed = false;
            mInitialDownX = -1;
            mInitialDownY = -1;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (Config.DEBUG) {
                Log.i(TAG, "OITE: MOVE");
            }

            if (!isStillALongPress((int) ev.getX(), (int) ev.getY())) {
                if (Config.DEBUG) {
                    Log.i(TAG, "OITE: MOVED TOO MUCH, CANCELLING CheckForLongPress Runnable");
                }

                if (mCheckForLongPress != null) {
                    removeCallbacks(mCheckForLongPress);
                }
            }
        }

        return mIsLongPressed || super.onInterceptTouchEvent(ev);
    }

    private boolean isStillALongPress(int x, int y) {
        return (((x - mInitialDownX) * (x - mInitialDownX))
                + ((y - mInitialDownY) * (y - mInitialDownY))) <= TOUCH_SLOP_SQUARED;
    }

    private class CheckForLongPress implements Runnable {
        @Override
        public void run() {
            if (mDayPickerPagerAdapter != null) {

                mTempSelectedDate = mDayPickerPagerAdapter.resolveStartDateForRange((int) mInitialDownX,
                        (int) mInitialDownY, getCurrentItem());

                if (mTempSelectedDate != null) {
                    if (Config.DEBUG) {
                        Log.i(TAG, "CheckForLongPress Runnable Fired");
                    }

                    mIsLongPressed = true;
                    mDayPickerPagerAdapter.onDateRangeSelectionStarted(mTempSelectedDate);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mCanPickRange) {
            return super.onTouchEvent(ev);
        }

        // looks like the ViewPager wants to step in
        if (mCheckForLongPress != null) {
            removeCallbacks(mCheckForLongPress);
        }

        if (mIsLongPressed && ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && (UP || CANCEL)");
            }

            if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (mDayPickerPagerAdapter != null) {
                    mTempSelectedDate = mDayPickerPagerAdapter.resolveEndDateForRange((int) ev.getX(),
                            (int) ev.getY(), getCurrentItem(), false);
                    mDayPickerPagerAdapter.onDateRangeSelectionEnded(mTempSelectedDate);
                }
            }

            mIsLongPressed = false;
            mInitialDownX = -1;
            mInitialDownY = -1;
            mScrollingDirection = NOT_SCROLLING;

            if (mScrollerRunnable != null) {
                removeCallbacks(mScrollerRunnable);
            }
            //return true;
        } else if (mIsLongPressed && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && DOWN");
            }

            mScrollingDirection = NOT_SCROLLING;
        } else if (mIsLongPressed && ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && MOVE");
            }

            int direction = resolveDirectionForScroll(ev.getX());
            boolean directionChanged = mScrollingDirection != direction;

            if (directionChanged) {
                if (mScrollerRunnable != null) {
                    removeCallbacks(mScrollerRunnable);
                }
            }

            if (mScrollerRunnable == null) {
                mScrollerRunnable = new ScrollerRunnable();
            }

            mScrollingDirection = direction;

            if (mScrollingDirection == NOT_SCROLLING) {
                if (mDayPickerPagerAdapter != null) {
                    mTempSelectedDate = mDayPickerPagerAdapter.resolveEndDateForRange((int) ev.getX(),
                            (int) ev.getY(), getCurrentItem(), true);

                    if (mTempSelectedDate != null) {
                        mDayPickerPagerAdapter.onDateRangeSelectionUpdated(mTempSelectedDate);
                    }
                }
            } else if (directionChanged) { // SCROLLING_LEFT || SCROLLING_RIGHT
                post(mScrollerRunnable);
            }
        }

        return mIsLongPressed || super.onTouchEvent(ev);
    }

    private int resolveDirectionForScroll(float x) {
        if (x - getLeft() < MONTH_SCROLL_THRESHOLD) {
            return SCROLLING_LEFT;
        } else if (getRight() - x < MONTH_SCROLL_THRESHOLD) {
            return SCROLLING_RIGHT;
        }

        return NOT_SCROLLING;
    }

    private class ScrollerRunnable implements Runnable {
        @Override
        public void run() {
            if (mScrollingDirection == NOT_SCROLLING) {
                return;
            }

            final int direction = mScrollingDirection;

            // Animation is expensive for accessibility services since it sends
            // lots of scroll and content change events.
            final boolean animate = true; //!mAccessibilityManager.isEnabled()

            // ViewPager clamps input values, so we don't need to worry
            // about passing invalid indices.
            setCurrentItem(getCurrentItem() + direction, animate);

            // Four times the default anim duration
            postDelayed(this, 1000L);
        }
    }

    // May need to refer to this later
    /*@Override
    public boolean onTouchEvent(MotionEvent ev) {
        // looks like the ViewPager wants to step in
        if (mCheckForLongPress != null) {
            removeCallbacks(mCheckForLongPress);
        }

        if (mIsLongPressed && ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && (UP || CANCEL)");
            }

            if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (mDayPickerPagerAdapter != null) {
                    mTempSelectedDate = mDayPickerPagerAdapter.resolveEndDateForRange((int)ev.getX(),
                            (int)ev.getY(), getCurrentItem(), false);
                    mDayPickerPagerAdapter.onDateRangeSelectionEnded(mTempSelectedDate);
                }
            }

            mIsLongPressed = false;
            mInitialDownX = -1;
            mInitialDownY = -1;

            if (mScrollerRunnable != null) {
                removeCallbacks(mScrollerRunnable);
            }
            //return true;
        } else if (mIsLongPressed &&  ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && DOWN");
            }
        } else if (mIsLongPressed &&  ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (Config.DEBUG) {
                Log.i(TAG, "OTE: LONGPRESS && MOVE");
            }

            int direction = resolveDirectionForScroll(ev.getX(), ev.getY());

            if (direction == 0) {
                mScrollingLeft = false;
                mScrollingRight = false;

                if (mScrollerRunnable != null) {
                    removeCallbacks(mScrollerRunnable);
                }

                if (mDayPickerPagerAdapter != null) {
                    mTempSelectedDate = mDayPickerPagerAdapter.resolveEndDateForRange((int)ev.getX(),
                            (int)ev.getY(), getCurrentItem(), true);

                    if (mTempSelectedDate != null) {
                        mDayPickerPagerAdapter.onDateRangeSelectionUpdated(mTempSelectedDate);
                    }
                }
            } else if (direction == -1) {
                if (mScrollingLeft) {
                    // nothing
                } else if (mScrollingRight) {
                    mScrollingRight = false;
                    mScrollingLeft = true;

                    if (mScrollerRunnable != null) {
                        removeCallbacks(mScrollerRunnable);
                    }

                    if (mScrollerRunnable == null) {
                        mScrollerRunnable = new ScrollerRunnable();
                    }

                    post(mScrollerRunnable);
                } else {
                    mScrollingLeft = true;

                    if (mScrollerRunnable == null) {
                        mScrollerRunnable = new ScrollerRunnable();
                    }

                    post(mScrollerRunnable);
                }
            } else if (direction == 1) {
                if (mScrollingRight) {
                    // nothing
                } else if (mScrollingLeft) {
                    mScrollingLeft = false;
                    mScrollingRight = true;

                    if (mScrollerRunnable != null) {
                        removeCallbacks(mScrollerRunnable);
                    }

                    if (mScrollerRunnable == null) {
                        mScrollerRunnable = new ScrollerRunnable();
                    }

                    post(mScrollerRunnable);
                } else {
                    mScrollingRight = true;

                    if (mScrollerRunnable == null) {
                        mScrollerRunnable = new ScrollerRunnable();
                    }

                    post(mScrollerRunnable);
                }
            }
        }

        return mIsLongPressed || super.onTouchEvent(ev);
    }

    private int resolveDirectionForScroll(float x, float y) {
        if (x - getLeft() < MONTH_SCROLL_THRESHOLD) {
            return -1;
        } else if (getRight() - x < MONTH_SCROLL_THRESHOLD) {
            return 1;
        }

        return 0;
    }

    public class ScrollerRunnable implements Runnable {
        @Override
        public void run() {
            final int direction;
            if (mScrollingLeft) {
                direction = -1;
            } else if (mScrollingRight) {
                direction = 1;
            } else {
                return;
            }

            // Animation is expensive for accessibility services since it sends
            // lots of scroll and content change events.
            final boolean animate = true; //!mAccessibilityManager.isEnabled()

            // ViewPager clamps input values, so we don't need to worry
            // about passing invalid indices.
            setCurrentItem(getCurrentItem() + direction, animate);

            // Four times the default anim duration
            postDelayed(this, 1000L);
        }
    }*/

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof DayPickerPagerAdapter) {
            mDayPickerPagerAdapter = (DayPickerPagerAdapter) adapter;
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        // A 'null' hack may be required to keep the ViewPager
        // from saving its own state.

        // Since we were using two
        // ViewPagers with the same ID, state restoration was
        // having issues ==> wrong 'current' item. The approach
        // I am currently using is to define two different layout
        // files, which contain ViewPagers with different IDs.
        // If this approach does not pan out, we will need to
        // employ a 'null' hack where the ViewPager does not
        // get to see 'state' in 'onRestoreInstanceState(Parecelable)'.
        // super.onRestoreInstanceState(null);
        super.onRestoreInstanceState(state);
    }
}
