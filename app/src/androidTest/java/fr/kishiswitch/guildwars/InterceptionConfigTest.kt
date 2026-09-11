package fr.kishiswitch.guildwars

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.InputDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.kishiswitch.guildwars.input.InterceptionConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterceptionConfigTest {
    @Test fun inactiveServiceClearsSubscriptionsRetainedByAndroid() {
        val info = AccessibilityServiceInfo().apply {
            setMotionEventSources(InputDevice.SOURCE_JOYSTICK)
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        assertTrue(InterceptionConfig.apply(info, motion = false, keys = false))
        assertEquals(0, info.motionEventSources)
        assertEquals(AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS, info.flags)
        assertFalse(InterceptionConfig.apply(info, motion = false, keys = false))
    }

    @Test fun intactTransmissionDoesNotRequestKeyFiltering() {
        val info = AccessibilityServiceInfo().apply {
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        InterceptionConfig.apply(info, motion = true, keys = false)
        assertEquals(InputDevice.SOURCE_JOYSTICK, info.motionEventSources)
        assertEquals(0, info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
    }

    @Test fun keyFilteringIsEnabledOnlyWhenExplicitlyNeeded() {
        val info = AccessibilityServiceInfo()
        InterceptionConfig.apply(info, motion = true, keys = true)
        assertTrue(info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS != 0)
        InterceptionConfig.apply(info, motion = false, keys = false)
        assertEquals(0, info.motionEventSources)
        assertEquals(0, info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
    }
}
