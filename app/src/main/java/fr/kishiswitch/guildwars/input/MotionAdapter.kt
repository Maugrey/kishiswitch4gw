package fr.kishiswitch.guildwars.input

import android.view.InputDevice
import android.view.MotionEvent
import fr.kishiswitch.engine.RemapEngine

object MotionAdapter {
    /** Copy all 64 Android axes, including history; only the calibrated axis may change. */
    fun transform(event: MotionEvent, engine: RemapEngine): MotionEvent {
        val properties = Array(event.pointerCount) { MotionEvent.PointerProperties().also { p -> event.getPointerProperties(it, p) } }
        fun coordinates(history: Int): Array<MotionEvent.PointerCoords> = Array(event.pointerCount) { pointer ->
            MotionEvent.PointerCoords().also { coords ->
                if (history < 0) event.getPointerCoords(pointer, coords) else event.getHistoricalPointerCoords(pointer, history, coords)
                val values = (0..63).associateWith { coords.getAxisValue(it) }
                if (pointer == 0) engine.observeMotion(values)
                // Do not materialize absent zero axes: native PointerCoords has a finite packed capacity.
                engine.transformMotion(values).forEach { (axis, value) ->
                    if (value != values[axis]) coords.setAxisValue(axis, value)
                }
            }
        }
        val first = if (event.historySize > 0) 0 else -1
        val firstTime = if (first >= 0) event.getHistoricalEventTime(first) else event.eventTime
        val out = MotionEvent.obtain(event.downTime, firstTime, event.action, event.pointerCount,
            properties, coordinates(first), event.metaState, event.buttonState, event.xPrecision, event.yPrecision,
            event.deviceId, event.edgeFlags, event.source, event.flags)
        if (event.historySize > 0) {
            for (i in 1 until event.historySize) out.addBatch(event.getHistoricalEventTime(i), coordinates(i), event.metaState)
            out.addBatch(event.eventTime, coordinates(-1), event.metaState)
        }
        return out
    }

    fun values(event: MotionEvent): Map<Int, Float> = (0..63).associateWith { event.getAxisValue(it) }

    fun isNeutral(event: MotionEvent, device: InputDevice): Boolean = device.motionRanges
        .filter { it.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK }
        .all { range ->
            val center = when (range.axis) {
                MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS -> range.min
                else -> (range.min + range.max) / 2f
            }
            kotlin.math.abs(event.getAxisValue(range.axis) - center) <= maxOf(range.flat, range.range * .025f)
        }

    fun neutral(event: MotionEvent, device: InputDevice): MotionEvent {
        val coords = Array(event.pointerCount) { MotionEvent.PointerCoords().also { p ->
            device.motionRanges.filter { it.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK }
                .forEach { range ->
                    val rest = when (range.axis) {
                        MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS -> range.min
                        else -> (range.min + range.max) / 2f
                    }
                    p.setAxisValue(range.axis, rest)
                }
        } }
        val properties = Array(event.pointerCount) { MotionEvent.PointerProperties().also { p -> event.getPointerProperties(it, p) } }
        return MotionEvent.obtain(event.downTime, android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
            event.pointerCount, properties, coords, 0, 0, event.xPrecision, event.yPrecision, event.deviceId, 0, event.source, 0)
    }
}
