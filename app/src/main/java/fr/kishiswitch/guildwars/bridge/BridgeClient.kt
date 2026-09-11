package fr.kishiswitch.guildwars.bridge

import fr.kishiswitch.guildwars.R
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Binder
import android.os.Looper
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.BuildConfig
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicInteger
import fr.kishiswitch.engine.Options
import org.json.JSONObject

class BridgeClient(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("Kishi-input").apply { start() }
    private val worker = Handler(workerThread.looper)
    private val generation = AtomicInteger()
    private val queued = AtomicInteger()
    @Volatile private var remote: IInputBridge? = null
    private var stateRes = R.string.shizuku_start
    val state: String get() = context.getString(stateRes)
    var onFailure: (() -> Unit)? = null
    val ready: Boolean get() = remote != null
    private var binding = false
    private val relayEpoch = AtomicInteger()
    private val lifetime = Binder()
    @Volatile private var relayOptions = Options(false, false)
    @Volatile private var relayDraining = false
    var onRelayStatus: ((JSONObject) -> Unit)? = null
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
                        remote = peer; stateRes = R.string.shizuku_connected
                        runCatching { binder?.linkToDeath({ main.post { lost(R.string.shizuku_connection_lost) } }, 0) }
                    } else stateRes = R.string.injection_unavailable
                    RuntimeState.record(state); RuntimeState.changed()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { lost(R.string.shizuku_restart) }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky { main.post { connect(false) } }
        Shizuku.addBinderDeadListener { main.post { lost(R.string.shizuku_restart) } }
        Shizuku.addRequestPermissionResultListener { _, result ->
            if (result == PackageManager.PERMISSION_GRANTED) main.post { connect(false) }
        }
    }

    fun connect(requestPermission: Boolean) {
        if (ready || binding) return
        try {
            if (!Shizuku.pingBinder()) { stateRes = R.string.shizuku_start; RuntimeState.changed(); return }
            if (Shizuku.getVersion() < 13) { stateRes = R.string.shizuku_update; RuntimeState.changed(); return }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                stateRes = R.string.shizuku_permission
                if (requestPermission) Shizuku.requestPermission(41)
            } else {
                binding = true; stateRes = R.string.shizuku_connecting
                Shizuku.bindUserService(args, connection)
                main.postDelayed({
                    if (binding) { binding = false; stateRes = R.string.shizuku_connect_failed; RuntimeState.changed() }
                }, 8000)
            }
        } catch (e: RuntimeException) { binding = false; stateRes = R.string.shizuku_unavailable; RuntimeState.record(context.getString(R.string.connection_error_log, e.javaClass.simpleName)) }
        RuntimeState.changed()
    }

    /** Serial queue shared by key and motion events. Never perform binder calls in onKeyEvent. */
    fun send(event: InputEvent, targetUid: Int, critical: Boolean = true, callback: ((Boolean) -> Unit)? = null): Boolean {
        val peer = remote ?: return false
        if (queued.get() >= 128) { main.post { lost(R.string.stream_timeout) }; return false }
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
                    ?: runCatching { peer.lastError }.getOrDefault(context.getString(R.string.transport_error_unknown))
                main.post {
                    if (generation.get() == epoch) {
                        callback?.invoke(success)
                        if (!success && critical) {
                            RuntimeState.record(context.getString(R.string.injection_failed, detail))
                            lost(R.string.transmission_interrupted)
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

    fun startRelay(configuration: JSONObject, targetUid: Int) {
        val peer=remote ?: return
        val epoch=relayEpoch.incrementAndGet()
        worker.post {
            try {
                if(epoch!=relayEpoch.get())return@post
                check(peer.startRelay(configuration.toString(),lifetime,targetUid))
                pollRelay(peer,epoch)
            } catch(e:Exception) {
                main.post { if(epoch==relayEpoch.get())onRelayStatus?.invoke(JSONObject().put("state","failed").put("error",e.message ?: context.getString(R.string.connection_interrupted))) }
            }
        }
    }
    fun updateRelay(options:Options,draining:Boolean=false) { relayOptions=options;relayDraining=draining }
    private fun pollRelay(peer:IInputBridge,epoch:Int) {
        if(epoch!=relayEpoch.get())return
        val result=runCatching { peer.updateRelay(relayOptions.skills,relayOptions.rightVertical,relayDraining) }
        val json=result.getOrNull()?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: JSONObject().put("state","failed").put("error",context.getString(R.string.relay_connection_lost))
        main.post { if(epoch==relayEpoch.get())onRelayStatus?.invoke(json) }
        if(json.optString("state") in setOf("starting","active"))worker.postDelayed({pollRelay(peer,epoch)},250)
    }
    fun stopRelay() {
        relayEpoch.incrementAndGet()
        val peer=remote ?: return
        worker.post {
            runCatching { peer.stopRelay() }.getOrNull()?.let { status ->
                val json=runCatching { JSONObject(status) }.getOrNull()
                if(json!=null)RuntimeState.record(context.getString(R.string.relay_stopped_log, json.optLong("frames"), json.optLong("keys")))
            }
        }
    }

    private fun lost(reason: Int) {
        val wasReady = ready || binding
        stopRelay()
        remote = null; binding = false; cancelPending(); stateRes = reason
        if (wasReady) { RuntimeState.record(context.getString(reason)); onFailure?.invoke() }
        RuntimeState.changed()
    }
}
