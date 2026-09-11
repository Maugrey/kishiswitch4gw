package fr.kishiswitch.guildwars

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.kishiswitch.guildwars.data.Settings
import fr.kishiswitch.guildwars.data.TrialStage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun localized(language: String) = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    )

    @Test fun englishFrenchAndUnsupportedLanguageResolveCorrectly() {
        assertEquals("Your controls,\nin the right place.", localized("en").getString(R.string.main_title))
        assertEquals("Vos commandes,\nà la bonne place.", localized("fr").getString(R.string.main_title))
        assertEquals("en", localized("de").getString(R.string.resource_language))
        assertEquals("fr", localized("fr-CA").getString(R.string.resource_language))
        assertEquals("1 · Passthrough", TrialStage.PASSTHROUGH.label(localized("en")))
        assertEquals("1 · Transmission intacte", TrialStage.PASSTHROUGH.label(localized("fr")))
    }

    @Test fun formattedResourcesRetainLineBreaksPercentagesAndSpacing() {
        for (language in listOf("en", "fr")) {
            val local = localized(language)
            for (field in R.string::class.java.fields) {
                val id = field.getInt(null)
                val raw = local.getString(id)
                val placeholders = Regex("%(\\d+)\\\$([sdb])").findAll(raw).toList()
                val count = placeholders.maxOfOrNull { it.groupValues[1].toInt() } ?: 0
                val arguments = Array<Any>(count) { index ->
                    when (placeholders.first { it.groupValues[1].toInt() == index + 1 }.groupValues[2]) {
                        "d" -> 42
                        "b" -> true
                        else -> "value${index + 1}"
                    }
                }
                val rendered = local.getString(id, *arguments)
                assertFalse(field.name, Regex("%\\d+\\\$[sdb]").containsMatchIn(rendered))
            }
            assertEquals(3, local.getString(R.string.identified_axes, "LT", "RT", "RZ", "overlay").count { it == '\n' })
            val threshold = local.getString(R.string.trigger_threshold_value, 50)
            assertEquals(1, threshold.count { it == '%' })
            assertTrue(threshold.contains("50"))
            assertTrue(local.getString(R.string.skills_help).endsWith(" "))
        }
    }

    @Test fun relayErrorsUseSelectedLanguageAndRejectUnsupportedLanguageTags() {
        val frenchBase = localized("fr")
        assertEquals("Kishi disconnected", AppLanguage.relayContext(frenchBase, "en").getString(R.string.kishi_disconnected))
        assertEquals("Kishi débranchée", AppLanguage.relayContext(localized("en"), "fr").getString(R.string.kishi_disconnected))
        assertEquals("en", AppLanguage.relayContext(frenchBase, "ar").getString(R.string.resource_language))
    }

    @Test fun languageDoesNotChangeSavedProfileOrValidationStamp() {
        val original = Settings(context)
        for (language in listOf("en", "fr", "de")) {
            val translated = Settings(localized(language))
            assertEquals(original.currentStamp(), translated.currentStamp())
            assertEquals(original.profile, translated.profile)
            assertEquals(original.options, translated.options)
            for (stage in TrialStage.entries) assertEquals(original.validated(stage), translated.validated(stage))
        }
    }
}
