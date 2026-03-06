# 🗺️ 小冷翻译 - 功能路线图

当前版本：**v1.1** (增强版 - 开发中)
上一版本：**v1.0** (基础版 - 已完成)
状态：🚧 开发中

---

## ✅ v1.0 - 基础版（已完成）

### 核心功能
- [x] 拍照OCR识别文字
- [x] ML Kit 设备端文字识别（拉丁/中文/日/韩文）
- [x] 自动翻译成中文（离线可用）
- [x] 手动文字输入翻译
- [x] 一键复制翻译结果
- [x] 底部导航切换
- [x] 完全本地处理（隐私保护）

### 技术栈
- Kotlin + MVVM
- CameraX
- ML Kit Text Recognition
- ML Kit Translate
- Material Design 3

---

## 🎯 v1.1 - 实用增强版（开发中）

### ✅ 已完成功能

- [x] **品牌升级 - "小冷翻译"**
      实现时间：2026-03-06
      - 新增启动页面（SplashActivity）with 渐变背景和动画
      - 应用名称改为"小冷翻译"
      - 所有UI文案改用温馨友好的"小冷"风格
      - 错误提示人性化（如："小冷没看到文字呢，换个角度试试？"）

- [x] **Plan C: 混合翻译方案**
      实现时间：2026-03-06
      技术方案：
      - TFLiteTranslator: 预打包轻量模型（完全离线）
      - HybridTranslator: 智能切换 ML Kit ↔ TFLite
      - 翻译策略：优先ML Kit（高质量）→ 失败则降级TFLite（离线）
      - 后台自动升级：启动时尝试下载ML Kit模型
      - 用户体验：启动即可使用，无需等待下载
      - 显示翻译方法：在结果中标注"在线翻译"或"离线翻译"

### 📋 计划功能
- [ ] **从相册选择图片翻译**
      优先级：⭐⭐⭐⭐⭐
      工作量：1-2小时
      实现方式：
      ```kotlin
      // 使用 Activity Result API
      val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
          uri?.let { viewModel.recognizeAndTranslate(it.toBitmap()) }
      }
      ```

- [ ] **分享翻译结果**
      优先级：⭐⭐⭐⭐⭐
      工作量：30分钟
      实现方式：
      ```kotlin
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, "原文: $original\n译文: $translated")
      }
      startActivity(Intent.createChooser(shareIntent, "分享翻译"))
      ```

- [ ] **最近翻译记录**
      优先级：⭐⭐⭐⭐
      工作量：1-2小时
      实现方式：SharedPreferences 存储最近5条
      ```kotlin
      data class RecentTranslation(
          val original: String,
          val translated: String,
          val timestamp: Long
      )
      ```

- [ ] **多语言互译**
      优先级：⭐⭐⭐
      工作量：2-3小时
      实现方式：
      ```kotlin
      // 支持语言：英中日韩法德西俄
      val translators = mapOf(
          "en-zh" to EnglishChineseTranslator,
          "zh-en" to ChineseEnglishTranslator,
          "ja-zh" to JapaneseChineseTranslator
      )
      ```

---

## 🚀 v1.2 - 高级功能版（未来）

### 新增功能
- [ ] **翻译历史数据库**
      优先级：⭐⭐⭐
      工作量：2-3小时
      技术：Room Database
      ```kotlin
      @Entity(tableName = "history")
      data class TranslationHistory(
          @PrimaryKey(autoGenerate = true) val id: Long,
          val originalText: String,
          val translatedText: String,
          val timestamp: Long,
          val isFavorite: Boolean = false
      )
      ```

- [ ] **收藏功能**
      优先级：⭐⭐⭐
      工作量：1小时
      实现：在历史记录基础上添加收藏标记

- [ ] **批量翻译**
      优先级：⭐⭐
      工作量：3-4小时
      实现：选择多张图片，批量OCR+翻译

---

## 💬 v2.0 - AI助手版（探索中）

