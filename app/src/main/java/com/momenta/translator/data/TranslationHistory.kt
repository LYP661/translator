package com.momenta.translator.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 翻译历史记录
 */
data class RecentTranslation(
    val original: String,
    val translated: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("original", original)
        put("translated", translated)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): RecentTranslation {
            return RecentTranslation(
                original = json.getString("original"),
                translated = json.getString("translated"),
                timestamp = json.getLong("timestamp")
            )
        }
    }
}

/**
 * 翻译历史管理器
 * 使用 SharedPreferences 存储最近 5 条翻译记录
 */
class TranslationHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "translation_history",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HISTORY = "recent_translations"
        private const val MAX_HISTORY_SIZE = 5
    }

    /**
     * 添加新的翻译记录
     */
    fun addTranslation(original: String, translated: String) {
        val history = getHistory().toMutableList()

        // 添加到列表开头
        val newItem = RecentTranslation(original, translated)
        history.add(0, newItem)

        // 限制最多 5 条
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        saveHistory(history)
    }

    /**
     * 获取所有历史记录
     */
    fun getHistory(): List<RecentTranslation> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonString)
            List(jsonArray.length()) { i ->
                RecentTranslation.fromJson(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清空历史记录
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    /**
     * 保存历史记录
     */
    private fun saveHistory(history: List<RecentTranslation>) {
        val jsonArray = JSONArray()
        history.forEach { jsonArray.put(it.toJson()) }

        prefs.edit()
            .putString(KEY_HISTORY, jsonArray.toString())
            .apply()
    }
}
