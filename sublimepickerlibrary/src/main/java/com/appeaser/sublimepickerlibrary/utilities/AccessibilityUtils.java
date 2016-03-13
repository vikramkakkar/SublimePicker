/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.appeaser.sublimepickerlibrary.utilities;

import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * AccessibilityUtils provides functions needed in accessibility mode. All the functions
 * in this class are made compatible with gingerbread and later APIs
 * Borrowed from /packages/apps/Gallery2/src/com/android/gallery3d/util/AccessibilityUtils.java
 */
public class AccessibilityUtils {
    public static void makeAnnouncement(View view, CharSequence announcement) {
        if (view == null)
            return;
        if (SUtils.isApi_16_OrHigher()) {
            view.announceForAccessibility(announcement);
        } else {
            // For API 15 and earlier, we need to construct an accessibility event
            Context ctx = view.getContext();
            AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(
                    Context.ACCESSIBILITY_SERVICE);
            if (!am.isEnabled()) return;

            AccessibilityEvent event = AccessibilityEvent.obtain(
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
            AccessibilityRecordCompat arc = AccessibilityEventCompat.asRecord(event);
            arc.setSource(view);
            event.setClassName(view.getClass().getName());
            event.setPackageName(view.getContext().getPackageName());
            event.setEnabled(view.isEnabled());
            event.getText().add(announcement);
            am.sendAccessibilityEvent(event);
        }
    }
}