### 概念功能
- [ ] **AI对话翻译助手**
      优先级：⭐⭐
      工作量：5-10小时
      挑战：
      - 需要集成AI API（GPT/Claude/Gemini）
      - 成本问题（API收费）
      - 网络依赖（违背隐私优先原则）

      替代方案：**本地智能助手**
      ```kotlin
      // 使用ML Kit自然语言API
      class SmartAssistant {
          fun detectLanguage(text: String): String
          fun suggestTranslation(context: String): String
          fun provideExamples(word: String): List<String>
      }
      ```

- [ ] **语音输入/朗读**
      优先级：⭐⭐⭐
      工作量：2-3小时
      技术：Android SpeechRecognizer + TTS
      ```kotlin
      // 语音识别
      val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

      // 文字转语音
      val tts = TextToSpeech(context) { status ->
          if (status == TextToSpeech.SUCCESS) {
              tts.language = Locale.CHINESE
              tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
          }
      }
      ```

- [ ] **实时翻译**
      优先级：⭐
      工作量：10+小时
      技术：CameraX Analysis + 实时OCR
      挑战：
      - 性能问题（实时处理）
      - 电池消耗
      - UI/UX设计复杂

---

## 🎨 UI/UX 改进

- [ ] **深色模式优化**
      优先级：⭐⭐
      工作量：1小时
      Note：Material Design已支持，需微调颜色

- [ ] **动画效果**
      优先级：⭐
      工作量：2-3小时
      - 页面切换动画
      - 翻译进度动画
      - 手势交互

- [ ] **多语言界面**
      优先级：⭐⭐
      工作量：2小时
      支持：中文、英文、日文

---

## 🔧 技术优化

- [ ] **性能优化**
      - 图片压缩
      - 内存管理
      - 启动速度优化

- [ ] **错误处理增强**
      - 更友好的错误提示
      - 离线模式检测
      - 崩溃日志收集（本地）

- [ ] **测试覆盖**
      - 单元测试
      - UI测试
      - 性能测试

---

## 📊 优先级说明

| 优先级 | 说明 | 建议时间线 |
|--------|------|------------|
| ⭐⭐⭐⭐⭐ | 必做，用户强需求 | 立即实现 |
| ⭐⭐⭐⭐ | 应该做，提升体验 | 1-2周内 |
| ⭐⭐⭐ | 可以做，锦上添花 | 1-2月内 |
| ⭐⭐ | 探索性，看情况 | 未来考虑 |
| ⭐ | 低优先级 | 暂不考虑 |

---

## 🎯 近期目标（下个版本）

**推荐先实现这3个功能**（v1.1）：

1. ✅ 从相册选择图片（5⭐）
2. ✅ 分享翻译结果（5⭐）
3. ✅ 最近翻译记录（4⭐）

**预计工作量**：3-5小时
**收益**：显著提升实用性

---

## 📝 实现指南

### 快速开始
```bash
# 1. Fork 或 clone 项目
git clone https://github.com/LYP661/translator.git

# 2. 创建功能分支
git checkout -b feature/gallery-pick

# 3. 实现功能

# 4. 测试
./gradlew test

# 5. 提交
git commit -m "feat: add gallery image picker"
git push origin feature/gallery-pick
```

### 开发规范
- 遵循 Kotlin 编码规范
- 使用 MVVM 架构
- 添加必要的注释
- 保持隐私优先原则
- 所有AI功能必须设备端处理

---

## 🤝 贡献

欢迎提交 PR 实现路线图中的功能！

要求：
1. 功能完整且经过测试
2. 不破坏现有功能
3. 遵循隐私保护原则
4. 代码注释清晰

---

## 📮 反馈

有功能建议？在 GitHub Issues 中告诉我们！

---

**最后更新**: 2026-03-06 23:00
**维护者**: LYP661
**当前版本**: v1.1 (增强版 - 开发中)
