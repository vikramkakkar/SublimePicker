/*
 * Copyright 2015 Vikram Kakkar
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

public interface DatePickerFunctions {
    void init(int year, int monthOfYear, int dayOfMonth,
              OnDateChangedListener onDateChangedListener);

    void updateDate(int year, int month, int dayOfMonth);

    int getYear();

    int getMonth();

    int getDayOfMonth();

    void setFirstDayOfWeek(int firstDayOfWeek);

    int getFirstDayOfWeek();

    void setMinDate(long minDate);

    long getMinDate();

    void setMaxDate(long maxDate);

    long getMaxDate();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setValidationCallback(ValidationCallback callback);

    /**
     * The callback used to indicate the user changes\d the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link java.util.Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateChanged(SublimeDatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     */
    interface ValidationCallback {
        void onDatePickerValidationChanged(boolean valid);
    }
}
