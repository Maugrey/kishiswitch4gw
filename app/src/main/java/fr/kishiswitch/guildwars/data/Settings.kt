package fr.kishiswitch.guildwars.data

import android.content.Context
import android.os.Build
import android.view.InputDevice
import fr.kishiswitch.engine.AxisSpec
import fr.kishiswitch.engine.Options
import fr.kishiswitch.engine.TriggerSpec
import org.json.JSONObject

const val GAME_PACKAGE = "net.arena.guildwars.reforged"
const val APP_PACKAGE = "fr.kishiswitch.guildwars"

data class HardwareProfile(
    val descriptor: String,
    val name: String,
    val vendor: Int,
    val product: Int,
    val leftTrigger: TriggerSpec? = null,
    val rightTrigger: TriggerSpec? = null,
    val rightVertical: AxisSpec? = null,
    val m2Code: Int? = null,
) {
    val ready: Boolean get() = leftTrigger != null && rightTrigger != null
    fun matches(device: InputDevice): Boolean = device.descriptor == descriptor && device.vendorId == vendor && device.productId == product
}

class Settings(private val context: Context) {
    val prefs = context.getSharedPreferences("kishi", Context.MODE_PRIVATE)
    var options: Options
        get() = Options(prefs.getBoolean("skills", true), prefs.getBoolean("right_vertical", true))
        set(value) { prefs.edit().putBoolean("skills", value.skills).putBoolean("right_vertical", value.rightVertical).apply() }
    var threshold: Float
        get() = prefs.getFloat("threshold", .5f).coerceIn(.05f, .95f)
        set(value) { invalidate(); prefs.edit().putFloat("threshold", value.coerceIn(.05f, .95f)).apply() }
    var overlayX: Float
        get() = prefs.getFloat("overlay_x", .96f)
        set(value) { prefs.edit().putFloat("overlay_x", value.coerceIn(0f, 1f)).apply() }
    var overlayY: Float
        get() = prefs.getFloat("overlay_y", .3f)
        set(value) { prefs.edit().putFloat("overlay_y", value.coerceIn(0f, 1f)).apply() }

    var profile: HardwareProfile?
        get() = runCatching {
            val json = JSONObject(prefs.getString("profile", null) ?: return null)
            fun trigger(name: String): TriggerSpec? = json.optJSONObject(name)?.let {
                TriggerSpec(it.getInt("id"), it.getDouble("min").toFloat(), it.getDouble("max").toFloat())
            }
            HardwareProfile(json.getString("descriptor"), json.getString("name"), json.getInt("vendor"), json.getInt("product"),
                trigger("lt"), trigger("rt"), json.optJSONObject("ry")?.let {
                    AxisSpec(it.getInt("id"), it.getDouble("min").toFloat(), it.getDouble("max").toFloat(), it.optDouble("flat", 0.0).toFloat())
                }, if (json.has("m2")) json.getInt("m2") else null)
        }.getOrNull()
        set(value) {
            invalidate()
            if (value == null) { prefs.edit().remove("profile").apply(); return }
            fun trigger(value: TriggerSpec) = JSONObject().put("id", value.axis).put("min", value.min).put("max", value.max)
            val json = JSONObject().put("descriptor", value.descriptor).put("name", value.name)
                .put("vendor", value.vendor).put("product", value.product)
            value.leftTrigger?.let { json.put("lt", trigger(it)) }
            value.rightTrigger?.let { json.put("rt", trigger(it)) }
            value.rightVertical?.let { json.put("ry", JSONObject().put("id", it.id).put("min", it.min).put("max", it.max).put("flat", it.flat)) }
            value.m2Code?.let { json.put("m2", it) }
            prefs.edit().putString("profile", json.toString()).apply()
        }

    fun currentStamp(): String {
        val version = runCatching { context.packageManager.getPackageInfo(GAME_PACKAGE, 0).longVersionCode }.getOrDefault(-1)
        return "${Build.FINGERPRINT}|$version|${prefs.getString("profile", "")}|$threshold|protocol-2"
    }
    fun validated(stage: TrialStage): Boolean = stage != TrialStage.NONE && prefs.getString("validated_${stage.name}", null) == currentStamp()
    fun validate(stage: TrialStage) {
        require(stage != TrialStage.NONE)
        require(stage == TrialStage.PASSTHROUGH || validated(TrialStage.PASSTHROUGH))
        prefs.edit().putString("validated_${stage.name}", currentStamp()).apply()
    }
    fun invalidate() { prefs.edit().apply { TrialStage.entries.forEach { remove("validated_${it.name}") } }.apply() }
    fun unvalidate(stage: TrialStage) {
        if (stage == TrialStage.PASSTHROUGH) invalidate()
        else prefs.edit().remove("validated_${stage.name}").apply()
    }
}

enum class TrialStage(val label: String) {
    NONE("Aucun essai"),
    PASSTHROUGH("1 · Transmission intacte"),
    SKILLS("2 · Compétences"),
    VERTICAL("3 · Stick droit"),
}
