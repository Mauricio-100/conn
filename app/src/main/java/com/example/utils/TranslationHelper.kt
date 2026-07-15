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
     * Translates a given text to the target language name (e.g. "French", "Spanish", "Portuguese", "Italian").
     * Uses the free Google Translate library (therealbush/translator).
     */
    suspend fun translateText(text: String, targetLanguageName: String): String = withContext(Dispatchers.IO) {
        try {
            // Map common language names to codes if needed
            val lang = try {
                Language(targetLanguageName.lowercase())
            } catch (e: Exception) {
                // Fallback to English if target not found
                Language.ENGLISH
            }

            val result = translator.translate(text, lang, Language.AUTO)
            return@withContext result.translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Translation error with free library", e)
            throw e
        }
    }
}
