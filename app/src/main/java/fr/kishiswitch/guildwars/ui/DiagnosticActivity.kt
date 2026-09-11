package fr.kishiswitch.guildwars.ui

import fr.kishiswitch.guildwars.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import fr.kishiswitch.engine.AxisSpec
import fr.kishiswitch.engine.TriggerSpec
import fr.kishiswitch.guildwars.KishiApplication
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.data.GAME_PACKAGE
import fr.kishiswitch.guildwars.data.HardwareProfile
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.ui.Ui.action
import fr.kishiswitch.guildwars.ui.Ui.heading
import fr.kishiswitch.guildwars.ui.Ui.paragraph

class DiagnosticActivity : Activity() {
    private val app get() = application as KishiApplication
    private val settings get() = app.settings
    private lateinit var live: TextView
    private var capture: String? = null
    private val amplitudes = mutableMapOf<Int, Float>()
    private val observedMin = mutableMapOf<Int, Float>()
    private val observedMax = mutableMapOf<Int, Float>()
    private val observedKeys = mutableSetOf<Int>()
    private var candidateM2: Int? = null
    private var lastDisplayAt = 0L
    private var receiverTest = false
    private var receivedKeys = 0
    private var receivedMotions = 0
    private val listener: () -> Unit = { runOnUiThread { if (capture == null) render() } }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onResume() {
        super.onResume()
        RuntimeState.ownUiVisible = true
        RuntimeState.endTrial(this@DiagnosticActivity)
        RuntimeState.diagnosticVisible = true
        RuntimeState.listen(listener)
        RuntimeState.changed()
        render()
    }
    override fun onPause() {
        RuntimeState.unlisten(listener)
        RuntimeState.ownUiVisible = false
        RuntimeState.changed()
        RuntimeState.diagnosticVisible = false
        receiverTest = false
        super.onPause()
    }

