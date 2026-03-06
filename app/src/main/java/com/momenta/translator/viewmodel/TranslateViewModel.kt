package com.momenta.translator.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class TranslateState {
    object Idle : TranslateState()
    object Loading : TranslateState()
    data class OcrDone(val text: String) : TranslateState()      // OCR 识别完成，待翻译
    data class Success(val original: String, val translated: String) : TranslateState()
    data class Error(val message: String) : TranslateState()
}

class TranslateViewModel : ViewModel() {

    private val _state = MutableLiveData<TranslateState>(TranslateState.Idle)
    val state: LiveData<TranslateState> = _state

    // ML Kit 文字识别器（拉丁字母，可识别英/法/德/西等）
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // ML Kit 翻译器（英→中，按需下载模型）
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    )

    private var modelReady = false
    private var modelDownloading = false

    init {
        ensureModelDownloaded()
    }

    /** 确保翻译模型已下载（首次约 30 MB，需联网） */
    private fun ensureModelDownloaded() {
        if (modelDownloading) return

        modelDownloading = true
        val conditions = DownloadConditions.Builder()
            .requireWifi() // 使用WiFi下载（可选）
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                modelReady = true
                modelDownloading = false
            }
            .addOnFailureListener { e ->
                modelDownloading = false
                // 下载失败时，下次翻译会重试
            }
    }

    /** 翻译前检查并下载模型 */
    private suspend fun ensureModelReady(): Boolean =
        suspendCancellableCoroutine { cont ->
            if (modelReady) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }

            // 强制下载模型
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    modelReady = true
                    cont.resume(true)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(
                        Exception("模型下载失败（约30MB）\n请确保：\n1. 已连接网络\n2. 有足够存储空间\n3. Google服务可用\n\n错误: ${e.message}")
                    )
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
                    _state.value = TranslateState.Error("未检测到文字，请重新拍照")
                    return@launch
                }
                // 先展示识别结果
                _state.value = TranslateState.OcrDone(text)
                // 再翻译
                val result = translateText(text)
                _state.value = TranslateState.Success(text, result)
            } catch (e: Exception) {
                _state.value = TranslateState.Error(e.message ?: "识别失败")
            }
        }
    }

    // ─────────────────────────────────────────────
    // 文字输入路径：直接翻译
    // ─────────────────────────────────────────────
    fun translateInput(text: String) {
        if (text.isBlank()) {
            _state.value = TranslateState.Error("请输入要翻译的文字")
            return
        }
        _state.value = TranslateState.Loading
        viewModelScope.launch {
            try {
                val result = translateText(text.trim())
                _state.value = TranslateState.Success(text.trim(), result)
            } catch (e: Exception) {
                _state.value = TranslateState.Error(e.message ?: "翻译失败")
            }
        }
    }

    fun reset() {
        _state.value = TranslateState.Idle
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

    private suspend fun translateText(text: String): String {
        // 翻译前确保模型已下载
        try {
            ensureModelReady()
        } catch (e: Exception) {
            throw e
        }

        // 执行翻译
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { translated ->
                    cont.resume(translated)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(
                        Exception("翻译失败：${e.message}")
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        latinRecognizer.close()
        translator.close()
    }
}
