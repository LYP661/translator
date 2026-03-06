package com.momenta.translator.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    // ─── 权限请求 ───
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showError("需要相机权限才能使用此功能")
    }

    // ─── 相册图片选择 ───
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = uriToBitmap(it)
                // 保存到临时文件，然后进入裁剪页面
                val tempFile = saveBitmapToTempFile(bitmap)
                startCropActivity(tempFile.absolutePath)
            } catch (e: Exception) {
                showError("读取图片失败: ${e.message}")
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
                val bitmap = BitmapFactory.decodeFile(croppedImagePath)
                viewModel.recognizeAndTranslate(bitmap)
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ─── 拍照 ───
    private fun takePhoto() {
        val capture = imageCapture ?: return
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    requireActivity().runOnUiThread {
                        // 保存到临时文件，然后进入裁剪页面
                        val tempFile = saveBitmapToTempFile(bitmap)
                        startCropActivity(tempFile.absolutePath)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    requireActivity().runOnUiThread {
                        showError("拍照失败: ${exception.message}")
                    }
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        // 修正旋转
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
            android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                uri
            )
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
}
