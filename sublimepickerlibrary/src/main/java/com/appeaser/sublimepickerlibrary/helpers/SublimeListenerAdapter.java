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

package com.appeaser.sublimepickerlibrary.helpers;

import com.appeaser.sublimepickerlibrary.SublimePicker;
import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;

import java.util.Date;

public abstract class SublimeListenerAdapter {
    /**
     * @param sublimeMaterialPicker SublimeMaterialPicker view
     * @param selectedDate          The date that was set.
     * @param hourOfDay             The hour of day that was set.
     * @param minute                The minute that was set.
     * @param recurrenceOption      One of the options defined in
     *                              SublimeRecurrencePicker.RecurrenceOption.
     *                              'recurrenceRule' will only be passed if
     *                              'recurrenceOption' is 'CUSTOM'.
     * @param recurrenceRule        The recurrence rule that was set. This will
     *                              be 'null' if 'recurrenceOption' is anything
     *                              other than 'CUSTOM'.
     */
    public abstract void onDateTimeRecurrenceSet(SublimePicker sublimeMaterialPicker,
                                                 SelectedDate selectedDate,
                                                 int hourOfDay, int minute,
                                                 SublimeRecurrencePicker.RecurrenceOption recurrenceOption,
                                                 String recurrenceRule);

    // Cancel button or icon clicked
    public abstract void onCancelled();

    /**
     * @param selectedDate The date(or range) that is selected.
     * @return Formatted date to display on `Switcher` button
     */
    @SuppressWarnings("UnusedParameters")
    public CharSequence formatDate(SelectedDate selectedDate) {
        return null;
    }

    /**
     * @param selectedTime The time of day that was set.
     * @return Formatted time to display on `Switcher` button
     */
    @SuppressWarnings("UnusedParameters")
    public CharSequence formatTime(Date selectedTime) {
        return null;
    }
}
