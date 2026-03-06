package com.momenta.translator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.momenta.translator.databinding.FragmentTextInputBinding
import com.momenta.translator.viewmodel.TranslateState
import com.momenta.translator.viewmodel.TranslateViewModel

class TextInputFragment : Fragment() {

    private var _binding: FragmentTextInputBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranslateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTranslate.setOnClickListener {
            hideKeyboard()
            viewModel.translateInput(binding.etInput.text.toString())
        }

        binding.btnClear.setOnClickListener {
            binding.etInput.setText("")
            viewModel.reset()
        }

        // 复制翻译结果到剪贴板
        binding.btnCopy.setOnClickListener {
            val text = binding.tvTranslated.text.toString()
            if (text.isNotBlank()) {
                val clipboard = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("translated", text))
                binding.btnCopy.text = "✓ 已复制"
                binding.btnCopy.postDelayed({ binding.btnCopy.text = "复制" }, 1500)
            }
        }

        // 分享翻译结果
        binding.btnShare.setOnClickListener {
            shareTranslation(
                binding.tvOriginal.text.toString(),
                binding.tvTranslated.text.toString()
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TranslateState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardResult.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }
                is TranslateState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.cardResult.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.btnTranslate.isEnabled = false
                }
                is TranslateState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.btnTranslate.isEnabled = true
                    binding.cardResult.visibility = View.VISIBLE
                    binding.tvOriginalLabel.text = "原文"
                    binding.tvOriginal.text = state.original

                    // 显示翻译结果 + 方法提示
                    binding.tvTranslated.text = if (state.method.isNotEmpty()) {
                        "${state.translated}\n\n💡 ${state.method}"
                    } else {
                        state.translated
                    }
                }
                is TranslateState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnTranslate.isEnabled = true
                    binding.cardResult.visibility = View.GONE
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
                else -> { /* OcrDone 不在文字输入流程中 */ }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
    }

    // ─── 分享翻译 ───
    private fun shareTranslation(original: String, translated: String) {
        val cleanTranslated = translated.replace("\n\n💡.*".toRegex(), "")
        val shareText = """
            📖 原文：
            $original

            ❄️ 小冷的翻译：
            $cleanTranslated

            --- 来自小冷翻译 ---
        """.trimIndent()

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }

        startActivity(
            android.content.Intent.createChooser(shareIntent, "分享翻译结果")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
