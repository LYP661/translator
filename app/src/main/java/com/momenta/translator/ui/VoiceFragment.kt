package com.momenta.translator.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.momenta.translator.databinding.FragmentVoiceBinding
import com.momenta.translator.viewmodel.TranslateState
import com.momenta.translator.viewmodel.TranslateViewModel

class VoiceFragment : Fragment() {

    private var _binding: FragmentVoiceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranslateViewModel by activityViewModels()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private val pendingRunnables = mutableListOf<Runnable>()

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            showError("需要录音权限才能使用语音翻译")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化语音识别器
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())

        // 录音按钮
        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                checkPermissionAndStart()
            }
        }

        // 复制按钮
        binding.btnCopy.setOnClickListener {
            val text = binding.tvTranslated.text.toString()
            if (text.isNotBlank()) {
                val clipboard = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("translated", text))
                binding.btnCopy.text = "✓ 已复制"

                val resetRunnable = Runnable { _binding?.btnCopy?.text = "复制" }
                pendingRunnables.add(resetRunnable)
                binding.btnCopy.postDelayed(resetRunnable, 1500)
            }
        }

        // 分享按钮
        binding.btnShare.setOnClickListener {
            shareTranslation(
                binding.tvOriginal.text.toString(),
                binding.tvTranslated.text.toString()
            )
        }

        // 观察翻译状态
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
                    binding.tvStatus.text = "小冷在翻译..."
                }
                is TranslateState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.cardResult.visibility = View.VISIBLE
                    binding.tvOriginal.text = state.original
                    binding.tvTranslated.text = state.translated
                }
                is TranslateState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                    binding.cardResult.visibility = View.GONE
                }
                else -> {}
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            hasRecordPermission() -> startVoiceRecognition()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        isRecording = true
        binding.btnRecord.setImageResource(android.R.drawable.ic_media_pause)
        binding.tvStatus.text = "小冷在听... 🎤"
        binding.tvRecognized.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.cardResult.visibility = View.GONE

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _binding?.tvStatus?.text = "请开始说话... 🎤"
            }

            override fun onBeginningOfSpeech() {
                _binding?.tvStatus?.text = "正在听取... 🎤"
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _binding?.tvStatus?.text = "识别中..."
            }

            override fun onError(error: Int) {
                isRecording = false
                // ⚠️ 安全访问 binding，防止 Fragment 销毁后崩溃
                _binding?.btnRecord?.setImageResource(android.R.drawable.ic_btn_speak_now)
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再说一次"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音"
                    else -> "识别失败"
                }
                if (_binding != null) {
                    showError("小冷没听清楚：$errorMsg")
                }
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                // ⚠️ 安全访问 binding，防止 Fragment 销毁后崩溃
                _binding?.btnRecord?.setImageResource(android.R.drawable.ic_btn_speak_now)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && _binding != null) {
                    val recognizedText = matches[0]
                    binding.tvRecognized.text = recognizedText
                    binding.tvRecognized.visibility = View.VISIBLE
                    binding.tvStatus.text = "识别完成，正在翻译..."

                    // 调用翻译
                    viewModel.translateInput(recognizedText)
                } else if (_binding != null) {
                    showError("没有识别到内容")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && _binding != null) {
                    binding.tvRecognized.text = matches[0]
                    binding.tvRecognized.visibility = View.VISIBLE
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun stopRecording() {
        isRecording = false
        binding.btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now)
        binding.tvStatus.text = "点击麦克风开始说话"
        speechRecognizer?.stopListening()
    }

    private fun showError(msg: String) {
        binding.tvStatus.text = "点击麦克风开始说话"
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun shareTranslation(original: String, translated: String) {
        val shareText = """
            📖 我说的话：
            $original

            ❄️ 小冷的翻译：
            $translated

            --- 来自小冷翻译 ---
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "分享翻译结果"))
    }

    private fun hasRecordPermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()

        // 清理所有待执行的回调，防止崩溃
        pendingRunnables.forEach { binding.btnCopy.removeCallbacks(it) }
        pendingRunnables.clear()

        speechRecognizer?.destroy()
        _binding = null
    }
}
