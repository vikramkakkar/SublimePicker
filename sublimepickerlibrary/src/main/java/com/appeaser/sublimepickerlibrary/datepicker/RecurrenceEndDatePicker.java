package com.appeaser.sublimepickerlibrary.datepicker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import com.appeaser.sublimepickerlibrary.R;
import com.appeaser.sublimepickerlibrary.common.DecisionButtonLayout;
import com.appeaser.sublimepickerlibrary.utilities.AccessibilityUtils;
import com.appeaser.sublimepickerlibrary.utilities.Config;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Admin on 11/03/2016.
 */
public class RecurrenceEndDatePicker extends FrameLayout {
    private static final String TAG = RecurrenceEndDatePicker.class.getSimpleName();

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;

    private Context mContext;

    // Top-level container.
    private ViewGroup mContainer;

    // Picker view.
    private DayPickerView mDayPickerView;

    private RecurrenceEndDatePicker.OnDateSetListener mOnDateSetListener;

    private SelectedDate mCurrentDate;
    private Calendar mTempDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;

    private int mFirstDayOfWeek;

    private Locale mCurrentLocale;

    private DatePickerValidationCallback mValidationCallback;

    private DecisionButtonLayout mDecisionButtonLayout;

    private DecisionButtonLayout.Callback mDecisionButtonLayoutCallback = new DecisionButtonLayout.Callback() {
        @Override
        public void onOkay() {
            if (mOnDateSetListener != null) {
                mOnDateSetListener.onDateSet(RecurrenceEndDatePicker.this,
                        mCurrentDate.getStartDate().get(Calendar.YEAR),
                        mCurrentDate.getStartDate().get(Calendar.MONTH),
                        mCurrentDate.getStartDate().get(Calendar.DAY_OF_MONTH));
            }
        }

        @Override
        public void onCancel() {
            if (mOnDateSetListener != null) {
                mOnDateSetListener.onDateOnlyPickerCancelled(RecurrenceEndDatePicker.this);
            }
        }
    };

    public RecurrenceEndDatePicker(Context context) {
        this(context, null);
    }