    private fun render() {
        if (isFinishing || isDestroyed) return
        val p = settings.profile
        if (RuntimeState.trialStamp != settings.currentStamp()) RuntimeState.trialReadyToConfirm = TrialStage.NONE
        val screen = Ui.screen(this)
        screen.paragraph(getString(R.string.setup_kicker), Ui.accent)
        screen.heading(getString(R.string.verify_kishi_title), true)
        screen.paragraph(getString(R.string.diagnostic_intro))
        screen.action(getString(R.string.back_to_inversions)) { finish() }
        screen.action(getString(R.string.stop_service), RuntimeState.serviceConnected) { RuntimeState.stopService?.invoke() }
        screen.heading(getString(R.string.identify_controller_title))
        screen.paragraph(p?.name ?: getString(R.string.connect_press_button))
        screen.paragraph(if (p == null) getString(R.string.no_controller_identified) else getString(R.string.hardware_saved, p.vendor, p.product))
        live = screen.paragraph(getString(R.string.input_preview_hint), Ui.accent)
        screen.heading(getString(R.string.identify_controls_title))
        screen.action(getString(R.string.identify_lt), p != null) { beginCapture("lt") }
        screen.action(getString(R.string.identify_rt), p != null) { beginCapture("rt") }
        screen.action(getString(R.string.identify_right_vertical), p != null) { beginCapture("ry") }
        screen.paragraph(getString(R.string.m2_overlay_hint))
        screen.paragraph(getString(R.string.identified_axes, p?.leftTrigger?.axis?.let(MotionEvent::axisToString) ?: getString(R.string.needs_identification), p?.rightTrigger?.axis?.let(MotionEvent::axisToString) ?: getString(R.string.needs_identification), p?.rightVertical?.id?.let(MotionEvent::axisToString) ?: getString(R.string.needs_identification), p?.m2Code?.let(KeyEvent::keyCodeToString) ?: getString(R.string.overlay_used)))
        screen.action(getString(R.string.save_control), capture != null) { saveCapture() }
        screen.action(getString(R.string.cancel_identification), capture != null) { capture = null; render() }
        screen.heading(getString(R.string.trigger_threshold_title))
        screen.paragraph(getString(R.string.trigger_threshold_help))
        val thresholdLabel = screen.paragraph(getString(R.string.trigger_threshold_value, (settings.threshold * 100).toInt()))
        val slider = SeekBar(this).apply {
            min = 5; max = 95; progress = (settings.threshold * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { thresholdLabel.text = getString(R.string.trigger_threshold_value, progress) }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) { settings.threshold = (seekBar?.progress ?: 50) / 100f; RuntimeState.changed() }
            })
        }
        screen.addView(slider)
        screen.heading(getString(R.string.verify_connection_title))
        screen.paragraph(app.bridge.state)
        screen.action(getString(R.string.shizuku_reconnect)) { app.bridge.connect(true) }
        screen.paragraph(getString(R.string.relay_trial_hint))
        screen.heading(getString(R.string.game_trials_title))
        for (stage in TrialStage.entries.filter { it != TrialStage.NONE }) {
            val valid = settings.validated(stage)
            val available = app.bridge.ready && RuntimeState.serviceConnected && p?.ready == true &&
                (stage == TrialStage.PASSTHROUGH || settings.validated(TrialStage.PASSTHROUGH)) &&
                (stage != TrialStage.VERTICAL || p?.rightVertical != null)
            screen.action(getString(R.string.trial_button, stage.label(this@DiagnosticActivity), if (valid) getString(R.string.validated) else getString(R.string.try_test)), available) { startTrial(stage) }
        }
        if (RuntimeState.trial != TrialStage.NONE) screen.action(getString(R.string.stop_pending_trial)) { RuntimeState.endTrial(this@DiagnosticActivity) }
        val pending = RuntimeState.trialReadyToConfirm
        if (pending != TrialStage.NONE) {
            screen.paragraph(getString(R.string.trial_returned, pending.label(this@DiagnosticActivity), RuntimeState.trialEvents), Ui.accent)
            screen.action(getString(R.string.confirm_trial_button)) { confirmTrial(pending) }
            screen.action(getString(R.string.trial_failed_button)) {
                settings.unvalidate(pending); RuntimeState.trialReadyToConfirm = TrialStage.NONE
                RuntimeState.record(getString(R.string.trial_failed_log, pending.label(this@DiagnosticActivity)))
                RuntimeState.changed()
            }
        }
        screen.paragraph(getString(R.string.validation_preferences_hint))
        screen.heading(getString(R.string.local_report_title))
        screen.action(getString(R.string.show_report)) {
            val report = getString(R.string.report_header, fr.kishiswitch.guildwars.BuildConfig.VERSION_NAME, android.os.Build.VERSION.RELEASE) +
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                "${p?.name ?: getString(R.string.controller_missing)}\n${app.bridge.state}\n" +
                getString(R.string.report_interception, RuntimeState.capturingMotion, RuntimeState.filteringKeys) +
                getString(R.string.report_hid, RuntimeState.relayActive) +
                getString(R.string.report_axes, p?.leftTrigger?.axis, p?.rightTrigger?.axis, p?.rightVertical?.id) +
                TrialStage.entries.filter { it != TrialStage.NONE }.joinToString("\n") { getString(R.string.report_validation, it.label(this@DiagnosticActivity), if (settings.validated(it)) getString(R.string.user_confirmed) else getString(R.string.not_validated)) } +
                "\n\n" + RuntimeState.report()
            AlertDialog.Builder(this).setTitle(getString(R.string.diagnostics)).setMessage(report)
                .setPositiveButton(getString(R.string.close), null).setNeutralButton(getString(R.string.share)) { _, _ ->
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), getString(R.string.report_title)))
                }.show()
        }
    }

    private fun candidateDevice(event: InputEvent): InputDevice? {
        if (event.deviceId < 0) return null
        val device = InputDevice.getDevice(event.deviceId) ?: return null
        if (!device.supportsSource(InputDevice.SOURCE_GAMEPAD) && !device.supportsSource(InputDevice.SOURCE_JOYSTICK)) return null
        val p = settings.profile
        if (p != null) return if (p.matches(device)) device else null
        if (device.vendorId != 0x1532 && !device.name.contains("kishi", true)) return null
        settings.profile = HardwareProfile(device.descriptor, device.name, device.vendorId, device.productId)
        RuntimeState.record(getString(R.string.kishi_identified_log, device.name))
        render()
        return device
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (receiverTest && event.deviceId < 0 && event.keyCode == KeyEvent.KEYCODE_BUTTON_16) { showReceived(event); return true }
        if (candidateDevice(event) == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            live.text = getString(R.string.key_scancode, KeyEvent.keyCodeToString(event.keyCode), event.scanCode)
            if (capture == "m2") {
                candidateM2 = event.keyCode.takeIf { it in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16 && it !in observedKeys }
                if (candidateM2 == null) live.text = getString(R.string.m2_duplicate_hint)
            } else observedKeys.add(event.keyCode)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (receiverTest && event.deviceId < 0) { showReceived(event); return true }
        val device = candidateDevice(event) ?: return super.onGenericMotionEvent(event)
        if (capture in setOf("lt", "rt", "ry")) {
            for (range in device.motionRanges.filter { it.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && it.range > 0 }) {
                val value = event.getAxisValue(range.axis)
                observedMin[range.axis] = minOf(observedMin[range.axis] ?: value, value)
                observedMax[range.axis] = maxOf(observedMax[range.axis] ?: value, value)
                amplitudes[range.axis] = (observedMax.getValue(range.axis) - observedMin.getValue(range.axis)) / range.range
            }
        }
        if (SystemClock.uptimeMillis() - lastDisplayAt > 150) {
            lastDisplayAt = SystemClock.uptimeMillis()
            live.text = device.motionRanges.filter { kotlin.math.abs(event.getAxisValue(it.axis)) > .03f }
                .joinToString(" · ") { "${MotionEvent.axisToString(it.axis)} = ${"%.2f".format(event.getAxisValue(it.axis))}" }.ifBlank { getString(R.string.controls_at_rest) }
        }
        return true
    }

    private fun beginCapture(which: String) {
        capture = which; amplitudes.clear(); observedMin.clear(); observedMax.clear(); candidateM2 = null
        render()
        val instruction = when (which) {
            "lt" -> getString(R.string.capture_lt_help)
            "rt" -> getString(R.string.capture_rt_help)
            "ry" -> getString(R.string.capture_right_help)
            else -> getString(R.string.capture_m2_help)
        }
        live.text = instruction
        AlertDialog.Builder(this).setTitle(getString(R.string.identify_control_title)).setMessage(instruction).setPositiveButton(getString(R.string.understood), null).show()
    }

    private fun saveCapture() {
        val p = settings.profile ?: return
        val device = InputDevice.getDeviceIds().asSequence().mapNotNull(InputDevice::getDevice).firstOrNull { p.matches(it) } ?: return toast(getString(R.string.reconnect_kishi))
        val which = capture ?: return
        if (which == "m2") {
            settings.profile = p.copy(m2Code = candidateM2)
            RuntimeState.record(if (candidateM2 != null) getString(R.string.m2_saved_log) else getString(R.string.m2_duplicate_log))
        } else {
            val preferred = when (which) {
                "lt" -> listOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
                "rt" -> listOf(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
                else -> listOf(MotionEvent.AXIS_RZ, MotionEvent.AXIS_RY)
            }
            val excluded = listOfNotNull(15, 16, 17, 18, 22, 23, p.leftTrigger?.axis, p.rightTrigger?.axis)
            val eligible = if (which == "ry") amplitudes.filterKeys { it !in excluded }.filterValues { it >= .8f }
                else amplitudes.filterKeys { it in preferred }
            val axis = if (which == "ry") eligible.keys.singleOrNull()
                else preferred.firstOrNull { (eligible[it] ?: 0f) >= .8f }
            if (axis == null) return toast(getString(R.string.axis_ambiguous))
            val range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK) ?: device.getMotionRange(axis)
                ?: return toast(getString(R.string.axis_no_range))
            settings.profile = when (which) {
                "lt" -> p.copy(leftTrigger = TriggerSpec(axis, range.min, range.max))
                "rt" -> p.copy(rightTrigger = TriggerSpec(axis, range.min, range.max))
                else -> p.copy(rightVertical = AxisSpec(axis, range.min, range.max, range.flat))
            }
            RuntimeState.record(getString(R.string.control_identified_log, which, MotionEvent.axisToString(axis)))
        }
        capture = null; render(); RuntimeState.changed()
    }

    private fun startTrial(stage: TrialStage) {
        val launch = packageManager.getLaunchIntentForPackage(GAME_PACKAGE) ?: return toast(getString(R.string.game_not_installed))
        val task = when (stage) {
            TrialStage.PASSTHROUGH -> getString(R.string.passthrough_test_help)
            TrialStage.SKILLS -> getString(R.string.skills_test_help)
            TrialStage.VERTICAL -> getString(R.string.vertical_test_help)
            else -> return
        }
        AlertDialog.Builder(this).setTitle(stage.label(this@DiagnosticActivity)).setMessage(getString(R.string.trial_instructions, task))
            .setNegativeButton(getString(R.string.cancel), null).setPositiveButton(getString(R.string.start)) { _, _ ->
                settings.unvalidate(stage)
                RuntimeState.beginTrial(this@DiagnosticActivity, stage, settings.currentStamp())
                startActivity(launch)
            }.show()
    }

    private fun confirmTrial(stage: TrialStage) {
        AlertDialog.Builder(this).setTitle(getString(R.string.confirm_result_title))
            .setMessage(getString(R.string.confirm_result_help))
            .setNegativeButton(getString(R.string.not_yet), null).setPositiveButton(getString(R.string.everything_works)) { _, _ ->
                if (RuntimeState.trialStamp != settings.currentStamp() || RuntimeState.trialReadyToConfirm != stage) {
                    RuntimeState.trialReadyToConfirm = TrialStage.NONE
                    toast(getString(R.string.installation_changed))
                    render()
                    return@setPositiveButton
                }
                settings.validate(stage)
                RuntimeState.record(getString(R.string.trial_confirmed_log, stage.label(this@DiagnosticActivity)))
                RuntimeState.trialReadyToConfirm = TrialStage.NONE
                RuntimeState.changed(); render()
            }.show()
    }

    private fun testReceiver() {
        receiverTest = true; receivedKeys = 0; receivedMotions = 0
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_16, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
        val up = KeyEvent(now, now + 1, KeyEvent.ACTION_UP, down.keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
        live.text = getString(R.string.local_receiver_testing)
        app.bridge.send(down, applicationInfo.uid)
        app.bridge.send(up, applicationInfo.uid)
        for (value in listOf(.25f, 0f)) {
            val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
            val coords = arrayOf(MotionEvent.PointerCoords().apply {
                setAxisValue(MotionEvent.AXIS_X, value)
                setAxisValue(MotionEvent.AXIS_RZ, -value)
            })
            val motion = MotionEvent.obtain(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, 1, props, coords,
                0, 0, 1f, 1f, -1, 0, InputDevice.SOURCE_JOYSTICK, 0)
            app.bridge.send(motion, applicationInfo.uid)
            motion.recycle()
        }
    }
    private fun showReceived(event: InputEvent) {
        if (event is KeyEvent) receivedKeys++ else receivedMotions++
        live.text = getString(R.string.local_receiver_result, receivedKeys, receivedMotions)
        RuntimeState.record(if (event is KeyEvent) getString(R.string.local_key_log, KeyEvent.keyCodeToString(event.keyCode), event.action)
            else getString(R.string.local_motion_log, (event as MotionEvent).getAxisValue(MotionEvent.AXIS_X).toString(), event.getAxisValue(MotionEvent.AXIS_RZ).toString()))
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
