package com.momenta.translator.utils

object FAQHelper {

    data class FAQ(val question: String, val answer: String)

    private val faqList = listOf(
        FAQ(
            question = "小冷能做什么？",
            answer = "小冷是你的智能翻译小助手❄️！我可以帮你：\n\n" +
                    "📷 拍照翻译 - 拍照后框选区域精准翻译\n" +
                    "✍️ 文字翻译 - 快速输入文字翻译\n" +
                    "🎤 语音翻译 - 说话即可翻译\n\n" +
                    "所有功能都在本地处理，完全保护你的隐私哦~"
        ),
        FAQ(
            question = "如何拍照翻译？",
            answer = "拍照翻译很简单哦！\n\n" +
                    "1️⃣ 点击首页的\"拍照翻译\"卡片\n" +
                    "2️⃣ 点击大相机按钮拍照（或从相册选择）\n" +
                    "3️⃣ 拖动边框选择要翻译的区域\n" +
                    "4️⃣ 点击\"确认\"，小冷就开始翻译啦！\n\n" +
                    "💡 小贴士：框选区域可以提高识别准确率~"
        ),
        FAQ(
            question = "语音翻译怎么用？",
            answer = "语音翻译超级方便！\n\n" +
                    "1️⃣ 点击首页的\"语音翻译\"卡片\n" +
                    "2️⃣ 点击麦克风图标\n" +
                    "3️⃣ 首次使用需要授予录音权限\n" +
                    "4️⃣ 对着手机说话\n" +
                    "5️⃣ 小冷会实时识别并自动翻译\n\n" +
                    "🎙️ 支持中文语音识别哦~"
        ),
        FAQ(
            question = "小冷支持哪些语言？",
            answer = "目前小冷支持：\n\n" +
                    "🌏 中文 ↔️ 英语\n" +
                    "🌏 中文 ↔️ 日语\n" +
                    "🌏 中文 ↔️ 韩语\n\n" +
                    "小冷使用 ML Kit 翻译模型，完全离线可用！\n\n" +
                    "📢 未来版本会支持更多语言互译，敬请期待~"
        ),
        FAQ(
            question = "翻译结果可以分享吗？",
            answer = "当然可以！\n\n" +
                    "翻译完成后，你可以：\n" +
                    "✅ 点击\"复制\"按钮复制翻译结果\n" +
                    "✅ 点击\"分享\"按钮分享到其他应用\n" +
                    "✅ 在\"历史记录\"页面查看最近的翻译\n\n" +
                    "💾 历史记录最多保存5条，可以一键清空。"
        ),
        FAQ(
            question = "小冷会上传我的数据吗？",
            answer = "绝对不会！❄️\n\n" +
                    "小冷的所有功能都在本地处理：\n" +
                    "🔒 拍照识别 - 本地 ML Kit OCR\n" +
                    "🔒 语音识别 - 本地 Android API\n" +
                    "🔒 翻译 - 本地 ML Kit 模型\n" +
                    "🔒 历史记录 - 仅存储在你的手机上\n\n" +
                    "零数据上传，零隐私泄露！\n你的隐私安全是小冷最重视的~"
        ),
        FAQ(
            question = "OCR识别不准确怎么办？",
            answer = "如果识别不太准确，可以试试这些方法：\n\n" +
                    "📸 拍照时保持手机稳定\n" +
                    "💡 确保光线充足\n" +
                    "🎯 使用裁剪功能只选择需要的区域\n" +
                    "📐 尽量让文字水平、清晰\n\n" +
                    "小冷会不断学习，识别会越来越准哦~"
        ),
        FAQ(
            question = "小冷还有哪些功能？",
            answer = "除了翻译，小冷还有：\n\n" +
                    "📚 历史记录 - 查看最近的翻译\n" +
                    "🤖 智能助手 - 就是现在和你聊天的我~\n" +
                    "⚙️ 设置页面 - 个性化你的小冷\n\n" +
                    "未来还会有更多功能：\n" +
                    "🔊 语音朗读\n" +
                    "🎨 多主题切换\n" +
                    "📊 更强大的历史记录\n\n" +
                    "敬请期待 v1.4+ 版本！"
        )
    )

    private val quickQuestions = listOf(
        "小冷能做什么？",
        "如何拍照翻译？",
        "语音翻译怎么用？",
        "支持哪些语言？",
        "隐私安全吗？"
    )

    fun getQuickQuestions(): List<String> = quickQuestions

    fun getAnswer(question: String): String {
        // 精确匹配
        val exactMatch = faqList.find { it.question == question }
        if (exactMatch != null) {
            return exactMatch.answer
        }

        // 模糊匹配
        val fuzzyMatch = faqList.find { faq ->
            question.contains(faq.question) || faq.question.contains(question)
        }
        if (fuzzyMatch != null) {
            return fuzzyMatch.answer
        }

        // 关键词匹配
        return when {
            question.contains("拍照") || question.contains("相机") || question.contains("OCR") ->
                faqList.find { it.question.contains("拍照翻译") }?.answer

            question.contains("语音") || question.contains("说话") || question.contains("麦克风") ->
                faqList.find { it.question.contains("语音翻译") }?.answer

            question.contains("语言") || question.contains("支持") || question.contains("翻译成") ->
                faqList.find { it.question.contains("语言") }?.answer

            question.contains("分享") || question.contains("复制") || question.contains("历史") ->
                faqList.find { it.question.contains("分享") }?.answer

            question.contains("隐私") || question.contains("安全") || question.contains("数据") || question.contains("上传") ->
                faqList.find { it.question.contains("数据") }?.answer

            question.contains("不准") || question.contains("识别") || question.contains("错误") ->
                faqList.find { it.question.contains("不准确") }?.answer

            question.contains("功能") || question.contains("还有") || question.contains("什么") ->
                faqList.find { it.question.contains("哪些功能") }?.answer

            else -> "嗯...小冷不太理解你的问题呢~❄️\n\n" +
                    "你可以试试问我：\n" +
                    "• 小冷能做什么？\n" +
                    "• 如何使用拍照翻译？\n" +
                    "• 语音翻译怎么用？\n" +
                    "• 小冷的隐私安全吗？\n\n" +
                    "或者点击下方的快捷问题按钮哦~"
        } ?: "小冷暂时还不知道怎么回答呢~😊"
    }
}
