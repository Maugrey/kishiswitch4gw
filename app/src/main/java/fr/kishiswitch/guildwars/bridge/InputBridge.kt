package fr.kishiswitch.guildwars.bridge

import android.content.Context
import android.os.Binder
import android.os.Process
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import fr.kishiswitch.guildwars.data.GAME_PACKAGE

/** Instantiated by Shizuku, never registered as an exported Android service. */
@Suppress("unused")
class InputBridge(context: Context) : IInputBridge.Stub() {
    private val ownerUid = context.applicationInfo.uid
    private val gameUid = runCatching { context.packageManager.getApplicationInfo(GAME_PACKAGE, 0).uid }.getOrDefault(-1)
    private val managerClass = Class.forName("android.hardware.input.InputManagerGlobal")
    private val manager = managerClass.getDeclaredMethod("getInstance").invoke(null)
    private val inject = managerClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    private var lastError = ""
    override fun getLastError(): String { enforceOwner(); return lastError }

    private fun enforceOwner() {
        check(ownerUid >= 10_000 && Binder.getCallingUid() == ownerUid) { "Appelant non autorisé" }
    }
    override fun ping(): Boolean { enforceOwner(); return Process.myUid() == 2000 || Process.myUid() == 0 }
    override fun injectKey(event: KeyEvent, targetUid: Int): Boolean = deliver(event, targetUid)
    override fun injectMotion(event: MotionEvent, targetUid: Int): Boolean = deliver(event, targetUid)

    private fun deliver(event: InputEvent, targetUid: Int): Boolean {
        enforceOwner()
        // The owner UID is only for the in-app test receiver. No arbitrary target injection.
        check(targetUid == ownerUid || (gameUid >= 10_000 && targetUid == gameUid)) { "Cible non autorisée" }
        val identity = Binder.clearCallingIdentity()
        return try {
            // WAIT_FOR_RESULT (1): verify that the target accepted delivery. This call is off UI thread.
            (inject.invoke(manager, event, 1, targetUid) as Boolean).also { accepted ->
                lastError = if (accepted) "" else "Android a refusé l’injection vers UID $targetUid"
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
        kotlin.system.exitProcess(0)
    }
}
