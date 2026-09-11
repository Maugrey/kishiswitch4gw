package fr.kishiswitch.guildwars

import android.os.SystemClock
import fr.kishiswitch.guildwars.data.TrialStage
import java.util.concurrent.CopyOnWriteArraySet

/** Process-local status only: diagnostic sessions never silently resume after process death. */
object RuntimeState {
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    var serviceConnected = false
    var ownUiVisible = false
    var stopService: (() -> Unit)? = null
    var capturingMotion = false
    var filteringKeys = false
    var relayActive = false
    var status = "Activez le service d’accessibilité."
    var trial: TrialStage = TrialStage.NONE
        private set
    var trialExpiresAt = 0L
        private set
    var trialEvents = 0
    var trialCapturedMotions = 0
    var trialInjectedMotions = 0
    var trialInjectedKeys = 0
    var trialFailed = false
    var trialReadyToConfirm: TrialStage = TrialStage.NONE
    var trialStamp: String = ""
        private set
    var gameForeground = false
    var diagnosticVisible = false
    private val log = ArrayDeque<String>()

    fun listen(listener: () -> Unit) { listeners.add(listener) }
    fun unlisten(listener: () -> Unit) { listeners.remove(listener) }
    fun changed() { listeners.forEach { it() } }
    fun record(message: String) {
        synchronized(log) {
            if (log.size >= 120) log.removeFirst()
            log.addLast("${SystemClock.elapsedRealtime() / 1000}s · $message")
        }
    }
    fun report(): String = synchronized(log) { log.joinToString("\n") }
    fun beginTrial(stage: TrialStage, stamp: String) {
        trialStamp = stamp
        trial = stage; trialExpiresAt = SystemClock.elapsedRealtime() + 180_000
        trialEvents = 0; trialCapturedMotions = 0; trialInjectedMotions = 0; trialInjectedKeys = 0
        trialFailed = false; trialReadyToConfirm = TrialStage.NONE
        record("Essai démarré : ${stage.label}"); changed()
    }
    fun endTrial() {
        if (trial == TrialStage.NONE) return
        record("Fin ${trial.label} : trames HID=$trialInjectedMotions, changements de boutons=$trialInjectedKeys, échec technique=$trialFailed")
        if (trial != TrialStage.NONE && trialEvents > 0 && !trialFailed) trialReadyToConfirm = trial
        trial = TrialStage.NONE; trialExpiresAt = 0
        changed()
    }
}