    public RecurrenceEndDatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spDatePickerStyle);
    }

    public RecurrenceEndDatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeLayout(attrs, defStyleAttr, R.style.SublimeDatePickerStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecurrenceEndDatePicker(Context context, AttributeSet attrs,
                                   int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeLayout(attrs, defStyleAttr, defStyleRes);
    }

    private void initializeLayout(AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        mContext = getContext();

        setCurrentLocale(Locale.getDefault());
        mCurrentDate = new SelectedDate(Calendar.getInstance(mCurrentLocale));
        mTempDate = Calendar.getInstance(mCurrentLocale);
        mMinDate = Calendar.getInstance(mCurrentLocale);
        mMaxDate = Calendar.getInstance(mCurrentLocale);

        mMinDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1);
        mMaxDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);

        final Resources res = getResources();
        final TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.SublimeDatePicker, defStyleAttr, defStyleRes);
        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final int layoutResourceId = R.layout.recurrence_end_date_picker;

        try {
            // Set up and attach container.
            mContainer = (ViewGroup) inflater.inflate(layoutResourceId, this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addView(mContainer);

        int firstDayOfWeek = a.getInt(R.styleable.SublimeDatePicker_spFirstDayOfWeek,
                mCurrentDate.getFirstDate().getFirstDayOfWeek());

        final String minDate = a.getString(R.styleable.SublimeDatePicker_spMinDate);
        final String maxDate = a.getString(R.styleable.SublimeDatePicker_spMaxDate);

        // Set up min and max dates.
        final Calendar tempDate = Calendar.getInstance();

        if (!SUtils.parseDate(minDate, tempDate)) {
            tempDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1);
        }

        final long minDateMillis = tempDate.getTimeInMillis();

        if (!SUtils.parseDate(maxDate, tempDate)) {
            tempDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);
        }

        final long maxDateMillis = tempDate.getTimeInMillis();

        if (maxDateMillis < minDateMillis) {
            throw new IllegalArgumentException("maxDate must be >= minDate");
        }

        final long setDateMillis = SUtils.constrain(
                System.currentTimeMillis(), minDateMillis, maxDateMillis);

        mMinDate.setTimeInMillis(minDateMillis);
        mMaxDate.setTimeInMillis(maxDateMillis);
        mCurrentDate.setTimeInMillis(setDateMillis);

        a.recycle();

        mDecisionButtonLayout = (DecisionButtonLayout) mContainer.findViewById(R.id.redp_decision_button_layout);
        mDecisionButtonLayout.applyOptions(mDecisionButtonLayoutCallback);

        // Set up day picker view.
        mDayPickerView = (DayPickerView) mContainer.findViewById(R.id.redp_day_picker);
        setFirstDayOfWeek(firstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate);
        mDayPickerView.setProxyDaySelectionEventListener(mProxyDaySelectionEventListener);
        mDayPickerView.setCanPickRange(false);

        // Set up content descriptions.
        String selectDay = res.getString(R.string.select_day);

        // Initialize for current locale. This also initializes the date, so no
        // need to call onDateChanged.
        onLocaleChanged(mCurrentLocale);
        AccessibilityUtils.makeAnnouncement(mDayPickerView, selectDay);
    }

    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private final DayPickerView.ProxyDaySelectionEventListener mProxyDaySelectionEventListener
            = new DayPickerView.ProxyDaySelectionEventListener() {
        @Override
        public void onDaySelected(DayPickerView view, Calendar day) {
            mCurrentDate = new SelectedDate(day);
            onDateChanged(true, true);
        }

        @Override
        public void onDateRangeSelectionStarted(@NonNull SelectedDate selectedDate) {
            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false);
        }

        @Override
        public void onDateRangeSelectionEnded(@Nullable SelectedDate selectedDate) {
            if (selectedDate != null) {
                mCurrentDate = new SelectedDate(selectedDate);
                onDateChanged(false, false);
            }
        }

        @Override
        public void onDateRangeSelectionUpdated(@NonNull SelectedDate selectedDate) {
            if (Config.DEBUG) {
                Log.i(TAG, "onDateRangeSelectionUpdated: " + selectedDate.toString());
            }

            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false);
        }
    };

    private void onLocaleChanged(Locale locale) {
        final DayPickerView dayPickerView = mDayPickerView;
        if (dayPickerView == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        // Update the header text.
        onCurrentDateChanged(false);
    }

    private void onCurrentDateChanged(boolean announce) {
        if (mDayPickerView == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        // TODO: This should use live regions.
        if (announce) {
            final long millis = mCurrentDate.getStartDate().getTimeInMillis();
            final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            final String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
            AccessibilityUtils.makeAnnouncement(mDayPickerView, fullDateText);
        }
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param year        The initial year.
     * @param monthOfYear The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth  The initial day of the month.
     * @param callback    How user is notified date is changed by
     *                    user, can be null.
     */
    public void init(int year, int monthOfYear, int dayOfMonth,
                     RecurrenceEndDatePicker.OnDateSetListener callback) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, monthOfYear);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        mOnDateSetListener = callback;

        onDateChanged(false, true);
    }

    /**
     * Update the current date.
     *
     * @param year       The year.
     * @param month      The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    @SuppressWarnings("unused")
    public void updateDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        onDateChanged(false, true);
    }

    // callbackToClient is useless for now & gives us an unnecessary round-trip
    // by calling init(...)
    private void onDateChanged(boolean fromUser, boolean goToPosition) {
        mDayPickerView.setDate(new SelectedDate(mCurrentDate), false, goToPosition);

        onCurrentDateChanged(fromUser);

        if (fromUser) {
            SUtils.vibrateForDatePicker(RecurrenceEndDatePicker.this);
        }
    }

    public SelectedDate getSelectedDate() {
        return new SelectedDate(mCurrentDate);
    }

    public long getSelectedDateInMillis() {
        return mCurrentDate.getStartDate().getTimeInMillis();
    }

    /**
     * Sets the minimal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.getStartDate().before(mTempDate)) {
            mCurrentDate.getStartDate().setTimeInMillis(minDate);
            onDateChanged(false, true);
        }
        mMinDate.setTimeInMillis(minDate);
        mDayPickerView.setMinDate(minDate);
    }

    /**
     * Gets the minimal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default minimal date is 01/01/1900.
     *
     * @return The minimal supported date.
     */
    @SuppressWarnings("unused")
    public Calendar getMinDate() {
        return mMinDate;
    }

    /**
     * Sets the maximal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.getEndDate().after(mTempDate)) {
            mCurrentDate.getEndDate().setTimeInMillis(maxDate);
            onDateChanged(false, true);
        }
        mMaxDate.setTimeInMillis(maxDate);
        mDayPickerView.setMaxDate(maxDate);
    }

    /**
     * Gets the maximal date supported by this {@link SublimeDatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default maximal date is 12/31/2100.
     *
     * @return The maximal supported date.
     */
    @SuppressWarnings("unused")
    public Calendar getMaxDate() {
        return mMaxDate;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            if (Config.DEBUG) {
                Log.e(TAG, "Provided `firstDayOfWeek` is invalid - it must be between 1 and 7. " +
                        "Given value: " + firstDayOfWeek + " Picker will use the default value for the given locale.");
            }

            firstDayOfWeek = mCurrentDate.getFirstDate().getFirstDayOfWeek();
        }

        mFirstDayOfWeek = firstDayOfWeek;

        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() == enabled) {
            return;
        }

        mContainer.setEnabled(enabled);
        mDayPickerView.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mContainer.isEnabled();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    private void setCurrentLocale(Locale locale) {
        if (!locale.equals(mCurrentLocale)) {
            mCurrentLocale = locale;
            onLocaleChanged(locale);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        int listPosition = mDayPickerView.getMostVisiblePosition();

        return new SavedState(superState, mCurrentDate, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), listPosition);
    }

    @SuppressLint("NewApi")
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        Calendar date = Calendar.getInstance(mCurrentLocale);
        date.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());

        mCurrentDate.setDate(date);

        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());

        onCurrentDateChanged(false);

        final int listPosition = ss.getListPosition();
        if (listPosition != -1) {
            mDayPickerView.setPosition(listPosition);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mCurrentDate.getStartDate().getTime().toString());
    }

    public CharSequence getAccessibilityClassName() {
        return SublimeDatePicker.class.getName();
    }

    public void setValidationCallback(DatePickerValidationCallback callback) {
        mValidationCallback = callback;
    }

    @SuppressWarnings("unused")
    protected void onValidationChanged(boolean valid) {
        if (mValidationCallback != null) {
            mValidationCallback.onDatePickerValidationChanged(valid);
        }

        mDecisionButtonLayout.updateValidity(valid);
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     */
    public interface DatePickerValidationCallback {
        void onDatePickerValidationChanged(boolean valid);
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends BaseSavedState {

        private final int mSelectedYear;
        private final int mSelectedMonth;
        private final int mSelectedDay;
        private final long mMinDate;
        private final long mMaxDate;
        private final int mListPosition;

        /**
         * Constructor called from {@link SublimeDatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, SelectedDate selectedDate,
                           long minDate, long maxDate, int listPosition) {
            super(superState);
            mSelectedYear = selectedDate.getStartDate().get(Calendar.YEAR);
            mSelectedMonth = selectedDate.getStartDate().get(Calendar.MONTH);
            mSelectedDay = selectedDate.getStartDate().get(Calendar.DAY_OF_MONTH);
            mMinDate = minDate;
            mMaxDate = maxDate;
            mListPosition = listPosition;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSelectedYear = in.readInt();
            mSelectedMonth = in.readInt();
            mSelectedDay = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
            mListPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYear);
            dest.writeInt(mSelectedMonth);
            dest.writeInt(mSelectedDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mListPosition);
        }

        public int getSelectedDay() {
            return mSelectedDay;
        }

        public int getSelectedMonth() {
            return mSelectedMonth;
        }

        public int getSelectedYear() {
            return mSelectedYear;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public int getListPosition() {
            return mListPosition;
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * The callback used to indicate the user changed the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view         The view associated with this listener.
         * @param selectedDate The date that was set.
         */
        void onDateChanged(RecurrenceEndDatePicker view, SelectedDate selectedDate);
    }

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {

        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateSet(RecurrenceEndDatePicker view, int year, int monthOfYear, int dayOfMonth);

        void onDateOnlyPickerCancelled(RecurrenceEndDatePicker view);
    }
}
