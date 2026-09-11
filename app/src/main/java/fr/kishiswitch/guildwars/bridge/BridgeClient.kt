package fr.kishiswitch.guildwars.bridge

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.BuildConfig
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicInteger

class BridgeClient(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("Kishi-input").apply { start() }
    private val worker = Handler(workerThread.looper)
    private val generation = AtomicInteger()
    private val queued = AtomicInteger()
    @Volatile private var remote: IInputBridge? = null
    var state = "Shizuku à démarrer"
        private set
    var onFailure: (() -> Unit)? = null
    val ready: Boolean get() = remote != null
    private var binding = false
    private val args = Shizuku.UserServiceArgs(ComponentName(context, InputBridge::class.java))
        .daemon(false).processNameSuffix("input").tag("kishi-input-v1").version(BuildConfig.VERSION_CODE)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val peer = IInputBridge.Stub.asInterface(binder)
            worker.post {
                val success = runCatching { peer.ping() }.getOrDefault(false)
                main.post {
                    binding = false
                    if (success) {
                        remote = peer; state = "Shizuku connecté"
                        runCatching { binder?.linkToDeath({ main.post { lost("Connexion Shizuku interrompue") } }, 0) }
                    } else state = "Injection indisponible : ouvrez le diagnostic"
                    RuntimeState.record(state); RuntimeState.changed()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { lost("Shizuku à relancer") }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky { main.post { connect(false) } }
        Shizuku.addBinderDeadListener { main.post { lost("Shizuku à relancer") } }
        Shizuku.addRequestPermissionResultListener { _, result ->
            if (result == PackageManager.PERMISSION_GRANTED) main.post { connect(false) }
        }
    }

    fun connect(requestPermission: Boolean) {
        if (ready || binding) return
        try {
            if (!Shizuku.pingBinder()) { state = "Shizuku à démarrer"; RuntimeState.changed(); return }
            if (Shizuku.getVersion() < 13) { state = "Mettez Shizuku à jour (version 13 ou supérieure)"; RuntimeState.changed(); return }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                state = "Autorisation Shizuku requise"
                if (requestPermission) Shizuku.requestPermission(41)
            } else {
                binding = true; state = "Connexion à Shizuku…"
                Shizuku.bindUserService(args, connection)
                main.postDelayed({
                    if (binding) { binding = false; state = "Connexion impossible : relancez Shizuku"; RuntimeState.changed() }
                }, 8000)
            }
        } catch (e: RuntimeException) { binding = false; state = "Shizuku indisponible"; RuntimeState.record("Connexion : ${e.javaClass.simpleName}") }
        RuntimeState.changed()
    }

    /** Serial queue shared by key and motion events. Never perform binder calls in onKeyEvent. */
    fun send(event: InputEvent, targetUid: Int, critical: Boolean = true, callback: ((Boolean) -> Unit)? = null): Boolean {
        val peer = remote ?: return false
        if (queued.get() >= 128) { main.post { lost("Flux interrompu : délai excessif") }; return false }
        val copy: InputEvent = when (event) {
            is KeyEvent -> KeyEvent(event)
            is MotionEvent -> MotionEvent.obtain(event)
            else -> return false
        }
        val epoch = generation.get()
        queued.incrementAndGet()
        worker.post {
            try {
                if (generation.get() != epoch) return@post
                val result = runCatching {
                    when (copy) {
                        is KeyEvent -> peer.injectKey(copy, targetUid)
                        is MotionEvent -> peer.injectMotion(copy, targetUid)
                        else -> false
                    }
                }
                val success = result.getOrDefault(false)
                val detail = if (success) "" else result.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message?.take(180)}" }
                    ?: runCatching { peer.lastError }.getOrDefault("Erreur de transport sans détail")
                main.post {
                    if (generation.get() == epoch) {
                        callback?.invoke(success)
                        if (!success && critical) {
                            RuntimeState.record("Échec d’injection : $detail")
                            lost("Transmission interrompue · interception arrêtée")
                        }
                    }
                }
            } finally {
                if (copy is MotionEvent) copy.recycle()
                queued.decrementAndGet()
            }
        }
        return true
    }

    fun cancelPending() { generation.incrementAndGet() }

    private fun lost(reason: String) {
        val wasReady = ready || binding
        remote = null; binding = false; cancelPending(); state = reason
        if (wasReady) { RuntimeState.record(reason); onFailure?.invoke() }
        RuntimeState.changed()
    }
}
