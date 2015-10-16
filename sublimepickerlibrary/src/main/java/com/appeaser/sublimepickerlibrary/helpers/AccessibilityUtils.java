package com.appeaser.sublimepickerlibrary.helpers;

/**
 * Created by Slizer on 16.10.2015.
 */

import android.content.Context;
import android.os.Build;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * AccessibilityUtils provides functions needed in accessibility mode. All the functions
 * in this class are made compatible with gingerbread and later API's
 */
public class AccessibilityUtils {
    public static void makeAnnouncement(View view, CharSequence announcement) {
        if (view == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.announceForAccessibility(announcement);
        } else {
            // For API 15 and earlier, we need to construct an accessibility event
            Context ctx = view.getContext();
            AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(
                    Context.ACCESSIBILITY_SERVICE);
            if (!am.isEnabled()) return;
            AccessibilityEvent event = AccessibilityEvent.obtain(
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
            AccessibilityRecordCompat arc = new AccessibilityRecordCompat(event);
            arc.setSource(view);
            event.setClassName(view.getClass().getName());
            event.setPackageName(view.getContext().getPackageName());
            event.setEnabled(view.isEnabled());
            event.getText().add(announcement);
            am.sendAccessibilityEvent(event);
        }
    }
}
