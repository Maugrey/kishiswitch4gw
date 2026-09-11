package fr.kishiswitch.guildwars.input

import fr.kishiswitch.guildwars.R
import android.content.res.Configuration
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import fr.kishiswitch.engine.Options
import fr.kishiswitch.guildwars.KishiApplication
import fr.kishiswitch.guildwars.RuntimeState
import fr.kishiswitch.guildwars.data.GAME_PACKAGE
import fr.kishiswitch.guildwars.data.TrialStage
import fr.kishiswitch.guildwars.ui.FloatingControls
import org.json.JSONObject

/** Foreground/keyboard gating and overlay only. No accessibility input interception. */
class RemapService:AccessibilityService(),InputManager.InputDeviceListener {
    private val main=Handler(Looper.getMainLooper())
    private val app get()=application as KishiApplication
    private val settings get()=app.settings
    private val bridge get()=app.bridge
    private lateinit var floating:FloatingControls
    private var refreshing=false
    private var sessionKey=""
    private var blockedKey=""
    private var lastContextKey=""
    private var connecting=false
    private var active=false
    private var effective=Options(false,false)
    private var pending=false
    private var relayError=""
    private var trialSeenGame=false
    private var seenTrialExpiry=0L
    private var currentLanguage=""
    private val listener:()->Unit={main.post{refresh()}}
    private val preferences=SharedPreferences.OnSharedPreferenceChangeListener { _,_->main.post{refresh()} }
    private val timeout=Runnable{stopRelay();RuntimeState.endTrial(this@RemapService);refresh()}
    private val screen=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){stopRelay();refresh()}}

    override fun onServiceConnected() {
        currentLanguage=getString(R.string.resource_language)
        val info=serviceInfo
        InterceptionConfig.apply(info,false,false)
        serviceInfo=info
        RuntimeState.capturingMotion=false;RuntimeState.filteringKeys=false
        floating=FloatingControls(this,settings)
        RuntimeState.serviceConnected=true
        RuntimeState.stopService={stopRelay();RuntimeState.endTrial(this@RemapService);floating.hide();disableSelf()}
        settings.prefs.registerOnSharedPreferenceChangeListener(preferences)
        RuntimeState.listen(listener)
        getSystemService(InputManager::class.java).registerInputDeviceListener(this,main)
        registerReceiver(screen,IntentFilter().apply {addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_USER_PRESENT)},Context.RECEIVER_NOT_EXPORTED)
        bridge.onFailure={stopRelay();RuntimeState.trialFailed=true;RuntimeState.endTrial(this@RemapService);refresh()}
        bridge.onRelayStatus={status->onRelayStatus(status)}
        bridge.connect(false);refresh();RuntimeState.changed()
    }
    override fun onAccessibilityEvent(event:AccessibilityEvent?){refresh()}
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::floating.isInitialized) return
        val language=getString(R.string.resource_language)
        if(language==currentLanguage)return
        currentLanguage=language
        stopRelay()
        blockedKey="";lastContextKey="";relayError=""
        floating.hide()
        refresh()
        RuntimeState.changed()
    }
    private fun requested():Options=when(RuntimeState.trial){
        TrialStage.PASSTHROUGH->Options(false,false)
        TrialStage.SKILLS->Options(true,false)
        TrialStage.VERTICAL->Options(false,true)
        TrialStage.NONE->Options(settings.options.skills&&settings.validated(TrialStage.PASSTHROUGH)&&settings.validated(TrialStage.SKILLS),
            settings.options.rightVertical&&settings.profile?.rightVertical?.id==14&&settings.validated(TrialStage.PASSTHROUGH)&&settings.validated(TrialStage.VERTICAL))
    }
    private fun refresh() {
        if(refreshing || !::floating.isInitialized)return
        refreshing=true
        val oldStatus=RuntimeState.status
        try {
            val current=windows
            val focused=current.firstOrNull{it.isFocused} ?: current.firstOrNull{it.type==AccessibilityWindowInfo.TYPE_APPLICATION&&it.isActive}
            val keyboard=current.any{it.type==AccessibilityWindowInfo.TYPE_INPUT_METHOD}
            val unlocked=!getSystemService(KeyguardManager::class.java).isKeyguardLocked
            val game=focused?.type==AccessibilityWindowInfo.TYPE_APPLICATION&&focused.root?.packageName?.toString()==GAME_PACKAGE&&unlocked&&!RuntimeState.ownUiVisible
            RuntimeState.gameForeground=game
            if(seenTrialExpiry!=RuntimeState.trialExpiresAt){
                stopRelay();seenTrialExpiry=RuntimeState.trialExpiresAt;trialSeenGame=false;blockedKey=""
                main.removeCallbacks(timeout)
                if(RuntimeState.trial!=TrialStage.NONE)main.postDelayed(timeout,(seenTrialExpiry-SystemClock.elapsedRealtime()).coerceAtLeast(1))
            }
            if(RuntimeState.trial!=TrialStage.NONE){
                if(game)trialSeenGame=true
                else if(trialSeenGame){stopRelay();RuntimeState.endTrial(this@RemapService)}
            }
            val profile=settings.profile
            val device=InputDevice.getDeviceIds().asSequence().mapNotNull(InputDevice::getDevice).firstOrNull{profile?.matches(it)==true}
            val options=requested()
            val isTrial=RuntimeState.trial!=TrialStage.NONE
            val key=listOf(device?.id,profile,settings.threshold,RuntimeState.trialExpiresAt).joinToString("|")
            val contextKey=listOf(key,game,keyboard,bridge.ready,options).joinToString("|")
            if(contextKey!=lastContextKey){blockedKey="";relayError="";lastContextKey=contextKey}
            val eligible=game&&!keyboard&&bridge.ready&&device!=null&&profile?.ready==true
            val wanted=isTrial||options.skills||options.rightVertical
            if(!eligible || (sessionKey.isNotEmpty()&&sessionKey!=key))stopRelay()
            if(eligible && (wanted||active||connecting)){
                bridge.updateRelay(options,!wanted)
                if(sessionKey.isEmpty()&&wanted&&blockedKey!=key){
                    val uid=packageManager.getApplicationInfo(GAME_PACKAGE,0).uid
                    sessionKey=key;connecting=true
                    val configuration=JSONObject().put("deviceId",device!!.id).put("descriptor",profile!!.descriptor)
                        .put("lt",profile.leftTrigger!!.axis).put("rt",profile.rightTrigger!!.axis)
                        .put("ry",profile.rightVertical?.id ?: 14).put("threshold",settings.threshold)
                        .put("skills",options.skills).put("vertical",options.rightVertical)
                        .put("language",getString(R.string.resource_language))
                    bridge.startRelay(configuration,uid)
                    RuntimeState.record(getString(R.string.relay_start_log)+device.name)
                }
            } else if(!wanted)stopRelay()
            RuntimeState.relayActive=active
            RuntimeState.status=when {
                !bridge.ready->bridge.state
                profile==null->getString(R.string.identify_kishi_prompt)
                device==null->getString(R.string.connect_kishi_prompt)
                !profile.ready->getString(R.string.identify_triggers_prompt)
                !game->getString(R.string.waiting_for_game)
                keyboard->getString(R.string.typing_native)
                relayError.isNotEmpty()->relayError
                connecting->getString(R.string.controller_connecting)
                pending->getString(R.string.settings_pending)
                active&&isTrial->getString(R.string.temporary_trial)+RuntimeState.trial.label(this@RemapService)
                active->getString(R.string.inversions_active)
                !settings.validated(TrialStage.PASSTHROUGH)->getString(R.string.passthrough_needs_test)
                else->getString(R.string.native_inversions_off)
            }
            floating.update(game&&!keyboard,active,effective)
        } catch(e:Exception){stopRelay();floating.hide();RuntimeState.status=getString(R.string.window_unavailable);RuntimeState.record(getString(R.string.window_error_log)+e.javaClass.simpleName)}
        finally{refreshing=false;if(oldStatus!=RuntimeState.status)RuntimeState.changed()}
    }
    private fun onRelayStatus(status:JSONObject){
        if(sessionKey.isEmpty())return
        val previous=listOf(active,connecting,effective,pending)
        when(status.optString("state")){
            "active"->{active=true;connecting=false}
            "starting"->{active=false;connecting=true}
            "failed"->{
                blockedKey=sessionKey;relayError=status.optString("error",getString(R.string.transmission_unavailable))
                RuntimeState.record(getString(R.string.relay_interrupted_log)+relayError);RuntimeState.trialFailed=true
                stopRelay();RuntimeState.endTrial(this@RemapService);refresh();return
            }
            "stopped"->{
                val options=requested()
                if(RuntimeState.trial!=TrialStage.NONE || options.skills || options.rightVertical){
                    blockedKey=sessionKey
                    relayError=getString(R.string.relay_stopped_retry)
                    RuntimeState.trialFailed=true
                    RuntimeState.record(relayError)
                }
                stopRelay();RuntimeState.endTrial(this@RemapService);refresh();return
            }
        }
        effective=Options(status.optBoolean("skills"),status.optBoolean("vertical"));pending=status.optBoolean("pending")
        if(RuntimeState.trial!=TrialStage.NONE){
            val frames=status.optLong("frames").coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val keys=status.optLong("keys").coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            RuntimeState.trialCapturedMotions=frames;RuntimeState.trialInjectedMotions=frames;RuntimeState.trialInjectedKeys=keys
            RuntimeState.trialEvents=if(RuntimeState.trial==TrialStage.SKILLS)keys else frames
        }
        if(previous!=listOf(active,connecting,effective,pending))refresh()
    }
    private fun stopRelay(){
        if(sessionKey.isNotEmpty())bridge.stopRelay()
        sessionKey="";active=false;connecting=false;pending=false;effective=Options(false,false);RuntimeState.relayActive=false
    }
    override fun onInputDeviceAdded(id:Int){refresh()}
    override fun onInputDeviceRemoved(id:Int){refresh()}
    override fun onInputDeviceChanged(id:Int){refresh()}
    override fun onInterrupt(){stopRelay()}
    override fun onUnbind(intent:Intent?):Boolean{stopRelay();return super.onUnbind(intent)}
    override fun onDestroy(){
        stopRelay();main.removeCallbacksAndMessages(null)
        RuntimeState.unlisten(listener);settings.prefs.unregisterOnSharedPreferenceChangeListener(preferences)
        getSystemService(InputManager::class.java).unregisterInputDeviceListener(this)
        runCatching{unregisterReceiver(screen)}
        bridge.onRelayStatus=null;bridge.onFailure=null
        if(::floating.isInitialized)floating.hide()
        RuntimeState.serviceConnected=false;RuntimeState.stopService=null;RuntimeState.endTrial(this@RemapService);RuntimeState.changed()
        super.onDestroy()
    }
}
