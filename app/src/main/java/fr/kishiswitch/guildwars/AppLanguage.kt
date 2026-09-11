package fr.kishiswitch.guildwars

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The Shizuku process needs the language selected by the app's resources. */
object AppLanguage {
    fun relayContext(base: Context, language: String): Context {
        val locale = Locale.forLanguageTag(if (language == "fr") "fr" else "en")
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            setLocale(locale)
        })
    }
}
