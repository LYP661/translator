package com.momenta.translator.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.momenta.translator.databinding.ActivityCropImageBinding
import java.io.File
import java.io.FileOutputStream

class CropImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageBinding
    private var originalBitmap: Bitmap? = null

    companion object {
        private const val TAG = "CropImageActivity"
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_CROPPED_IMAGE_PATH = "cropped_image_path"
        const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "✂️ onCreate 开始")
        Toast.makeText(this, "✂️ 打开裁剪页面...", Toast.LENGTH_SHORT).show()

        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "✅ super.onCreate 完成")

            binding = ActivityCropImageBinding.inflate(layoutInflater)
            Log.d(TAG, "✅ binding 创建完成")

            setContentView(binding.root)
            Log.d(TAG, "✅ setContentView 完成")
            Toast.makeText(this, "✅ 界面加载完成", Toast.LENGTH_SHORT).show()

            // 加载原始图片
            val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
            Log.d(TAG, "📁 图片路径: $imagePath")

            if (imagePath == null) {
                Log.e(TAG, "❌ 图片路径为空")
                Toast.makeText(this, "❌ 图片路径错误", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val file = File(imagePath)
            Log.d(TAG, "📁 文件存在: ${file.exists()}, 大小: ${file.length()} bytes")
            Toast.makeText(this, "📁 正在加载图片...", Toast.LENGTH_SHORT).show()

            // 使用采样压缩加载图片，避免内存溢出
            originalBitmap = decodeSampledBitmapFromFile(imagePath, 2048, 2048)
            if (originalBitmap == null) {
                Log.e(TAG, "❌ 图片加载失败")
                Toast.makeText(this, "❌ 图片加载失败", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            Log.d(TAG, "✅ 图片加载成功: ${originalBitmap!!.width}x${originalBitmap!!.height}")
            Toast.makeText(this, "✅ 图片加载成功", Toast.LENGTH_SHORT).show()

            binding.ivPhoto.setImageBitmap(originalBitmap)
            Log.d(TAG, "✅ 图片设置到 ImageView")

            // 取消按钮
            binding.btnCancel.setOnClickListener {
                Log.d(TAG, "❌ 用户点击取消")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            // 确认按钮
            binding.btnConfirm.setOnClickListener {
                Log.d(TAG, "✅ 用户点击确认")
                cropAndReturn()
            }

            Log.d(TAG, "✅ CropImageActivity 初始化完成")
            Toast.makeText(this, "✅ 准备就绪，可以裁剪", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ onCreate 异常: ${e.message}", e)
            Toast.makeText(this, "❌ 裁剪页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
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
        return try {
            Log.d(TAG, "📊 开始解析图片尺寸...")
            // 第一次解析，只获取图片尺寸，不加载到内存
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            Log.d(TAG, "📊 原始尺寸: ${options.outWidth}x${options.outHeight}")

            // 计算合适的采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            Log.d(TAG, "📊 采样率: ${options.inSampleSize}")

            // 第二次解析，使用采样率加载压缩后的图片
            options.inJustDecodeBounds = false
            Log.d(TAG, "📊 开始加载图片到内存...")
            val bitmap = BitmapFactory.decodeFile(path, options)
            if (bitmap != null) {
                Log.d(TAG, "✅ 图片加载成功: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.e(TAG, "❌ BitmapFactory.decodeFile 返回 null")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "❌ decodeSampledBitmapFromFile 异常: ${e.message}", e)
            Toast.makeText(this, "❌ 图片解析失败: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
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
