package fr.kishiswitch.engine

/** Pure state machine. Android event delivery and device identification live in the app. */
data class Options(val skills: Boolean = true, val rightVertical: Boolean = true)

data class AxisSpec(val id: Int, val min: Float, val max: Float, val flat: Float = 0f) {
    val center: Float get() = (min + max) / 2f
    fun neutral(value: Float) = kotlin.math.abs(value - center) <= maxOf(flat, (max - min) * .025f)
    fun invert(value: Float): Float = (min + max - value).coerceIn(min, max)
}

data class TriggerSpec(val axis: Int, val min: Float = 0f, val max: Float = 1f) {
    fun fraction(value: Float): Float = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f
}

data class Route(val physical: Int, val logical: Int, val injected: Boolean)

class RemapEngine(
    private val rightAxis: AxisSpec?,
    private val leftTrigger: TriggerSpec?,
    private val rightTrigger: TriggerSpec?,
    val triggerThreshold: Float = .5f,
    initialOptions: Options = Options(),
) {
    companion object {
        const val A = 96
        const val B = 97
        const val X = 99
        const val Y = 100
        const val L2 = 104
        const val R2 = 105
        val FACE_BUTTONS = setOf(A, B, X, Y)
        fun swapped(code: Int): Int = when (code) { A -> X; X -> A; B -> Y; Y -> B; else -> code }
    }

    private val held = linkedMapOf<Int, Route>()
    private var leftAnalog = false
    private var rightAnalog = false
    private var leftDigital = false
    private var rightDigital = false
    private var rightValue = rightAxis?.center ?: 0f
    var effective: Options = initialOptions
        private set
    var requested: Options = initialOptions
        private set
    val hasHeldButtons: Boolean get() = held.isNotEmpty()
    // Calibrated analog axes are authoritative; duplicate L2/R2 keys cannot bypass the threshold.
    val modifierHeld: Boolean get() = leftAnalog || rightAnalog ||
        (leftTrigger == null && leftDigital) || (rightTrigger == null && rightDigital)
    val hasPendingOptions: Boolean get() = effective != requested
    val canStop: Boolean get() = held.isEmpty() && !modifierHeld && (rightAxis?.neutral(rightValue) != false)

    fun requestOptions(options: Options) {
        requested = options
        applyPending()
    }

    /** Originals (not transformed samples) update the modifier state. */
    fun observeMotion(values: Map<Int, Float>) {
        leftTrigger?.let { spec -> values[spec.axis]?.let { leftAnalog = hysteresis(leftAnalog, spec.fraction(it)) } }
        rightTrigger?.let { spec -> values[spec.axis]?.let { rightAnalog = hysteresis(rightAnalog, spec.fraction(it)) } }
        rightAxis?.let { values[it.id]?.let { v -> rightValue = v } }
        applyPending()
    }

    fun transformMotion(values: Map<Int, Float>): Map<Int, Float> {
        if (!effective.rightVertical || rightAxis == null || !values.containsKey(rightAxis.id)) return values.toMap()
        return values.toMutableMap().apply { put(rightAxis.id, rightAxis.invert(getValue(rightAxis.id))) }
    }

    fun keyDown(code: Int): Route {
        // Repeats retain the exact route chosen at the first DOWN.
        held[code]?.let { return it }
        if (code == L2) leftDigital = true
        if (code == R2) rightDigital = true
        val logical = if (effective.skills && modifierHeld) swapped(code) else code
        return Route(code, logical, logical != code).also { held[code] = it }
    }

    fun keyUp(code: Int): Route {
        val route = held.remove(code) ?: Route(code, code, false)
        if (code == L2) leftDigital = false
        if (code == R2) rightDigital = false
        applyPending()
        return route
    }

    /** Returned routes are the synthetic keys for which the caller must send UP. */
    fun reset(): List<Route> {
        val releases = held.values.filter { it.injected }
        held.clear()
        leftAnalog = false; rightAnalog = false; leftDigital = false; rightDigital = false
        rightValue = rightAxis?.center ?: 0f
        effective = requested
        return releases
    }

    private fun hysteresis(wasHeld: Boolean, value: Float): Boolean =
        value >= if (wasHeld) (triggerThreshold - .05f).coerceAtLeast(.01f) else triggerThreshold

    private fun applyPending() {
        val faceReleased = held.keys.none { it in FACE_BUTTONS }
        effective = Options(
            skills = if (faceReleased && !modifierHeld) requested.skills else effective.skills,
            rightVertical = if (rightAxis?.neutral(rightValue) != false) requested.rightVertical else effective.rightVertical,
        )
    }
}
