package fr.kishiswitch.guildwars

import android.content.pm.PackageManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.kishiswitch.engine.AxisSpec
import fr.kishiswitch.engine.Options
import fr.kishiswitch.engine.RemapEngine
import fr.kishiswitch.engine.TriggerSpec
import fr.kishiswitch.guildwars.data.HardwareProfile
import fr.kishiswitch.guildwars.data.Settings
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.input.MotionAdapter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidBehaviorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun clean() { Settings(context).prefs.edit().clear().commit() }

    @Test fun preferencesStartEnabledButCapabilitiesAreNotCertified() {
        val settings = Settings(context)
        assertEquals(Options(true, true), settings.options)
        assertFalse(settings.validated(TrialStage.PASSTHROUGH))
        assertFalse(settings.validated(TrialStage.SKILLS))
        assertFalse(settings.validated(TrialStage.VERTICAL))
        settings.options = Options(false, true)
        assertEquals(Options(false, true), Settings(context).options)
    }

    @Test fun profileAndValidationPersistUntilConfigurationChanges() {
        val settings = Settings(context)
        val profile = HardwareProfile("test-kishi", "Kishi test", 0x1532, 123,
            TriggerSpec(17), TriggerSpec(18), AxisSpec(14, -1f, 1f, .05f), 190)
        settings.profile = profile
        assertEquals(profile, Settings(context).profile)
        settings.validate(TrialStage.PASSTHROUGH)
        settings.validate(TrialStage.SKILLS)
        settings.validate(TrialStage.VERTICAL)
        assertTrue(Settings(context).validated(TrialStage.SKILLS))
        settings.threshold = .25f
        assertFalse(Settings(context).validated(TrialStage.PASSTHROUGH))
        assertFalse(Settings(context).validated(TrialStage.SKILLS))
        assertEquals(profile, Settings(context).profile)
    }

    @Test fun skillsAndVerticalCannotBeValidatedWithoutTransport() {
        val settings = Settings(context)
        assertThrows(IllegalArgumentException::class.java) { settings.validate(TrialStage.SKILLS) }
        assertThrows(IllegalArgumentException::class.java) { settings.validate(TrialStage.VERTICAL) }
    }

    @Test fun motionAdapterPreservesHistoryAndAllOtherAxes() {
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_UNKNOWN })
        fun coords(rightY: Float) = arrayOf(MotionEvent.PointerCoords().apply {
            setAxisValue(MotionEvent.AXIS_X, .2f)
            setAxisValue(MotionEvent.AXIS_Y, -.3f)
            setAxisValue(MotionEvent.AXIS_Z, .4f)
            setAxisValue(MotionEvent.AXIS_RZ, rightY)
            setAxisValue(MotionEvent.AXIS_LTRIGGER, .8f)
            setAxisValue(MotionEvent.AXIS_HAT_X, -1f)
            setAxisValue(MotionEvent.AXIS_GENERIC_16, .6f)
        })
        val event = MotionEvent.obtain(100, 110, MotionEvent.ACTION_MOVE, 1, properties, coords(.25f),
            0, 0, 1f, 1f, 12, 0, InputDevice.SOURCE_JOYSTICK, 0)
        event.addBatch(120, coords(.75f), 0)
        val engine = RemapEngine(AxisSpec(14, -1f, 1f), TriggerSpec(17), TriggerSpec(18))
        val transformed = MotionAdapter.transform(event, engine)
        try {
            assertEquals(1, transformed.historySize)
            assertEquals(-.25f, transformed.getHistoricalAxisValue(14, 0), .00001f)
            assertEquals(-.75f, transformed.getAxisValue(14), .00001f)
            for (axis in listOf(0, 1, 11, 17, 15, MotionEvent.AXIS_GENERIC_16)) {
                assertEquals(event.getAxisValue(axis), transformed.getAxisValue(axis), .00001f)
                assertEquals(event.getHistoricalAxisValue(axis, 0), transformed.getHistoricalAxisValue(axis, 0), .00001f)
            }
            assertEquals(event.source, transformed.source)
            assertEquals(event.deviceId, transformed.deviceId)
            assertEquals(event.eventTime, transformed.eventTime)
            assertEquals(.75f, event.getAxisValue(14), .00001f)
            assertTrue(engine.modifierHeld)
        } finally { event.recycle(); transformed.recycle() }
    }

    @Test fun manifestDoesNotReplaceKeyboardOrRequestNotificationAndNetworkPermissions() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS)
        assertTrue(info.services.orEmpty().none { it.permission == "android.permission.BIND_INPUT_METHOD" })
        assertFalse(info.requestedPermissions.orEmpty().contains("android.permission.POST_NOTIFICATIONS"))
        assertFalse(info.requestedPermissions.orEmpty().contains("android.permission.INTERNET"))
    }
}
