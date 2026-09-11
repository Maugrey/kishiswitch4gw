package fr.kishiswitch.guildwars.ui

import fr.kishiswitch.guildwars.R
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.ScrollView
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
        super.onResume(); RuntimeState.ownUiVisible = true; RuntimeState.endTrial(this@MainActivity)
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
        screen.heading(getString(R.string.main_title), true)
        screen.paragraph(getString(R.string.main_intro))
        screen.heading(getString(R.string.status_title))
        screen.paragraph(app.bridge.state)
        screen.paragraph(if (RuntimeState.serviceConnected) RuntimeState.status else getString(R.string.accessibility_disabled))
        screen.action(getString(R.string.stop_service), RuntimeState.serviceConnected) { RuntimeState.stopService?.invoke() }
        screen.heading(getString(R.string.inversions_title))
        screen.toggle(getString(R.string.skills_mapping), settings.options.skills) { settings.options = settings.options.copy(skills = it); RuntimeState.changed() }
        screen.paragraph(getString(R.string.skills_help) + if (settings.validated(TrialStage.SKILLS)) getString(R.string.validated_installation) else getString(R.string.skills_unvalidated))
        screen.toggle(getString(R.string.right_vertical_title), settings.options.rightVertical) { settings.options = settings.options.copy(rightVertical = it); RuntimeState.changed() }
        screen.paragraph(getString(R.string.vertical_help) + if (settings.validated(TrialStage.VERTICAL)) getString(R.string.validated_installation) else getString(R.string.vertical_unvalidated))
        screen.action(getString(R.string.open_game)) { launchGame() }
        screen.heading(getString(R.string.setup_title))
        screen.action(getString(R.string.setup_shizuku_install)) {
            val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            runCatching { startActivity(launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }
                .onFailure { Toast.makeText(this, getString(R.string.install_shizuku_hint), Toast.LENGTH_LONG).show() }
        }
        screen.action(getString(R.string.setup_shizuku_authorize)) { app.bridge.connect(true) }
        screen.action(getString(R.string.setup_accessibility)) {
            val intent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
                .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(this, RemapService::class.java))
            runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        screen.action(getString(R.string.setup_diagnostics)) { startActivity(Intent(this, DiagnosticActivity::class.java)) }
        screen.paragraph(getString(R.string.setup_restart_help))
        screen.action(getString(R.string.help_button)) {
            AlertDialog.Builder(this).setTitle(getString(R.string.usage_title))
                .setMessage(getString(R.string.usage_help))
                .setPositiveButton(getString(R.string.close), null).show()
        }
        screen.action(getString(R.string.language_button)) {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure {
                Toast.makeText(this, getString(R.string.language_settings_unavailable), Toast.LENGTH_LONG).show()
            }
        }
        screen.action(getString(R.string.licenses_title)) { showLegalIndex() }
        screen.paragraph(getString(R.string.version_license, BuildConfig.VERSION_NAME), Ui.muted)
    }

    private fun showLegalIndex() {
        val documents = listOf(
            getString(R.string.attribution_document) to "legal/NOTICE",
            getString(R.string.license_document) to "legal/LICENSE",
            getString(R.string.third_party_document) to "legal/THIRD_PARTY_NOTICES.md",
            "Shizuku API · MIT" to "legal/licenses/Shizuku-API-MIT.txt",
            "Apache 2.0" to "legal/licenses/Apache-2.0.txt",
            "Kotlin · GWT" to "legal/licenses/Kotlin-gwt_license.txt",
            "Kotlin · Guava" to "legal/licenses/Kotlin-guava_license.txt",
            "Kotlin · Boost" to "legal/licenses/Kotlin-boost_LICENSE.txt",
            "Kotlin · ThreeTenBP" to "legal/licenses/Kotlin-threetenbp_license.txt"
        )
        AlertDialog.Builder(this).setTitle(getString(R.string.licenses_title))
            .setItems(documents.map { it.first }.toTypedArray()) { _, index ->
                val (title, path) = documents[index]
                val content = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = Ui.text(this, content, 14f).apply {
                    setTextIsSelectable(true)
                    Linkify.addLinks(this, Linkify.WEB_URLS)
                    movementMethod = LinkMovementMethod.getInstance()
                    val padding = Ui.dp(context, 20)
                    setPadding(padding, padding, padding, padding)
                }
                val scroll = ScrollView(this).apply { addView(text) }
                AlertDialog.Builder(this).setTitle(title).setView(scroll)
                    .setPositiveButton(getString(R.string.close), null).show()
            }
            .setNegativeButton(getString(R.string.close), null).show()
    }

    private fun launchGame() {
        val launch = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        if (launch == null) Toast.makeText(this, getString(R.string.game_not_installed_device), Toast.LENGTH_LONG).show()
        else startActivity(launch)
    }
}
