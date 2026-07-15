package com.example.utils

import android.util.Log
import me.bush.translator.Translator
import me.bush.translator.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TranslationHelper {
    private const val TAG = "TranslationHelper"
    
    private val translator = Translator()

    /**
     * List of supported languages provided by the engine.
     */
    val supportedLanguages: List<String> by lazy {
        Language.values().map { it.name.lowercase().capitalize() }.sorted()
    }

    /**
     * Translates a given text to the target language name.
     */
    suspend fun translateText(text: String, targetLanguageName: String): String = withContext(Dispatchers.IO) {
        try {
            val lang = try {
                // Find matching language by name or code
                Language.values().find { it.name.equals(targetLanguageName, ignoreCase = true) } 
                    ?: Language.values().find { it.toString().equals(targetLanguageName, ignoreCase = true) }
                    ?: Language.ENGLISH
            } catch (e: Exception) {
                Language.ENGLISH
            }

            val result = translator.translate(text, lang, Language.AUTO)
            return@withContext result.translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            throw e
        }
    }
}

// Helper extension for capitalization
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
