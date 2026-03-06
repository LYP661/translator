package com.momenta.translator.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // 默认中文
                setLanguage(Locale.CHINESE)
                Log.d("TTSHelper", "TTS initialized successfully")
            } else {
                Log.e("TTSHelper", "TTS initialization failed")
            }
        }
    }

    fun setLanguage(locale: Locale): Boolean {
        if (!isInitialized) return false

        val result = tts?.setLanguage(locale)
        return when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.e("TTSHelper", "Language not supported: $locale")
                false
            }
            else -> {
                Log.d("TTSHelper", "Language set to: $locale")
                true
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("TTSHelper", "TTS not initialized yet")
            return
        }

        if (text.isEmpty()) {
            Log.w("TTSHelper", "Empty text, nothing to speak")
            return
        }

        // 自动检测语言并设置
        detectAndSetLanguage(text)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        Log.d("TTSHelper", "Speaking: $text")
    }

    private fun detectAndSetLanguage(text: String) {
        // 简单的语言检测：包含中文字符则用中文，否则用英文
        val hasChinese = text.any { it in '\u4e00'..'\u9fa5' }
        val locale = if (hasChinese) Locale.CHINESE else Locale.ENGLISH
        setLanguage(locale)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        Log.d("TTSHelper", "TTS shutdown")
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    companion object {
        fun isLanguageAvailable(context: Context, locale: Locale): Boolean {
            val tempTTS = TextToSpeech(context, null)
            val result = tempTTS.isLanguageAvailable(locale)
            tempTTS.shutdown()
            return result >= TextToSpeech.LANG_AVAILABLE
        }
    }
}
