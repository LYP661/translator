package com.momenta.translator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.momenta.translator.databinding.BottomSheetAssistantBinding

class AssistantBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAssistantBinding? = null
    private val binding get() = _binding!!

    private var onQuickTranslateClick: (() -> Unit)? = null
    private var onVoiceTranslateClick: (() -> Unit)? = null
    private var onChatAssistantClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 关闭按钮
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // 快速拍照翻译
        binding.cardQuickTranslate.setOnClickListener {
            onQuickTranslateClick?.invoke()
            dismiss()
        }

        // 语音翻译
        binding.cardVoiceTranslate.setOnClickListener {
            onVoiceTranslateClick?.invoke()
            dismiss()
        }

        // 和小冷聊聊
        binding.cardChatAssistant.setOnClickListener {
            onChatAssistantClick?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清空回调引用，防止内存泄漏
        onQuickTranslateClick = null
        onVoiceTranslateClick = null
        onChatAssistantClick = null
        _binding = null
    }

    companion object {
        fun newInstance(
            onQuickTranslate: () -> Unit,
            onVoiceTranslate: () -> Unit,
            onChatAssistant: () -> Unit
        ): AssistantBottomSheet {
            return AssistantBottomSheet().apply {
                this.onQuickTranslateClick = onQuickTranslate
                this.onVoiceTranslateClick = onVoiceTranslate
                this.onChatAssistantClick = onChatAssistant
            }
        }
    }
}
