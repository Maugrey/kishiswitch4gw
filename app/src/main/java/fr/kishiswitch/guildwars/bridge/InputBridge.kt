package fr.kishiswitch.guildwars.bridge

import fr.kishiswitch.guildwars.R
import fr.kishiswitch.guildwars.AppLanguage
import android.content.Context
import android.os.Binder
import android.os.Process
import android.os.IBinder
import org.json.JSONObject
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import fr.kishiswitch.guildwars.data.GAME_PACKAGE

/** Instantiated by Shizuku, never registered as an exported Android service. */
@Suppress("unused")
class InputBridge(private val context: Context) : IInputBridge.Stub() {
    private val ownerUid = context.applicationInfo.uid
    private val gameUid = runCatching { context.packageManager.getApplicationInfo(GAME_PACKAGE, 0).uid }.getOrDefault(-1)
    private val managerClass = Class.forName("android.hardware.input.InputManagerGlobal")
    private val manager = managerClass.getDeclaredMethod("getInstance").invoke(null)
    private val inject = managerClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    private var lastError = ""
    private var relay: HidRelay? = null
    override fun getLastError(): String { enforceOwner(); return lastError }

    private fun enforceOwner() {
        check(ownerUid >= 10_000 && Binder.getCallingUid() == ownerUid) { context.getString(R.string.caller_unauthorized) }
    }
    override fun ping(): Boolean { enforceOwner(); return Process.myUid() == 2000 || Process.myUid() == 0 }
    override fun injectKey(event: KeyEvent, targetUid: Int): Boolean = deliver(event, targetUid)
    override fun injectMotion(event: MotionEvent, targetUid: Int): Boolean = deliver(event, targetUid)

    @Synchronized override fun startRelay(configuration: String, lifetime: IBinder, targetUid: Int): Boolean {
        enforceOwner()
        check(gameUid >= 10_000 && targetUid == gameUid) { context.getString(R.string.target_unauthorized) }
        relay?.close()
        val config = JSONObject(configuration)
        val relayContext = AppLanguage.relayContext(context, config.optString("language", "en"))
        relay = HidRelay(config, lifetime, relayContext).also { it.start() }
        return true
    }
    @Synchronized override fun updateRelay(skills: Boolean, vertical: Boolean, draining: Boolean): String {
        enforceOwner()
        return relay?.update(skills, vertical, draining) ?: "{\"state\":\"stopped\"}"
    }
    @Synchronized override fun stopRelay(): String {
        enforceOwner()
        val current = relay ?: return "{\"state\":\"stopped\"}"
        current.close(); relay = null
        return current.status()
    }

    private fun deliver(event: InputEvent, targetUid: Int): Boolean {
        enforceOwner()
        // The owner UID is only for the in-app test receiver. No arbitrary target injection.
        check(targetUid == ownerUid || (gameUid >= 10_000 && targetUid == gameUid)) { context.getString(R.string.target_unauthorized) }
        val identity = Binder.clearCallingIdentity()
        return try {
            // Local diagnostic only: success means Android accepted dispatch, not game handling.
            (inject.invoke(manager, event, 1, targetUid) as Boolean).also { accepted ->
                lastError = if (accepted) "" else context.getString(R.string.android_injection_refused, targetUid)
            }
        } catch (e: ReflectiveOperationException) {
            val cause = e.cause ?: e
            lastError = "${cause.javaClass.simpleName}: ${cause.message?.take(180)}"
            false
        }
        finally { Binder.restoreCallingIdentity(identity) }
    }
    override fun destroy() {
        val caller = Binder.getCallingUid()
        check(caller == ownerUid || caller == 2000 || caller == 0)
        relay?.close()
        kotlin.system.exitProcess(0)
    }
}
