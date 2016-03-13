package com.appeaser.sublimepickerlibrary.datepicker;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created by Admin on 25/02/2016.
 */
public class SelectedDate {
    public enum Type {SINGLE, RANGE}

    private Calendar mFirstDate, mSecondDate;

    public SelectedDate(Calendar startDate, Calendar endDate) {
        mFirstDate = startDate;
        mSecondDate = endDate;
    }

    public SelectedDate(Calendar date) {
        mFirstDate = mSecondDate = date;
    }

    // TODO: Should be requiring Locale
    public SelectedDate(SelectedDate date) {
        mFirstDate = Calendar.getInstance();
        mSecondDate = Calendar.getInstance();

        if (date != null) {
            mFirstDate.setTimeInMillis(date.getStartDate().getTimeInMillis());
            mSecondDate.setTimeInMillis(date.getEndDate().getTimeInMillis());
        }
    }

    public Calendar getFirstDate() {
        return mFirstDate;
    }

    public void setFirstDate(Calendar firstDate) {
        mFirstDate = firstDate;
    }

    public Calendar getSecondDate() {
        return mSecondDate;
    }

    public void setSecondDate(Calendar secondDate) {
        mSecondDate = secondDate;
    }

    public void setDate(Calendar date) {
        mFirstDate = date;
        mSecondDate = date;
    }

    public Calendar getStartDate() {
        return compareDates(mFirstDate, mSecondDate) == -1 ? mFirstDate : mSecondDate;
    }

    public Calendar getEndDate() {
        return compareDates(mFirstDate, mSecondDate) == 1 ? mFirstDate : mSecondDate;
    }

    public Type getType() {
        return compareDates(mFirstDate, mSecondDate) == 0 ? Type.SINGLE : Type.RANGE;
    }

    // a & b should never be null, so don't perform a null check here.
    // Let the source of error identify itself.
    public static int compareDates(Calendar a, Calendar b) {
        int aYear = a.get(Calendar.YEAR);
        int bYear = b.get(Calendar.YEAR);

        int aMonth = a.get(Calendar.MONTH);
        int bMonth = b.get(Calendar.MONTH);

        int aDayOfMonth = a.get(Calendar.DAY_OF_MONTH);
        int bDayOfMonth = b.get(Calendar.DAY_OF_MONTH);

        if (aYear < bYear) {
            return -1;
        } else if (aYear > bYear) {
            return 1;
        } else {
            if (aMonth < bMonth) {
                return -1;
            } else if (aMonth > bMonth) {
                return 1;
            } else {
                if (aDayOfMonth < bDayOfMonth) {
                    return -1;
                } else if (aDayOfMonth > bDayOfMonth) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    public void setTimeInMillis(long timeInMillis) {
        mFirstDate.setTimeInMillis(timeInMillis);
        mSecondDate.setTimeInMillis(timeInMillis);
    }

    public void set(int field, int value) {
        mFirstDate.set(field, value);
        mSecondDate.set(field, value);
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        if (mFirstDate != null) {
            toReturn.append(DateFormat.getDateInstance().format(mFirstDate.getTime()));
            toReturn.append("\n");
        }

        if (mSecondDate != null) {
            toReturn.append(DateFormat.getDateInstance().format(mSecondDate.getTime()));
        }

        return toReturn.toString();
    }
}
