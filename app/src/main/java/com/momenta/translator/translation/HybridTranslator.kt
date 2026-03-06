package com.momenta.translator.translation

import android.content.Context
import com.google.mlkit.nl.translate.Translator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 混合翻译器 - Plan C 方案
 *
 * 策略：
 * 1. 首先尝试使用预打包的 TFLite 模型（快速、离线）
 * 2. 如果 TFLite 不可用，则尝试 ML Kit 在线模型（需下载）
 * 3. 用户体验：启动时即可使用，无需等待下载
 *
 * 优势：
 * - ✅ 即开即用（TFLite 预打包）
 * - ✅ 完全离线可用
 * - ✅ 可选在线增强（ML Kit 模型下载后自动升级翻译质量）
 */
class HybridTranslator(
    private val context: Context,
    private val mlkitTranslator: Translator
) {

    private val tfliteTranslator = TFLiteTranslator(context)
    private var tfliteReady = false
    private var mlkitReady = false

    /**
     * 初始化混合翻译器
     */
    suspend fun initialize(): InitResult {
        // 1. 先初始化 TFLite（本地模型，必成功）
        tfliteReady = tfliteTranslator.initialize()

        // 2. 尝试检查 ML Kit 模型是否已下载
        mlkitReady = checkMLKitModel()

        return InitResult(
            tfliteAvailable = tfliteReady,
            mlkitAvailable = mlkitReady,
            message = when {
                tfliteReady && mlkitReady -> "小冷准备好啦！在线模式 ✨"
                tfliteReady && !mlkitReady -> "小冷准备好啦！离线模式 ❄️"
                !tfliteReady && mlkitReady -> "小冷准备好啦！在线模式 ✨"
                else -> "小冷遇到问题了"
            }
        )
    }

    /**
     * 翻译文本
     * 优先使用 ML Kit（质量更好），失败则降级到 TFLite
     */
    suspend fun translate(text: String): TranslateResult {
        // 策略1: 优先尝试 ML Kit（如果已准备好）
        if (mlkitReady) {
            try {
                val result = translateWithMLKit(text)
                return TranslateResult(
                    text = result,
                    method = TranslateMethod.MLKIT,
                    success = true
                )
            } catch (e: Exception) {
                // ML Kit 失败，降级到 TFLite
                mlkitReady = false
            }
        }

        // 策略2: 使用 TFLite 本地模型
        if (tfliteReady) {
            try {
                val result = tfliteTranslator.translate(text)
                return TranslateResult(
                    text = result,
                    method = TranslateMethod.TFLITE,
                    success = true,
                    note = "离线翻译"
                )
            } catch (e: Exception) {
                return TranslateResult(
                    text = "",
                    method = TranslateMethod.NONE,
                    success = false,
                    error = "翻译失败: ${e.message}"
                )
            }
        }

        // 策略3: 都不可用时返回错误
        return TranslateResult(
            text = "",
            method = TranslateMethod.NONE,
            success = false,
            error = "小冷的翻译功能暂时不可用"
        )
    }

    /**
     * 检查 ML Kit 模型是否已下载
     */
    private suspend fun checkMLKitModel(): Boolean =
        suspendCancellableCoroutine { cont ->
            // 注意：ML Kit 没有直接的"检查模型"API
            // 我们通过尝试下载来判断（如果已存在会立即成功）
            mlkitTranslator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    cont.resume(true)
                }
                .addOnFailureListener {
                    cont.resume(false)
                }
        }

    /**
     * 使用 ML Kit 翻译
     */
    private suspend fun translateWithMLKit(text: String): String =
        suspendCancellableCoroutine { cont ->
            mlkitTranslator.translate(text)
                .addOnSuccessListener { translated ->
                    cont.resume(translated)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    /**
     * 尝试在后台下载 ML Kit 模型（用于升级翻译质量）
     */
    suspend fun tryUpgradeMLKit(): Boolean =
        suspendCancellableCoroutine { cont ->
            mlkitTranslator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    mlkitReady = true
                    cont.resume(true)
                }
                .addOnFailureListener {
                    cont.resume(false)
                }
        }

    /**
     * 关闭翻译器
     */
    fun close() {
        tfliteTranslator.close()
        mlkitTranslator.close()
    }

    /**
     * 初始化结果
     */
    data class InitResult(
        val tfliteAvailable: Boolean,
        val mlkitAvailable: Boolean,
        val message: String
    )

    /**
     * 翻译结果
     */
    data class TranslateResult(
        val text: String,
        val method: TranslateMethod,
        val success: Boolean,
        val note: String = "",
        val error: String = ""
    )

    /**
     * 翻译方法
     */
    enum class TranslateMethod {
        MLKIT,      // ML Kit 在线模型（高质量）
        TFLITE,     // TFLite 本地模型（快速）
        NONE        // 都不可用
    }
}
