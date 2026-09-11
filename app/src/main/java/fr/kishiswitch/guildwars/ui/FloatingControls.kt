package fr.kishiswitch.guildwars.ui

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import fr.kishiswitch.engine.Options
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.data.Settings
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.ui.Ui.action
import fr.kishiswitch.guildwars.ui.Ui.paragraph
import fr.kishiswitch.guildwars.ui.Ui.toggle

class FloatingControls(private val service: AccessibilityService, private val settings: Settings) {
    private val manager = service.getSystemService(WindowManager::class.java)
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var stateKey = ""

    fun update(show: Boolean, streaming: Boolean, effective: Options?) {
        if (!show) { hide(); return }
        val key = "$streaming|$effective|${settings.options}|${RuntimeState.trial}|${RuntimeState.status}"
        if (root != null && stateKey == key) return
        stateKey = key
        rebuild()
    }

    private fun rebuild() {
        root?.let { runCatching { manager.removeView(it) } }
        val column = Ui.column(service).apply {
            setPadding(Ui.dp(service, 8), Ui.dp(service, 6), Ui.dp(service, 8), Ui.dp(service, 6))
            background = Ui.panel(radius = Ui.dp(service, 18).toFloat())
            elevation = Ui.dp(service, 8).toFloat()
        }
        root = column
        val bubble = Ui.text(service, if (expanded) "GW  ·  Réglages" else "GW", 15f, Ui.accent).apply {
            gravity = Gravity.CENTER; minHeight = Ui.dp(service, 44); minWidth = Ui.dp(service, 44)
            contentDescription = "Afficher ou masquer les inversions Guild Wars"
            isClickable = true
        }
        column.addView(bubble, LinearLayout.LayoutParams(if (expanded) -1 else Ui.dp(service, 44), Ui.dp(service, 44)))
        if (expanded) {
            if (RuntimeState.trial != TrialStage.NONE) {
                column.paragraph("ESSAI · ${RuntimeState.trial.label}", Ui.accent)
                column.action("Arrêter l’essai") { RuntimeState.endTrial(); expanded = false; rebuild() }
            } else {
                column.toggle("Compétences", settings.options.skills, settings.validated(TrialStage.SKILLS)) {
                    settings.options = settings.options.copy(skills = it); collapse()
                }
                column.toggle("Vertical stick droit", settings.options.rightVertical, settings.validated(TrialStage.VERTICAL)) {
                    settings.options = settings.options.copy(rightVertical = it); collapse()
                }
                column.paragraph(RuntimeState.status)
                if (!settings.validated(TrialStage.SKILLS)) column.paragraph("Terminez les essais dans Kishi Switch.")
            }
        }
        val width = if (expanded) Ui.dp(service, 272) else Ui.dp(service, 60)
        val bounds = manager.currentWindowMetrics.bounds
        val lp = WindowManager.LayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = ((bounds.width() - width).coerceAtLeast(0) * settings.overlayX).toInt()
            y = ((bounds.height() - Ui.dp(service, if (expanded) 280 else 60)).coerceAtLeast(0) * settings.overlayY).toInt()
        }
        params = lp
        installDrag(bubble, lp)
        runCatching { manager.addView(column, lp) }.onFailure { root = null }
    }

    private fun collapse() { expanded = false; RuntimeState.changed(); rebuild() }
    private fun installDrag(view: TextView, lp: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f; var oldX = 0; var oldY = 0; var dragged = false
        view.setOnClickListener { expanded = !expanded; rebuild() }
        view.setOnTouchListener { _, event ->
            val bounds = manager.currentWindowMetrics.bounds
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; oldX = lp.x; oldY = lp.y; dragged = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > ViewConfiguration.get(service).scaledTouchSlop) dragged = true
                    if (dragged) {
                        lp.x = (oldX + dx.toInt()).coerceIn(0, (bounds.width() - lp.width).coerceAtLeast(0))
                        lp.y = (oldY + dy.toInt()).coerceIn(0, (bounds.height() - (root?.height ?: 60)).coerceAtLeast(0))
                        root?.let { runCatching { manager.updateViewLayout(it, lp) } }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        settings.overlayX = lp.x.toFloat() / (bounds.width() - lp.width).coerceAtLeast(1)
                        settings.overlayY = lp.y.toFloat() / (bounds.height() - (root?.height ?: 60)).coerceAtLeast(1)
                    } else view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    fun hide() {
        root?.let { runCatching { manager.removeView(it) } }
        root = null; params = null; expanded = false; stateKey = ""
    }
}
