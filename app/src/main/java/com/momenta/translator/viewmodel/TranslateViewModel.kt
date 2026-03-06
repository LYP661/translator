package com.momenta.translator.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.momenta.translator.data.TranslationHistoryManager
import com.momenta.translator.translation.HybridTranslator
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class TranslateState {
    object Idle : TranslateState()
    object Loading : TranslateState()
    data class OcrDone(val text: String) : TranslateState()      // OCR 识别完成，待翻译
    data class Success(
        val original: String,
        val translated: String,
        val method: String = ""  // "在线翻译" 或 "离线翻译"
    ) : TranslateState()
    data class Error(val message: String) : TranslateState()
}

class TranslateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableLiveData<TranslateState>(TranslateState.Idle)
    val state: LiveData<TranslateState> = _state

    // ML Kit 文字识别器（拉丁字母，可识别英/法/德/西等）
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // ML Kit 翻译器（英→中）
    private val mlkitTranslator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    )

    // 混合翻译器（Plan C: TFLite + ML Kit）
    private val hybridTranslator = HybridTranslator(
        context = application.applicationContext,
        mlkitTranslator = mlkitTranslator
    )

    // 翻译历史管理器
    private val historyManager = TranslationHistoryManager(application.applicationContext)

    private var translatorReady = false

    init {
        initializeTranslator()
    }

    /** 初始化混合翻译器 */
    private fun initializeTranslator() {
        viewModelScope.launch {
            try {
                val result = hybridTranslator.initialize()
                translatorReady = true
                // 静默：不需要通知用户初始化成功

                // 在后台尝试升级到 ML Kit 模型（如果网络可用）
                if (!result.mlkitAvailable) {
                    viewModelScope.launch {
                        hybridTranslator.tryUpgradeMLKit()
                    }
                }
            } catch (e: Exception) {
                translatorReady = false
            }
        }
    }

    // ─────────────────────────────────────────────
    // 拍照路径：Bitmap → OCR → 翻译
    // ─────────────────────────────────────────────
    fun recognizeAndTranslate(bitmap: Bitmap) {
        _state.value = TranslateState.Loading
        viewModelScope.launch {
            try {
                val text = recognizeText(bitmap)
                if (text.isBlank()) {
                    _state.value = TranslateState.Error("小冷没看到文字呢，换个角度试试？")
                    return@launch
                }
                // 先展示识别结果
                _state.value = TranslateState.OcrDone(text)
                // 再翻译
                val result = translateText(text)

                // 保存到历史记录
                historyManager.addTranslation(text, result.text)

                _state.value = TranslateState.Success(
                    original = text,
                    translated = result.text,
                    method = when (result.method) {
                        HybridTranslator.TranslateMethod.MLKIT -> "在线翻译"
                        HybridTranslator.TranslateMethod.TFLITE -> "离线翻译"
                        else -> ""
                    }
                )
            } catch (e: Exception) {
                _state.value = TranslateState.Error(e.message ?: "小冷遇到点问题了")
            }
        }
    }

    // ─────────────────────────────────────────────
    // 文字输入路径：直接翻译
    // ─────────────────────────────────────────────
    fun translateInput(text: String) {
        if (text.isBlank()) {
            _state.value = TranslateState.Error("告诉小冷要翻译什么吧")
            return
        }
        _state.value = TranslateState.Loading
        viewModelScope.launch {
            try {
                val result = translateText(text.trim())

                // 保存到历史记录
                historyManager.addTranslation(text.trim(), result.text)

                _state.value = TranslateState.Success(
                    original = text.trim(),
                    translated = result.text,
                    method = when (result.method) {
                        HybridTranslator.TranslateMethod.MLKIT -> "在线翻译"
                        HybridTranslator.TranslateMethod.TFLITE -> "离线翻译"
                        else -> ""
                    }
                )
            } catch (e: Exception) {
                _state.value = TranslateState.Error(e.message ?: "小冷翻译时出错了")
            }
        }
    }

    fun reset() {
        _state.value = TranslateState.Idle
    }

    /** 清空翻译历史 */
    fun clearHistory() {
        historyManager.clearHistory()
    }

    // ─────────────────────────────────────────────
    // 内部：协程包装 ML Kit 回调
    // ─────────────────────────────────────────────
    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            latinRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    private suspend fun translateText(text: String): HybridTranslator.TranslateResult {
        if (!translatorReady) {
            throw Exception("翻译器还没准备好，请稍后再试")
        }

        // 使用混合翻译器
        val result = hybridTranslator.translate(text)
        if (!result.success) {
            throw Exception(result.error)
        }

        return result
    }

    override fun onCleared() {
        super.onCleared()
        latinRecognizer.close()
        hybridTranslator.close()
    }
}
