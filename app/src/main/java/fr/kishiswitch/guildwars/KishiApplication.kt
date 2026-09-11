package fr.kishiswitch.guildwars

import android.app.Application
import fr.kishiswitch.guildwars.bridge.BridgeClient
import fr.kishiswitch.guildwars.data.Settings

class KishiApplication : Application() {
    lateinit var settings: Settings
        private set
    lateinit var bridge: BridgeClient
        private set
    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        bridge = BridgeClient(this)
    }
}
