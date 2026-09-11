package fr.kishiswitch.guildwars.input

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import fr.kishiswitch.engine.Options
import fr.kishiswitch.engine.RemapEngine
import fr.kishiswitch.guildwars.KishiApplication
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.data.GAME_PACKAGE
import fr.kishiswitch.guildwars.data.HardwareProfile
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.ui.FloatingControls

class RemapService : AccessibilityService(), InputManager.InputDeviceListener {
    private val main = Handler(Looper.getMainLooper())
    private val app get() = application as KishiApplication
    private val settings get() = app.settings
    private val bridge get() = app.bridge
    private var profile: HardwareProfile? = null
    private var device: InputDevice? = null
    private var engine: RemapEngine? = null
    private var streaming = false
    private var uid = -1
    private var refreshing = false
    private var inputAllowed = false
    private var lastMotion: MotionEvent? = null
    private val syntheticDowns = mutableMapOf<Int, KeyEvent>()
    private val swallowedM2 = mutableSetOf<Int>()
    private val drainingReleases = mutableSetOf<Int>()
    private var trialSeenGame = false
    private var seenTrialExpiry = 0L
    private var lastProfileKey = ""
    private lateinit var floating: FloatingControls
    private val listener: () -> Unit = { main.post { refresh() } }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> main.post { refresh() } }
    private val timeout = Runnable { RuntimeState.endTrial(); refresh() }

    override fun onServiceConnected() {
        // Android may retain the previous dynamic service info even when this instance is new.
        configureInterception(false, false)
        floating = FloatingControls(this, settings)
        RuntimeState.serviceConnected = true
        RuntimeState.stopService = {
            RuntimeState.trialFailed = true
            RuntimeState.endTrial()
            stopStreaming()
            floating.hide()
            disableSelf()
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        RuntimeState.listen(listener)
        getSystemService(InputManager::class.java).registerInputDeviceListener(this, main)
        bridge.onFailure = {
            RuntimeState.trialFailed = true
            stopStreaming()
            if (RuntimeState.trial != TrialStage.NONE) RuntimeState.endTrial()
            refresh()
        }
        bridge.connect(false)
        refresh()
        RuntimeState.changed()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { refresh() }

    private fun refresh() {
        if (refreshing || !::floating.isInitialized) return
        refreshing = true
        val previousStatus = RuntimeState.status
        try {
            val windowsNow = windows
            val keyboard = windowsNow.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            val focusedWindow = windowsNow.firstOrNull { it.isFocused }
            val applicationWindow = if (focusedWindow != null)
                focusedWindow.takeIf { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            else windowsNow.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive }
            val packageName = applicationWindow?.root?.packageName?.toString()
            val unlocked = !getSystemService(KeyguardManager::class.java).isKeyguardLocked
            val game = packageName == GAME_PACKAGE && unlocked && !RuntimeState.ownUiVisible
            inputAllowed = game && !keyboard
            RuntimeState.gameForeground = game
            val newProfile = settings.profile
            val newDevice = InputDevice.getDeviceIds().asSequence().mapNotNull { InputDevice.getDevice(it) }
                .firstOrNull { newProfile?.matches(it) == true }
            val profileKey = "$newProfile|${newDevice?.id}|${settings.threshold}"
            if (profileKey != lastProfileKey) {
                stopStreaming()
                profile = newProfile; device = newDevice; lastProfileKey = profileKey
                drainingReleases.clear()
            }
            uid = runCatching { packageManager.getApplicationInfo(GAME_PACKAGE, 0).uid }.getOrDefault(-1)

            if (seenTrialExpiry != RuntimeState.trialExpiresAt) {
                stopStreaming()
                seenTrialExpiry = RuntimeState.trialExpiresAt
                trialSeenGame = false
                main.removeCallbacks(timeout)
                if (RuntimeState.trial != TrialStage.NONE) {
                    main.postDelayed(timeout, (seenTrialExpiry - SystemClock.elapsedRealtime()).coerceAtLeast(1))
                }
            }
            if (RuntimeState.trial != TrialStage.NONE) {
                if (game) trialSeenGame = true
                else if (trialSeenGame) {
                    stopStreaming()
                    RuntimeState.endTrial()
                }
            }

            val requested = requestedOptions()
            val isTrial = RuntimeState.trial != TrialStage.NONE
            val mayStream = game && !keyboard && bridge.ready && uid >= 10_000 && profile?.ready == true && device != null &&
                (isTrial || requested.skills || requested.rightVertical)
            if (!mayStream) {
                // Settings toggles drain at a neutral boundary; focus/permission loss stops immediately.
                if (game && !keyboard && bridge.ready && device != null && streaming && !isTrial && !requested.skills && !requested.rightVertical) {
                    engine?.requestOptions(requested)
                    val analogNeutral = lastMotion?.let { motion -> device?.let { MotionAdapter.isNeutral(motion, it) } } ?: true
                    if (engine?.canStop == true && analogNeutral) stopStreaming()
                } else stopStreaming()
            } else if (!streaming) {
                val p = profile!!
                engine = RemapEngine(p.rightVertical, p.leftTrigger, p.rightTrigger, settings.threshold, requested)
                streaming = true
            } else engine?.requestOptions(requested)

            // Native keys stay outside the accessibility filter for transport/vertical trials.
            val remappingKeys = streaming && (engine?.effective?.skills == true || requested.skills)
            val m2Needed = inputAllowed && bridge.ready && device != null && profile?.m2Code != null &&
                RuntimeState.trial == TrialStage.NONE && settings.validated(TrialStage.SKILLS)
            configureInterception(streaming, remappingKeys || m2Needed)

            RuntimeState.status = when {
                !bridge.ready -> bridge.state
                profile == null -> "Identifiez votre Kishi dans le diagnostic."
                device == null -> "Branchez la Kishi."
                profile?.ready != true -> "Identifiez les deux gâchettes."
                isTrial && game -> "Essai temporaire · ${RuntimeState.trial.label}"
                !settings.validated(TrialStage.PASSTHROUGH) -> "Transmission dans Guild Wars à vérifier."
                !game -> "En attente de Guild Wars."
                keyboard -> "Saisie en cours · commandes natives"
                engine?.hasPendingOptions == true -> "Relâchez les commandes pour appliquer le réglage."
                streaming -> "Inversions actives dans Guild Wars"
                else -> "Commandes natives · inversions désactivées"
            }
            floating.update(game && !keyboard, streaming, engine?.effective)
        } catch (e: RuntimeException) {
            RuntimeState.record("Fenêtre/interception : ${e.javaClass.simpleName}")
            stopStreaming()
            floating.hide()
            RuntimeState.status = "Fenêtre indisponible · commandes natives"
        } finally {
            refreshing = false
            if (RuntimeState.status != previousStatus) RuntimeState.changed()
        }
    }

    private fun requestedOptions(): Options = when (RuntimeState.trial) {
        TrialStage.PASSTHROUGH -> Options(false, false)
        TrialStage.SKILLS -> Options(true, false)
        TrialStage.VERTICAL -> Options(false, true)
        TrialStage.NONE -> settings.options.let {
            Options(it.skills && settings.validated(TrialStage.PASSTHROUGH) && settings.validated(TrialStage.SKILLS),
                it.rightVertical && profile?.rightVertical != null && settings.validated(TrialStage.PASSTHROUGH) && settings.validated(TrialStage.VERTICAL))
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val physical = device?.id == event.deviceId
        if (!physical) return false
        if (event.keyCode in drainingReleases) {
            if (event.action == KeyEvent.ACTION_UP) drainingReleases.remove(event.keyCode)
            return true
        }
        if (inputAllowed && profile?.m2Code == event.keyCode && RuntimeState.trial == TrialStage.NONE &&
            settings.validated(TrialStage.SKILLS)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                swallowedM2.add(event.keyCode)
                settings.options = settings.options.copy(skills = !settings.options.skills)
                refresh()
            }
            if (event.action == KeyEvent.ACTION_UP) swallowedM2.remove(event.keyCode)
            return true
        }
        if (event.keyCode in swallowedM2) { if (event.action == KeyEvent.ACTION_UP) swallowedM2.remove(event.keyCode); return true }
        val current = engine ?: return false
        if (!streaming || !bridge.ready || !RuntimeState.gameForeground) return false
        val previousOptions = current.effective
        val route = when (event.action) {
            KeyEvent.ACTION_DOWN -> current.keyDown(event.keyCode)
            KeyEvent.ACTION_UP -> current.keyUp(event.keyCode)
            else -> return false
        }
        if (current.effective != previousOptions || current.hasPendingOptions || current.canStop) main.post { refresh() }
        if (!route.injected) return false
        val output = KeyEvent(event.downTime, event.eventTime, event.action, route.logical, event.repeatCount,
            event.metaState, event.deviceId, 0, event.flags, event.source)
        val physicalCode = event.keyCode
        val trialExpiry = RuntimeState.trialExpiresAt
        val accepted = bridge.send(output, uid) { success ->
            if (success && trialExpiry != 0L && RuntimeState.trialExpiresAt == trialExpiry) {
                RuntimeState.trialInjectedKeys++
                if (output.action == KeyEvent.ACTION_DOWN) RuntimeState.trialEvents++
            }
            if (success && output.action == KeyEvent.ACTION_UP && syntheticDowns[physicalCode]?.downTime == output.downTime) {
                syntheticDowns.remove(physicalCode)
            }
        }
        if (!accepted) { stopStreaming(); return false }
        if (event.action == KeyEvent.ACTION_DOWN) syntheticDowns[event.keyCode] = output
        if (current.hasPendingOptions) main.post { refresh() }
        return true
    }

    override fun onMotionEvent(event: MotionEvent) {
        if (!streaming || !bridge.ready) return
        // Source subscriptions cover all joysticks. Foreign devices must be forwarded intact.
        val selected = device?.id == event.deviceId
        val trialExpiry = RuntimeState.trialExpiresAt
        if (selected && trialExpiry != 0L) {
            if (RuntimeState.trialCapturedMotions++ == 0)
                RuntimeState.record("Premier mouvement capturé : périphérique=${event.deviceId}, source=${event.source}, action=${event.actionMasked}")
        }
        val current = engine
        val previousOptions = current?.effective
        val output = if (selected && current != null) MotionAdapter.transform(event, current) else MotionEvent.obtain(event)
        if (selected) { lastMotion?.recycle(); lastMotion = MotionEvent.obtain(event) }
        val accepted = bridge.send(output, uid) { success ->
            if (success && selected && trialExpiry != 0L && RuntimeState.trialExpiresAt == trialExpiry) {
                RuntimeState.trialInjectedMotions++
                if (RuntimeState.trial in setOf(TrialStage.PASSTHROUGH, TrialStage.VERTICAL)) RuntimeState.trialEvents++
            }
        }
        output.recycle()
        if (!accepted) { RuntimeState.trialFailed = true; stopStreaming() }
        else if (selected && current != null && (current.effective != previousOptions || current.hasPendingOptions || (!requestedOptions().skills && !requestedOptions().rightVertical && RuntimeState.trial == TrialStage.NONE))) {
            main.post { refresh() }
        }
    }

    private fun configureInterception(motion: Boolean, keys: Boolean) {
        val info = serviceInfo ?: return
        if (InterceptionConfig.apply(info, motion, keys)) {
            serviceInfo = info
            RuntimeState.record("Interception : mouvements=$motion, boutons=$keys")
        }
        RuntimeState.capturingMotion = motion
        RuntimeState.filteringKeys = keys
    }

    private fun stopStreaming() {
        // Always restore native delivery, even with no in-memory engine after a restart.
        configureInterception(false, false)
        if (!streaming) return
        streaming = false
        bridge.cancelPending()
        // Cleanup retains the previous target UID, so it cannot type into another app.
        syntheticDowns.forEach { (physical, down) ->
            drainingReleases.add(physical)
            val up = KeyEvent(down.downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, down.keyCode,
                0, down.metaState, down.deviceId, 0, down.flags, down.source)
            bridge.send(up, uid, critical = false)
        }
        syntheticDowns.clear()
        val last = lastMotion
        val oldDevice = device
        if (last != null && oldDevice != null) {
            val neutral = MotionAdapter.neutral(last, oldDevice)
            bridge.send(neutral, uid, critical = false)
            neutral.recycle()
        }
        lastMotion?.recycle(); lastMotion = null
        engine?.reset(); engine = null
    }

    override fun onInterrupt() {
        RuntimeState.trialFailed = true
        RuntimeState.endTrial()
        stopStreaming()
    }
    override fun onInputDeviceAdded(deviceId: Int) { refresh() }
    override fun onInputDeviceChanged(deviceId: Int) { refresh() }
    override fun onInputDeviceRemoved(deviceId: Int) { refresh() }
    override fun onDestroy() {
        stopStreaming(); main.removeCallbacksAndMessages(null)
        if (::floating.isInitialized) floating.hide()
        settings.prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        getSystemService(InputManager::class.java).unregisterInputDeviceListener(this)
        RuntimeState.unlisten(listener)
        bridge.onFailure = null
        RuntimeState.serviceConnected = false
        RuntimeState.stopService = null
        RuntimeState.endTrial()
        RuntimeState.changed()
        super.onDestroy()
    }
}
