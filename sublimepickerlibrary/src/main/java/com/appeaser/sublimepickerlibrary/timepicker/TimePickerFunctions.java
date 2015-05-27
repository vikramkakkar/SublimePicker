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

package com.appeaser.sublimepickerlibrary.timepicker;

import android.content.res.Configuration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

interface TimePickerFunctions {
    void setCurrentHour(Integer currentHour);

    Integer getCurrentHour();

    void setCurrentMinute(Integer currentMinute);

    Integer getCurrentMinute();

    void setIs24HourView(boolean is24HourView);

    boolean is24HourView();

    void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener);

    void setValidationCallback(ValidationCallback callback);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    int getBaseline();

    void onConfigurationChanged(Configuration newConfig);

    boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

    void onPopulateAccessibilityEvent(AccessibilityEvent event);

    void onInitializeAccessibilityEvent(AccessibilityEvent event);

    void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);

    /**
     * A callback interface for updating input validity when the TimePicker
     * when included into a Dialog.
     */
    interface ValidationCallback {
        void onTimePickerValidationChanged(boolean valid);
    }

    /**
     * The callback interface used to indicate the time has been adjusted.
     */
    interface OnTimeChangedListener {

        /**
         * @param view      The view associated with this listener.
         * @param hourOfDay The current hour.
         * @param minute    The current minute.
         */
        void onTimeChanged(SublimeTimePicker view, int hourOfDay, int minute);
    }
}
