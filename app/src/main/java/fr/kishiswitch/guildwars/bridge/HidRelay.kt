package fr.kishiswitch.guildwars.bridge

import fr.kishiswitch.guildwars.R
import android.content.Context
import android.os.IBinder
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.view.InputDevice
import fr.kishiswitch.engine.KishiHidMapper
import fr.kishiswitch.engine.Options
import org.json.JSONObject
import java.io.FileDescriptor
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Shell-owned evdev -> UHID relay. Accessibility never consumes the joystick stream. */
internal class HidRelay(private val config: JSONObject, private val lifetime: IBinder, private val context: Context) : AutoCloseable {
    companion object {
        const val DEVICE_NAME = "Kishi Switch"
    }
    private fun command(vararg args: String): String {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        val read = FutureTask { process.inputStream.bufferedReader().use { it.readText().take(1_000_000) } }
        Thread(read,"Kishi-inspect").apply { isDaemon=true;start() }
        try {
            check(process.waitFor(4,TimeUnit.SECONDS)) { context.getString(R.string.device_identification_timeout) }
            check(process.exitValue()==0) { context.getString(R.string.device_identification_refused, read.get(1,TimeUnit.SECONDS).take(180)) }
            return read.get(1,TimeUnit.SECONDS)
        } finally { if(process.isAlive) process.destroyForcibly() }
    }
    private fun blockFor(descriptor:String):String {
        val dump=command("dumpsys","input")
        val headers=Regex("(?m)^    -?\\d+: [^\\r\\n]+$").findAll(dump).toList()
        return headers.mapIndexed { i,m -> dump.substring(m.range.first,headers.getOrNull(i+1)?.range?.first ?: dump.length) }
            .firstOrNull { block -> Regex("(?m)^      Descriptor: ${Regex.escape(descriptor)}\\s*$").containsMatchIn(block) &&
                block.lineSequence().firstOrNull { it.trim().startsWith("Classes:") }?.contains("JOYSTICK")==true }
            ?: error(context.getString(R.string.physical_kishi_missing))
    }
    @Volatile private var stopped=false
    @Volatile private var state="starting"
    @Volatile private var error=""
    @Volatile private var lease=SystemClock.uptimeMillis()+6000
    @Volatile private var requested=Options(config.getBoolean("skills"),config.getBoolean("vertical"))
    @Volatile private var drain=false
    @Volatile private var snapshot=JSONObject().put("state","starting").toString()
    private val death=IBinder.DeathRecipient { stopped=true;releaseResources() }
    private val thread=Thread({ run() },"Kishi-HID")
    private var grabbedFd:FileDescriptor?=null
    private var hidChild:Process?=null

