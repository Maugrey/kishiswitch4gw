package fr.kishiswitch.engine

import org.junit.Assert.*
import org.junit.Test

class RemapEngineTest {
    private fun engine(options: Options = Options()) = RemapEngine(
        AxisSpec(14, -1f, 1f, .04f), TriggerSpec(17), TriggerSpec(18), initialOptions = options,
    )

    @Test fun eightSkillCombinations() {
        for (axis in listOf(17, 18)) for (button in RemapEngine.FACE_BUTTONS) {
            val e = engine()
            e.observeMotion(mapOf(axis to 1f))
            assertEquals(RemapEngine.swapped(button), e.keyDown(button).logical)
            assertEquals(RemapEngine.swapped(button), e.keyUp(button).logical)
        }
    }

    @Test fun faceButtonsAloneAreNotInjected() {
        val e = engine()
        RemapEngine.FACE_BUTTONS.forEach { key ->
            assertEquals(Route(key, key, false), e.keyDown(key))
            assertEquals(Route(key, key, false), e.keyUp(key))
        }
    }

    @Test fun releasingTriggerFirstRetainsReleaseAndRepeatRoute() {
        val e = engine()
        e.observeMotion(mapOf(17 to 1f))
        val down = e.keyDown(RemapEngine.X)
        e.observeMotion(mapOf(17 to 0f))
        assertEquals(down, e.keyDown(RemapEngine.X))
        assertEquals(down, e.keyUp(RemapEngine.X))
        assertEquals(RemapEngine.X, e.keyDown(RemapEngine.X).logical)
    }

    @Test fun pressingTriggerAfterButtonDoesNotInventAnotherPress() {
        val e = engine()
        val down = e.keyDown(RemapEngine.X)
        e.keyDown(RemapEngine.L2)
        assertEquals(down, e.keyDown(RemapEngine.X))
        assertEquals(down, e.keyUp(RemapEngine.X))
    }

    @Test fun simultaneousButtonsRemainIndependent() {
        val e = engine()
        e.observeMotion(mapOf(17 to 1f))
        e.keyDown(RemapEngine.L2)
        assertEquals(RemapEngine.A, e.keyDown(RemapEngine.X).logical)
        assertEquals(RemapEngine.X, e.keyDown(RemapEngine.A).logical)
        e.keyUp(RemapEngine.L2)
        e.observeMotion(mapOf(17 to 0f))
        assertEquals(RemapEngine.A, e.keyUp(RemapEngine.X).logical)
        assertEquals(RemapEngine.X, e.keyUp(RemapEngine.A).logical)
    }

    @Test fun digitalAndAnalogTriggerEventsDoNotCancelEachOther() {
        val e = engine()
        e.keyDown(RemapEngine.R2)
        e.observeMotion(mapOf(18 to .1f))
        assertFalse(e.modifierHeld)
        e.observeMotion(mapOf(18 to 1f))
        e.keyUp(RemapEngine.R2)
        assertTrue(e.modifierHeld)
        e.observeMotion(mapOf(18 to 0f))
        assertFalse(e.modifierHeld)
    }

    @Test fun thresholdHasHysteresis() {
        val e = engine()
        e.observeMotion(mapOf(17 to .51f)); assertTrue(e.modifierHeld)
        e.observeMotion(mapOf(17 to .47f)); assertTrue(e.modifierHeld)
        e.observeMotion(mapOf(17 to .4f)); assertFalse(e.modifierHeld)
    }

    @Test fun rightStickOnlyIsInvertedWithoutChangingAmplitude() {
        val e = engine()
        for (v in listOf(-1f, -.2f, 0f, .2f, 1f)) {
            val original = mapOf(0 to .3f, 1 to -.8f, 11 to .4f, 14 to v, 17 to .7f, 18 to .1f)
            e.observeMotion(original)
            val result = e.transformMotion(original)
            assertEquals(-v, result.getValue(14), .00001f)
            original.filterKeys { it != 14 }.forEach { (axis, value) -> assertEquals(value, result[axis]) }
            assertEquals(v, original[14])
        }
    }

    @Test fun asymmetricAxisIsInvertedAroundItsOwnCenter() {
        val axis = AxisSpec(13, 0f, 255f)
        assertEquals(127.5f, axis.invert(127.5f), 0f)
        assertEquals(255f, axis.invert(0f), 0f)
        assertEquals(55f, axis.invert(200f), 0f)
    }

    @Test fun switchesWaitIndependentlyForSafeBoundary() {
        val e = engine()
        e.observeMotion(mapOf(14 to .8f, 17 to 1f))
        e.keyDown(RemapEngine.A)
        e.requestOptions(Options(false, false))
        assertEquals(Options(), e.effective)
        e.observeMotion(mapOf(14 to 0f, 17 to 1f))
        assertEquals(Options(true, false), e.effective)
        e.keyUp(RemapEngine.A)
        assertTrue(e.effective.skills)
        e.observeMotion(mapOf(17 to 0f))
        assertEquals(Options(false, false), e.effective)
    }

    @Test fun resetReturnsOnlySyntheticReleasesAndClearsAllState() {
        val e = engine()
        e.observeMotion(mapOf(17 to 1f))
        e.keyDown(19)
        e.keyDown(RemapEngine.L2)
        e.keyDown(RemapEngine.X)
        e.requestOptions(Options(false, false))
        assertEquals(listOf(Route(RemapEngine.X, RemapEngine.A, true)), e.reset())
        assertTrue(e.canStop)
        assertEquals(Options(false, false), e.effective)
        assertFalse(e.keyDown(RemapEngine.X).injected)
    }

    @Test fun unavailableAxisDoesNotAffectSkills() {
        val e = RemapEngine(null, TriggerSpec(17), TriggerSpec(18))
        e.observeMotion(mapOf(17 to 1f, 14 to .5f))
        assertEquals(.5f, e.transformMotion(mapOf(14 to .5f))[14])
        assertEquals(RemapEngine.A, e.keyDown(RemapEngine.X).logical)
    }
}
