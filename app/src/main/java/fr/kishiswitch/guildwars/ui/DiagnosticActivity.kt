package fr.kishiswitch.guildwars.ui

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
        RuntimeState.endTrial()
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
        screen.paragraph("MISE EN PLACE", Ui.accent)
        screen.heading("Vérifier la Kishi", true)
        screen.paragraph("Cette étape se fait une fois pour votre installation. Les essais dans le jeu durent au maximum trois minutes et s’arrêtent en quittant Guild Wars.")
        screen.action("Retour aux inversions") { finish() }
        screen.action("Arrêter entièrement le service", RuntimeState.serviceConnected) { RuntimeState.stopService?.invoke() }
        screen.heading("1 · Reconnaître la manette")
        screen.paragraph(p?.name ?: "Branchez votre Kishi puis appuyez sur un de ses boutons.")
        screen.paragraph(if (p == null) "Aucune manette identifiée." else "Identifiant matériel enregistré · ${p.vendor}:${p.product}")
        live = screen.paragraph("Les commandes reçues s’afficheront ici.", Ui.accent)
        screen.heading("2 · Identifier les commandes")
        screen.action("Identifier LT · gâchette gauche", p != null) { beginCapture("lt") }
        screen.action("Identifier RT · gâchette droite", p != null) { beginCapture("rt") }
        screen.action("Identifier le vertical du stick droit", p != null) { beginCapture("ry") }
        screen.paragraph("M2 : utilisez l’overlay pour cette version du relais HID.")
        screen.paragraph("LT : ${p?.leftTrigger?.axis?.let(MotionEvent::axisToString) ?: "à identifier"}\nRT : ${p?.rightTrigger?.axis?.let(MotionEvent::axisToString) ?: "à identifier"}\nVertical droit : ${p?.rightVertical?.id?.let(MotionEvent::axisToString) ?: "à identifier"}\nM2 : ${p?.m2Code?.let(KeyEvent::keyCodeToString) ?: "overlay utilisé"}")
        screen.action("Enregistrer la commande observée", capture != null) { saveCapture() }
        screen.action("Annuler l’identification", capture != null) { capture = null; render() }
        screen.heading("Seuil des gâchettes")
        screen.paragraph("Réglage technique pour faire correspondre la bascule au maintien de LT/RT dans Guild Wars. Le modifier remet les essais à zéro.")
        val thresholdLabel = screen.paragraph("Activation à ${(settings.threshold * 100).toInt()} % de la course")
        val slider = SeekBar(this).apply {
            min = 5; max = 95; progress = (settings.threshold * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { thresholdLabel.text = "Activation à $progress % de la course" }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) { settings.threshold = (seekBar?.progress ?: 50) / 100f; RuntimeState.changed() }
            })
        }
        screen.addView(slider)
        screen.heading("3 · Vérifier la connexion")
        screen.paragraph(app.bridge.state)
        screen.action("Autoriser / reconnecter Shizuku") { app.bridge.connect(true) }
        screen.paragraph("Le relais HID est vérifié directement dans Guild Wars. Relâchez les commandes avant de commencer chaque essai.")
        screen.heading("4 · Essais dans Guild Wars")
        for (stage in TrialStage.entries.filter { it != TrialStage.NONE }) {
            val valid = settings.validated(stage)
            val available = app.bridge.ready && RuntimeState.serviceConnected && p?.ready == true &&
                (stage == TrialStage.PASSTHROUGH || settings.validated(TrialStage.PASSTHROUGH)) &&
                (stage != TrialStage.VERTICAL || p?.rightVertical != null)
            screen.action("${stage.label} · ${if (valid) "validé" else "essayer"}", available) { startTrial(stage) }
        }
        if (RuntimeState.trial != TrialStage.NONE) screen.action("Arrêter l’essai en attente") { RuntimeState.endTrial() }
        val pending = RuntimeState.trialReadyToConfirm
        if (pending != TrialStage.NONE) {
            screen.paragraph("Retour de l’essai : ${pending.label}. ${RuntimeState.trialEvents} événement(s) transmis au relais HID. Confirmez le résultat observé dans le jeu.", Ui.accent)
            screen.action("Confirmer que l’essai fonctionne dans le jeu") { confirmTrial(pending) }
            screen.action("L’essai ne fonctionne pas") {
                settings.unvalidate(pending); RuntimeState.trialReadyToConfirm = TrialStage.NONE
                RuntimeState.record("Échec constaté dans le jeu : ${pending.label}")
                RuntimeState.changed()
            }
        }
        screen.paragraph("L’inversion des compétences et celle du stick droit deviennent actives séparément après confirmation. Les deux préférences sont activées par défaut.")
        screen.heading("Compte rendu local")
        screen.action("Afficher le compte rendu") {
            val report = "Kishi Switch ${fr.kishiswitch.guildwars.BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE}\n" +
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                "${p?.name ?: "Manette absente"}\n${app.bridge.state}\n" +
                "Interception mouvements=${RuntimeState.capturingMotion}, boutons=${RuntimeState.filteringKeys}\n" +
                "Transport HID · relais actif=${RuntimeState.relayActive}\n" +
                "LT=${p?.leftTrigger?.axis}, RT=${p?.rightTrigger?.axis}, vertical droit=${p?.rightVertical?.id}\n" +
                TrialStage.entries.filter { it != TrialStage.NONE }.joinToString("\n") { "${it.label} : ${if (settings.validated(it)) "confirmé par l’utilisateur" else "non validé"}" } +
                "\n\n" + RuntimeState.report()
            AlertDialog.Builder(this).setTitle("Diagnostic").setMessage(report)
                .setPositiveButton("Fermer", null).setNeutralButton("Partager…") { _, _ ->
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), "Compte rendu"))
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
        RuntimeState.record("Kishi identifiée : ${device.name}")
        render()
        return device
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (receiverTest && event.deviceId < 0 && event.keyCode == KeyEvent.KEYCODE_BUTTON_16) { showReceived(event); return true }
        if (candidateDevice(event) == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            live.text = "${KeyEvent.keyCodeToString(event.keyCode)} · code physique ${event.scanCode}"
            if (capture == "m2") {
                candidateM2 = event.keyCode.takeIf { it in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16 && it !in observedKeys }
                if (candidateM2 == null) live.text = "M2 reproduit une touche standard ou déjà observée. L’overlay servira d’interrupteur."
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
                .joinToString(" · ") { "${MotionEvent.axisToString(it.axis)} = ${"%.2f".format(event.getAxisValue(it.axis))}" }.ifBlank { "Commandes au repos" }
        }
        return true
    }

    private fun beginCapture(which: String) {
        capture = which; amplitudes.clear(); observedMin.clear(); observedMax.clear(); candidateM2 = null
        render()
        val instruction = when (which) {
            "lt" -> "Sans toucher aux sticks, enfoncez LT complètement puis relâchez. Touchez ensuite « Enregistrer »."
            "rt" -> "Sans toucher aux sticks, enfoncez RT complètement puis relâchez. Touchez ensuite « Enregistrer »."
            "ry" -> "Sans toucher aux autres commandes, poussez le stick DROIT en haut puis en bas, puis laissez-le revenir au centre. Touchez ensuite « Enregistrer »."
            else -> "Appuyez une fois sur M2 puis touchez « Enregistrer ». Un bouton qui duplique une touche standard ne sera pas utilisé."
        }
        live.text = instruction
        AlertDialog.Builder(this).setTitle("Identifier la commande").setMessage(instruction).setPositiveButton("Compris", null).show()
    }

    private fun saveCapture() {
        val p = settings.profile ?: return
        val device = InputDevice.getDeviceIds().asSequence().mapNotNull(InputDevice::getDevice).firstOrNull { p.matches(it) } ?: return toast("Rebranchez la Kishi.")
        val which = capture ?: return
        if (which == "m2") {
            settings.profile = p.copy(m2Code = candidateM2)
            RuntimeState.record(if (candidateM2 != null) "M2 distinct enregistré" else "M2 non distinct : overlay retenu")
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
            if (axis == null) return toast("Mouvement insuffisant ou axe ambigu. Refaites le geste complet.")
            val range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK) ?: device.getMotionRange(axis)
                ?: return toast("Cet axe ne fournit pas de plage analogique.")
            settings.profile = when (which) {
                "lt" -> p.copy(leftTrigger = TriggerSpec(axis, range.min, range.max))
                "rt" -> p.copy(rightTrigger = TriggerSpec(axis, range.min, range.max))
                else -> p.copy(rightVertical = AxisSpec(axis, range.min, range.max, range.flat))
            }
            RuntimeState.record("Commande $which identifiée : ${MotionEvent.axisToString(axis)}")
        }
        capture = null; render(); RuntimeState.changed()
    }

    private fun startTrial(stage: TrialStage) {
        val launch = packageManager.getLaunchIntentForPackage(GAME_PACKAGE) ?: return toast("Guild Wars Reforged n’est pas installé.")
        val task = when (stage) {
            TrialStage.PASSTHROUGH -> "Vérifiez le déplacement, les deux sticks, les gâchettes, les boutons et le retour au repos. Rien ne doit être inversé."
            TrialStage.SKILLS -> "Testez LT+X/A/Y/B puis RT+X/A/Y/B. Les positions des compétences doivent correspondre à celles de la manette. Vérifiez aussi les boutons seuls et les relâchements."
            TrialStage.VERTICAL -> "Vérifiez que haut/bas du stick droit est inversé, à petite et grande amplitude. Le stick gauche et l’horizontal droit doivent fonctionner normalement."
            else -> return
        }
        AlertDialog.Builder(this).setTitle(stage.label).setMessage("$task\n\nRelâchez les commandes pendant la connexion. Revenez ensuite ici pour confirmer le résultat. L’essai s’arrête automatiquement après trois minutes ou en quittant le jeu.")
            .setNegativeButton("Annuler", null).setPositiveButton("Commencer") { _, _ ->
                settings.unvalidate(stage)
                RuntimeState.beginTrial(stage, settings.currentStamp())
                startActivity(launch)
            }.show()
    }

    private fun confirmTrial(stage: TrialStage) {
        AlertDialog.Builder(this).setTitle("Confirmer le résultat observé")
            .setMessage("Confirmez uniquement si vous avez effectué toutes les vérifications de cet essai dans Guild Wars, sans commandes perdues, doublées ou bloquées. Le succès technique d’une injection ne suffit pas.")
            .setNegativeButton("Pas encore", null).setPositiveButton("Tout fonctionne") { _, _ ->
                if (RuntimeState.trialStamp != settings.currentStamp() || RuntimeState.trialReadyToConfirm != stage) {
                    RuntimeState.trialReadyToConfirm = TrialStage.NONE
                    toast("L’installation a changé. Refaites cet essai.")
                    render()
                    return@setPositiveButton
                }
                settings.validate(stage)
                RuntimeState.record("Validation dans Guild Wars confirmée par l’utilisateur : ${stage.label}")
                RuntimeState.trialReadyToConfirm = TrialStage.NONE
                RuntimeState.changed(); render()
            }.show()
    }

    private fun testReceiver() {
        receiverTest = true; receivedKeys = 0; receivedMotions = 0
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_16, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
        val up = KeyEvent(now, now + 1, KeyEvent.ACTION_UP, down.keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
        live.text = "Test des boutons et mouvements dans notre récepteur…"
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
        live.text = "Réception locale : $receivedKeys événements de boutons, $receivedMotions mouvements.\nLa réception par Guild Wars reste à vérifier."
        RuntimeState.record(if (event is KeyEvent) "Récepteur local : ${KeyEvent.keyCodeToString(event.keyCode)}, action ${event.action}"
            else "Récepteur local : mouvement, X=${(event as MotionEvent).getAxisValue(MotionEvent.AXIS_X)}, RZ=${event.getAxisValue(MotionEvent.AXIS_RZ)}")
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