    fun start() {
        lifetime.linkToDeath(death,0);thread.start()
        Thread({
            while(thread.isAlive && !stopped && SystemClock.uptimeMillis()<lease) Thread.sleep(100)
            if(thread.isAlive) {
                if(!stopped) { error=context.getString(R.string.control_lease_expired);state="failed" }
                stopped=true;releaseResources()
            }
        },"Kishi-watchdog").apply { isDaemon=true;start() }
    }
    fun update(skills:Boolean,vertical:Boolean,draining:Boolean):String {
        requested=Options(skills,vertical);drain=draining
        lease=SystemClock.uptimeMillis()+1500
        return snapshot
    }
    fun status():String=snapshot
    private fun publish(mapper:KishiHidMapper?) {
        snapshot=JSONObject().put("state",state).put("error",error).put("frames",mapper?.frames ?: 0)
            .put("keys",mapper?.buttonChanges ?: 0).put("skills",mapper?.effective?.skills ?: false)
            .put("vertical",mapper?.effective?.rightVertical ?: false).put("pending",mapper?.pending ?: false).toString()
    }
    private fun run() {
        var fd:FileDescriptor?=null
        var hid:Process?=null
        var mapper:KishiHidMapper?=null
        var writer:OutputStreamWriter?=null
        try {
            val id=config.getInt("deviceId")
            val device=InputDevice.getDevice(id) ?: error(context.getString(R.string.kishi_disconnected))
            check(device.descriptor==config.getString("descriptor") && device.vendorId==0x1532 && device.productId==0x0717) { context.getString(R.string.kishi_profile_required) }
            check(config.getInt("lt") in setOf(17,23) && config.getInt("rt") in setOf(18,22)) { context.getString(R.string.trigger_axes_incompatible) }
            check(!requested.rightVertical || config.optInt("ry",14)==14) { context.getString(R.string.rz_required) }
            val block=blockFor(device.descriptor)
            val path=Regex("(?m)^      Path: (/dev/input/event\\d+)\\s*$").find(block)?.groupValues?.get(1) ?: error(context.getString(R.string.kishi_path_missing))
            check(block.contains("KeyState (pressed): <none>")) { context.getString(R.string.release_buttons_return) }
            val capabilities=command("getevent","-lp",path)
            val names=listOf("ABS_X","ABS_Y","ABS_Z","ABS_RZ","ABS_BRAKE","ABS_GAS","ABS_HAT0X","ABS_HAT0Y")
            val values=IntArray(8);val flats=IntArray(4)
            for((i,name) in names.withIndex()) {
                val m=Regex("$name\\s*: value (-?\\d+), min (-?\\d+), max (-?\\d+), fuzz \\d+, flat (\\d+)").find(capabilities)
                    ?: error(context.getString(R.string.physical_axis_missing, name))
                values[i]=m.groupValues[1].toInt()
                check(m.groupValues[2].toInt()==if(i<6)0 else -1)
                check(m.groupValues[3].toInt()==if(i<6)255 else 1)
                if(i<4)flats[i]=m.groupValues[4].toInt()
            }
            check(Regex("ABS_[A-Z0-9]+\\s*:").findAll(capabilities).count()==8) { context.getString(R.string.extra_axes_unsupported) }
            mapper=KishiHidMapper(config.getDouble("threshold").toFloat(),requested,flats)
            values.forEachIndexed { i,v -> mapper.axis(KishiHidMapper.RAW_AXES[i],v) }
            check(mapper.neutral) { context.getString(R.string.release_sticks_return) }
            if(stopped) return
            hid=ProcessBuilder("hid","-").redirectErrorStream(true).start()
            synchronized(this) { hidChild=hid;if(stopped)hid.destroy() }
            val child=hid
            Thread({ runCatching { child.inputStream.bufferedReader().use { stream -> while(stream.readLine()!=null) Unit } } },"Kishi-HID-output").apply { isDaemon=true;start() }
            writer=OutputStreamWriter(hid.outputStream,Charsets.UTF_8)
            val register=JSONObject().put("id",1).put("command","register").put("name",DEVICE_NAME)
                .put("vid",0x1532).put("pid",0x0717).put("bus","usb")
                .put("descriptor",org.json.JSONArray(KishiHidMapper.DESCRIPTOR.toList()))
            writer.write(register.toString()+"\n");writer.flush()
            val deadline=SystemClock.uptimeMillis()+4000
            while(!stopped && SystemClock.uptimeMillis()<deadline &&
                InputDevice.getDeviceIds().none { InputDevice.getDevice(it)?.name==DEVICE_NAME }) Thread.sleep(40)
            check(InputDevice.getDeviceIds().any { InputDevice.getDevice(it)?.name==DEVICE_NAME }) { context.getString(R.string.uhid_not_recognized) }
            check(hid.isAlive) { context.getString(R.string.hid_stopped) }
            if(stopped || SystemClock.uptimeMillis()>lease) return
            // Recheck the descriptor just before grabbing. Never open a path supplied by the client.
            val beforeGrab=blockFor(device.descriptor)
            check(beforeGrab.contains("Path: $path\n")) { context.getString(R.string.kishi_changed_connecting) }
            check(beforeGrab.contains("KeyState (pressed): <none>")) { context.getString(R.string.release_buttons_retry) }
            for((i,name) in names.withIndex()) {
                val value=Regex("$name=(-?\\d+)").find(beforeGrab)?.groupValues?.get(1)?.toInt()
                    ?: error(context.getString(R.string.kishi_state_unavailable))
                check(if(i<4) kotlin.math.abs(value-128)<=flats[i] else value==0) { context.getString(R.string.release_controls_retry) }
            }
            fd=Os.open(path,OsConstants.O_RDONLY or OsConstants.O_NONBLOCK,0)
            synchronized(this) { grabbedFd=fd;check(!stopped) { context.getString(R.string.session_ended) } }
            Os::class.java.getMethod("ioctlInt",FileDescriptor::class.java,Int::class.javaPrimitiveType).invoke(null,fd,0x40044590)
            send(writer,mapper.currentReport())
            state="active";publish(mapper)
            val poll=StructPollfd().apply { this.fd=fd;events=OsConstants.POLLIN.toShort() }
            val raw=ByteArray(24*64);val buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
            var lastPublish=0L
            while(!stopped && SystemClock.uptimeMillis()<lease && hid.isAlive) {
                mapper.request(requested)
                if(drain && mapper.neutral) break
                if(Os.poll(arrayOf(poll),40)>0) {
                    check(poll.revents.toInt() and (OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL)==0) { context.getString(R.string.kishi_disconnected) }
                    val count=Os.read(fd,raw,0,raw.size)
                    check(count>0 && count%24==0) { context.getString(R.string.controller_stream_incomplete) }
                    for(i in 0 until count step 24) {
                        if(stopped || SystemClock.uptimeMillis()>lease)break
                        val type=buffer.getShort(i+16).toInt() and 65535
                        val code=buffer.getShort(i+18).toInt() and 65535
                        val value=buffer.getInt(i+20)
                        when(type) {
                            0 -> when(code){ 0 -> send(writer,mapper.frame()); 3 -> error(context.getString(R.string.input_dropped)) }
                            1 -> mapper.key(code,value)
                            3 -> mapper.axis(code,value)
                        }
                    }
                }
                if(SystemClock.uptimeMillis()-lastPublish>200){publish(mapper);lastPublish=SystemClock.uptimeMillis()}
            }
            if(!stopped && !drain) error(context.getString(R.string.control_connection_interrupted))
            state="stopped"
        } catch(e:Exception) {
            if(state!="failed")state=if(stopped)"stopped" else "failed"
            val cause=e.cause ?: e
            if(!stopped)error="${cause.javaClass.simpleName}: ${cause.message?.take(180)}"
        } finally {
            // Close the grabbed fd even if HID has died or the application no longer responds.
            releaseResources()
            writer?.let { out -> runCatching { out.close() } }
            if(state=="active" || state=="starting")state="stopped"
            publish(mapper)
            runCatching { lifetime.unlinkToDeath(death,0) }
        }
    }
    private fun send(writer:OutputStreamWriter,report:ByteArray) {
        writer.write("{\"id\":1,\"command\":\"report\",\"report\":[")
        writer.write(report.joinToString(","){(it.toInt() and 255).toString()})
        writer.write("]}\n");writer.flush()
    }
    override fun close() {
        stopped=true
        releaseResources()
        if(Thread.currentThread()!=thread)thread.join(5500)
    }
    @Synchronized private fun releaseResources() {
        val fd=grabbedFd;grabbedFd=null
        fd?.let { runCatching { Os.close(it) } }
        hidChild?.let { child -> child.destroy();if(child.isAlive)child.destroyForcibly() }
        hidChild=null
    }
}
