package fr.kishiswitch.guildwars.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import fr.kishiswitch.guildwars.BuildConfig
import fr.kishiswitch.guildwars.KishiApplication
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.data.GAME_PACKAGE
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.input.RemapService
import fr.kishiswitch.guildwars.ui.Ui.action
import fr.kishiswitch.guildwars.ui.Ui.heading
import fr.kishiswitch.guildwars.ui.Ui.paragraph
import fr.kishiswitch.guildwars.ui.Ui.toggle

class MainActivity : Activity() {
    private val app get() = application as KishiApplication
    private val listener: () -> Unit = { runOnUiThread { render() } }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onResume() {
        super.onResume(); RuntimeState.ownUiVisible = true; RuntimeState.endTrial()
        RuntimeState.listen(listener); app.bridge.connect(false); RuntimeState.changed(); render()
    }
    override fun onPause() {
        RuntimeState.unlisten(listener); RuntimeState.ownUiVisible = false; RuntimeState.changed(); super.onPause()
    }

    private fun render() {
        if (isFinishing || isDestroyed) return
        val settings = app.settings
        val screen = Ui.screen(this)
        screen.paragraph("GUILD WARS  /  KISHI V2 PRO", Ui.accent)
        screen.heading("Vos commandes,\nà la bonne place.", true)
        screen.paragraph("Deux inversions. Seulement dans Guild Wars. Votre clavier reste le même.")
        screen.heading("État")
        screen.paragraph(app.bridge.state)
        screen.paragraph(if (RuntimeState.serviceConnected) RuntimeState.status else "Service d’accessibilité à activer.")
        screen.action("Arrêter entièrement le service", RuntimeState.serviceConnected) { RuntimeState.stopService?.invoke() }
        screen.heading("Vos inversions")
        screen.toggle("Compétences · A ↔ X, B ↔ Y", settings.options.skills) { settings.options = settings.options.copy(skills = it); RuntimeState.changed() }
        screen.paragraph("Pendant le maintien de LT ou RT uniquement. " + if (settings.validated(TrialStage.SKILLS)) "Validé sur votre installation." else "Préférence mémorisée ; un essai dans le jeu est requis.")
        screen.toggle("Vertical du stick droit", settings.options.rightVertical) { settings.options = settings.options.copy(rightVertical = it); RuntimeState.changed() }
        screen.paragraph("Inverse haut et bas, sans changer le stick gauche. " + if (settings.validated(TrialStage.VERTICAL)) "Validé sur votre installation." else "Préférence mémorisée ; disponible après identification et essai.")
        screen.action("Ouvrir Guild Wars") { launchGame() }
        screen.heading("Mise en place")
        screen.action("1 · Ouvrir ou installer Shizuku") {
            val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            runCatching { startActivity(launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }
                .onFailure { Toast.makeText(this, "Installez Shizuku depuis shizuku.rikka.app", Toast.LENGTH_LONG).show() }
        }
        screen.action("2 · Autoriser la connexion Shizuku") { app.bridge.connect(true) }
        screen.action("3 · Activer le service de manette") {
            val intent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
                .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(this, RemapService::class.java))
            runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        screen.action("4 · Identifier la Kishi et faire les essais") { startActivity(Intent(this, DiagnosticActivity::class.java)) }
        screen.paragraph("Après un redémarrage du téléphone, relancez Shizuku. Si Android bloque l’accessibilité, ouvrez les informations de Kishi Switch puis le menu ⋮ et « Autoriser les paramètres restreints », si cette option apparaît.")
        screen.action("Aide et désactivation") {
            AlertDialog.Builder(this).setTitle("Utilisation")
                .setMessage("Dans Guild Wars, touchez la petite pastille GW pour ouvrir les deux interrupteurs. Déplacez-la en la faisant glisser. Les réglages attendent le relâchement des commandes.\n\nPour tout arrêter, désactivez les deux inversions ou le service d’accessibilité. Votre clavier n’est jamais remplacé.\n\nL’app n’émet pas de notifications récurrentes. Android et Shizuku peuvent afficher leurs propres indications.\n\nLes mises à jour du jeu ou d’OxygenOS nécessitent de refaire les essais.")
                .setPositiveButton("Fermer", null).show()
        }
        screen.paragraph("Version ${BuildConfig.VERSION_NAME} · usage personnel", Ui.muted)
    }

    private fun launchGame() {
        val launch = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        if (launch == null) Toast.makeText(this, "Guild Wars Reforged n’est pas installé sur cet appareil.", Toast.LENGTH_LONG).show()
        else startActivity(launch)
    }
}
