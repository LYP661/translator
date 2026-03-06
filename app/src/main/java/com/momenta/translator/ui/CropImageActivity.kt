package com.momenta.translator.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.momenta.translator.databinding.ActivityCropImageBinding
import java.io.File
import java.io.FileOutputStream

class CropImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageBinding
    private var originalBitmap: Bitmap? = null

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_CROPPED_IMAGE_PATH = "cropped_image_path"
        const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载原始图片
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            finish()
            return
        }

        originalBitmap = BitmapFactory.decodeFile(imagePath)
        if (originalBitmap == null) {
            finish()
            return
        }

        binding.ivPhoto.setImageBitmap(originalBitmap)

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // 确认按钮
        binding.btnConfirm.setOnClickListener {
            cropAndReturn()
        }
    }

    private fun cropAndReturn() {
        val bitmap = originalBitmap ?: return

        // 获取选择区域的比例
        val cropRatio = binding.cropOverlay.getCropRectRatio()

        // 计算实际裁剪区域（相对于原始bitmap）
        val cropX = (bitmap.width * cropRatio.left).toInt().coerceIn(0, bitmap.width)
        val cropY = (bitmap.height * cropRatio.top).toInt().coerceIn(0, bitmap.height)
        val cropW = (bitmap.width * cropRatio.width()).toInt().coerceAtMost(bitmap.width - cropX)
        val cropH = (bitmap.height * cropRatio.height()).toInt().coerceAtMost(bitmap.height - cropY)

        // 裁剪图片
        val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

        // 保存裁剪后的图片
        val croppedFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // 返回结果
        val resultIntent = Intent().apply {
            putExtra(EXTRA_CROPPED_IMAGE_PATH, croppedFile.absolutePath)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
    }
}
