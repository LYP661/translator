package com.momenta.translator.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.momenta.translator.databinding.FragmentCameraBinding
import com.momenta.translator.utils.TTSHelper
import com.momenta.translator.viewmodel.TranslateState
import com.momenta.translator.viewmodel.TranslateViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranslateViewModel by activityViewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var ttsHelper: TTSHelper? = null

    companion object {
        private const val TAG = "CameraFragment"
    }

    // ─── 权限请求 ───
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // ⚠️ 检查 Fragment 是否仍然存活
        if (_binding == null) return@registerForActivityResult

        if (granted) {
            startCamera()
        } else {
            showError("需要相机权限才能使用此功能")
            // 禁用拍照按钮，只允许从相册选择
            binding.btnCapture.isEnabled = false
            binding.btnGallery.isEnabled = true
        }
    }

    // ─── 相册图片选择 ───
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            var bitmap: Bitmap? = null
            try {
                bitmap = uriToBitmap(it)
                // 保存到临时文件，然后进入裁剪页面
                val tempFile = saveBitmapToTempFile(bitmap)
                startCropActivity(tempFile.absolutePath)
            } catch (e: Exception) {
                showError("读取图片失败: ${e.message}")
            } finally {
                // ⚠️ 关键修复：释放 bitmap 内存
                bitmap?.recycle()
            }
        }
    }

    // ─── 图片裁剪结果 ───
    private val cropImageResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedImagePath = result.data?.getStringExtra(CropImageActivity.EXTRA_CROPPED_IMAGE_PATH)
            if (croppedImagePath != null) {
                // ⚠️ 关键修复：采样加载裁剪后的图片
                val bitmap = decodeSampledBitmapFromFile(croppedImagePath, 2048, 2048)
                if (bitmap != null) {
                    viewModel.recognizeAndTranslate(bitmap)
                } else {
                    showError("无法加载裁剪后的图片")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        ttsHelper = TTSHelper(requireContext())

        if (hasCameraPermission()) startCamera() else requestPermission()

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { pickFromGallery() }
        binding.btnRetry.setOnClickListener {
            viewModel.reset()
            showCameraView()
        }
        binding.btnSpeak.setOnClickListener {
            val translated = binding.tvTranslated.text.toString()
                .replace("\n\n💡.*".toRegex(), "") // 移除方法提示
            ttsHelper?.speak(translated)
        }
        binding.btnShare.setOnClickListener {
            shareTranslation(
                binding.tvOriginal.text.toString(),
                binding.tvTranslated.text.toString()
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TranslateState.Idle    -> showCameraView()
                is TranslateState.Loading -> showLoading("小冷在识别...")
                is TranslateState.OcrDone -> showLoading("小冷在翻译啦...")
                is TranslateState.Success -> showResult(state.original, state.translated, state.method)
                is TranslateState.Error   -> showError(state.message)
            }
        }
    }

    // ─── CameraX 启动 ───
    private fun startCamera() {
        Log.d(TAG, "🎬 startCamera() 开始")
        Toast.makeText(context, "🎬 启动相机...", Toast.LENGTH_SHORT).show()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // ⚠️ 检查 Fragment 是否仍然存活
            if (_binding == null) {
                Log.e(TAG, "❌ startCamera: _binding 为 null")
                return@addListener
            }

            try {
                val provider = cameraProviderFuture.get()
                Log.d(TAG, "✅ 获取到 CameraProvider")

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                Log.d(TAG, "✅ Preview 创建成功")

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                Log.d(TAG, "✅ ImageCapture 创建成功")

                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                Log.d(TAG, "✅ 相机绑定成功")
                Toast.makeText(context, "✅ 相机就绪", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 相机启动失败: ${e.message}", e)
                Toast.makeText(context, "❌ 相机启动失败: ${e.message}", Toast.LENGTH_LONG).show()

                // 相机初始化失败时显示错误
                if (_binding != null) {
                    showError("相机启动失败: ${e.message}")
                    binding.btnCapture.isEnabled = false
                    binding.btnGallery.isEnabled = true
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ─── 拍照 ───
    private fun takePhoto() {
        Log.d(TAG, "📸 takePhoto() 开始")
        Toast.makeText(context, "📸 开始拍照...", Toast.LENGTH_SHORT).show()

        // 检查相机是否就绪
        val capture = imageCapture ?: run {
            Log.e(TAG, "❌ imageCapture 为 null")
            Toast.makeText(context, "❌ 相机未就绪", Toast.LENGTH_SHORT).show()
            showError("相机未就绪，请稍后再试")
            return
        }
        Log.d(TAG, "✅ imageCapture 已就绪")

        // 检查 Fragment 是否存活
        val ctx = context
        if (ctx == null || _binding == null) {
            Log.e(TAG, "❌ context=$ctx, _binding=$_binding")
            Toast.makeText(context, "❌ Fragment 状态异常", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "✅ Fragment 状态正常")

        try {
            // ✅ 直接保存到文件，避免 YUV 转 Bitmap 的内存问题
            val photoFile = File(
                ctx.cacheDir,
                "photo_${System.currentTimeMillis()}.jpg"
            )
            Log.d(TAG, "📁 文件路径: ${photoFile.absolutePath}")
            Toast.makeText(ctx, "📁 准备保存照片...", Toast.LENGTH_SHORT).show()

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            Log.d(TAG, "📷 调用 takePicture...")
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(ctx),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d(TAG, "✅ 照片保存成功: ${photoFile.absolutePath}")
                        Toast.makeText(context, "✅ 照片保存成功！", Toast.LENGTH_SHORT).show()

                        // ⚠️ 检查 Fragment 是否仍然存活
                        if (_binding != null) {
                            Log.d(TAG, "🔄 启动裁剪页面...")
                            startCropActivity(photoFile.absolutePath)
                        } else {
                            Log.e(TAG, "❌ Fragment 已销毁，无法启动裁剪")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "❌ 拍照失败: ${exception.message}", exception)
                        Toast.makeText(context, "❌ 拍照失败: ${exception.message}", Toast.LENGTH_LONG).show()

                        // ⚠️ 检查 Fragment 是否仍然存活
                        if (_binding != null) {
                            showError("拍照失败: ${exception.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ takePhoto 异常: ${e.message}", e)
            Toast.makeText(ctx, "❌ 拍照异常: ${e.message}", Toast.LENGTH_LONG).show()
            if (_binding != null) {
                showError("拍照失败: ${e.message}")
            }
        }
    }

    // ─── 从相册选择 ───
    private fun pickFromGallery() {
        pickImage.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun uriToBitmap(uri: android.net.Uri): Bitmap {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(
                requireContext().contentResolver,
                uri
            )
            // ⚠️ 关键修复：限制图片大小
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val targetSize = 2048
                if (info.size.width > targetSize || info.size.height > targetSize) {
                    val scale = minOf(
                        targetSize.toFloat() / info.size.width,
                        targetSize.toFloat() / info.size.height
                    )
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt(),
                        (info.size.height * scale).toInt()
                    )
                }
            }
        } else {
            // Android 8.0 以下：使用采样加载
            // 第一次：读取图片尺寸
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: throw Exception("无法打开图片")

            // 第二次：采样加载图片
            val newStream = requireContext().contentResolver.openInputStream(uri)
            options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
            options.inJustDecodeBounds = false

            newStream?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options) ?: throw Exception("无法加载图片")
            } ?: throw Exception("无法打开图片")
        }
    }

    // ─── UI 状态切换 ───
    private fun showCameraView() {
        binding.previewView.visibility = View.VISIBLE
        binding.buttonLayout.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
    }

    private fun showLoading(msg: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = msg
        binding.tvStatus.visibility = View.VISIBLE
        binding.buttonLayout.visibility = View.GONE
        binding.cardResult.visibility = View.GONE
    }

    private fun showResult(original: String, translated: String, method: String = "") {
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.previewView.visibility = View.GONE
        binding.buttonLayout.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE
        binding.tvOriginal.text = original

        // 显示翻译结果 + 方法提示
        binding.tvTranslated.text = if (method.isNotEmpty()) {
            "$translated\n\n💡 $method"
        } else {
            translated
        }

        binding.btnRetry.visibility = View.VISIBLE
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "⚠ $msg"
        binding.tvStatus.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestPermission() = permissionLauncher.launch(Manifest.permission.CAMERA)

    // ─── 保存Bitmap到临时文件 ───
    private fun saveBitmapToTempFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return tempFile
    }

    // ─── 启动裁剪页面 ───
    private fun startCropActivity(imagePath: String) {
        val intent = Intent(requireContext(), CropImageActivity::class.java).apply {
            putExtra(CropImageActivity.EXTRA_IMAGE_PATH, imagePath)
        }
        cropImageResult.launch(intent)
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
        cameraExecutor.shutdown()
        ttsHelper?.shutdown()
        ttsHelper = null
        _binding = null
    }

    /**
     * 采样加载图片文件（与 CropImageActivity 相同的实现）
     */
    private fun decodeSampledBitmapFromFile(
        path: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(path, options)
    }

    /**
     * 计算图片采样率（与 CropImageActivity 相同的实现）
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
