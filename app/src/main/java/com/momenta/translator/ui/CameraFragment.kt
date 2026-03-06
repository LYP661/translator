package com.momenta.translator.ui

import android.Manifest
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
import com.momenta.translator.viewmodel.TranslateState
import com.momenta.translator.viewmodel.TranslateViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranslateViewModel by activityViewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    // ─── 权限请求 ───
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showError("需要相机权限才能使用此功能")
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

        if (hasCameraPermission()) startCamera() else requestPermission()

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnRetry.setOnClickListener {
            viewModel.reset()
            showCameraView()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TranslateState.Idle    -> showCameraView()
                is TranslateState.Loading -> showLoading("识别中...")
                is TranslateState.OcrDone -> showLoading("翻译中（OCR: ${state.text.take(30)}...）")
                is TranslateState.Success -> showResult(state.original, state.translated)
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
                        viewModel.recognizeAndTranslate(bitmap)
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

    // ─── UI 状态切换 ───
    private fun showCameraView() {
        binding.previewView.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
    }

    private fun showLoading(msg: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = msg
        binding.tvStatus.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.GONE
        binding.cardResult.visibility = View.GONE
    }

    private fun showResult(original: String, translated: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.previewView.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE
        binding.tvOriginal.text = original
        binding.tvTranslated.text = translated
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

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
