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

        // 使用采样压缩加载图片，避免内存溢出
        originalBitmap = decodeSampledBitmapFromFile(imagePath, 2048, 2048)
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
        val cropX = (bitmap.width * cropRatio.left).toInt().coerceIn(0, bitmap.width - 1)
        val cropY = (bitmap.height * cropRatio.top).toInt().coerceIn(0, bitmap.height - 1)
        val cropW = (bitmap.width * cropRatio.width()).toInt().coerceIn(1, bitmap.width - cropX)
        val cropH = (bitmap.height * cropRatio.height()).toInt().coerceIn(1, bitmap.height - cropY)

        // 裁剪图片（确保宽高至少为1，避免崩溃）
        val croppedBitmap = try {
            Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        } catch (e: Exception) {
            // 如果裁剪失败，使用原图
            e.printStackTrace()
            bitmap
        }

        // 保存裁剪后的图片
        val croppedFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // 如果裁剪生成了新的bitmap（不是原图），释放它
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
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

    /**
     * 采样压缩加载图片，避免大图片导致内存溢出
     * @param path 图片文件路径
     * @param reqWidth 目标宽度
     * @param reqHeight 目标高度
     */
    private fun decodeSampledBitmapFromFile(
        path: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        // 第一次解析，只获取图片尺寸，不加载到内存
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        // 计算合适的采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // 第二次解析，使用采样率加载压缩后的图片
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    /**
     * 计算图片采样率
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

            // 计算最大的 inSampleSize（2的幂次），保证压缩后的尺寸大于目标尺寸
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
