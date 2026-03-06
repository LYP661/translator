package com.momenta.translator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.momenta.translator.R
import com.momenta.translator.data.ChatMessage
import com.momenta.translator.databinding.FragmentChatAssistantBinding
import com.momenta.translator.ui.adapter.ChatAdapter
import com.momenta.translator.utils.FAQHelper

class ChatAssistantFragment : Fragment() {

    private var _binding: FragmentChatAssistantBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private val pendingRunnables = mutableListOf<Runnable>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupQuickQuestions()
        setupListeners()
        showWelcomeMessage()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupQuickQuestions() {
        val quickQuestions = FAQHelper.getQuickQuestions()

        quickQuestions.forEach { question ->
            val chip = Chip(requireContext()).apply {
                text = question
                setOnClickListener {
                    sendMessage(question)
                }
                setChipBackgroundColorResource(android.R.color.white)
                chipStrokeWidth = 2f
                setChipStrokeColorResource(android.R.color.holo_blue_light)
            }
            binding.quickQuestionsLayout.addView(chip)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun showWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            content = "你好呀！我是小冷❄️\n\n" +
                    "我可以帮你解答关于翻译功能的各种问题~\n\n" +
                    "试试点击下方的快捷问题，或者直接输入你想问的内容吧！",
            isUser = false
        )
        chatAdapter.addMessage(welcomeMessage)
        scrollToBottom()
    }

    private fun sendMessage(message: String) {
        // 添加用户消息
        val userMessage = ChatMessage(content = message, isUser = true)
        chatAdapter.addMessage(userMessage)
        scrollToBottom()

        // 隐藏快捷问题栏（首次发送后）
        if (chatAdapter.itemCount > 2) {
            binding.quickQuestionsContainer.visibility = View.GONE
        }

        // 延迟显示助手回复（模拟思考）
        val replyRunnable = Runnable {
            if (_binding != null) {
                val answer = FAQHelper.getAnswer(message)
                val assistantMessage = ChatMessage(content = answer, isUser = false)
                chatAdapter.addMessage(assistantMessage)
                scrollToBottom()
            }
        }
        pendingRunnables.add(replyRunnable)
        binding.root.postDelayed(replyRunnable, 500)
    }

    private fun scrollToBottom() {
        val scrollRunnable = Runnable {
            if (_binding != null && chatAdapter.itemCount > 0) {
                binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
        pendingRunnables.add(scrollRunnable)
        binding.rvChatMessages.postDelayed(scrollRunnable, 100)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // 清理所有待执行的回调，防止崩溃和内存泄漏
        pendingRunnables.forEach { binding.root.removeCallbacks(it) }
        pendingRunnables.clear()

        _binding = null
    }
}
