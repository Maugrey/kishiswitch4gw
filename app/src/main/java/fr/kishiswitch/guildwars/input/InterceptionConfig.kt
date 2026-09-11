package fr.kishiswitch.guildwars.input

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.InputDevice

/** Reconcile Android's actual subscription, including any state retained after a service restart. */
object InterceptionConfig {
    fun apply(info: AccessibilityServiceInfo, motion: Boolean, keys: Boolean): Boolean {
        val sources = if (motion) InputDevice.SOURCE_JOYSTICK else 0
        val keyFlag = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        val flags = if (keys) info.flags or keyFlag else info.flags and keyFlag.inv()
        val changed = info.motionEventSources != sources || info.flags != flags
        info.setMotionEventSources(sources)
        info.flags = flags
        return changed
    }
}
