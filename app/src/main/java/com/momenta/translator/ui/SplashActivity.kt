package com.momenta.translator.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.momenta.translator.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 隐藏系统状态栏，全屏显示
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        startAnimations()
    }

    private fun startAnimations() {
        lifecycleScope.launch {
            // Logo 淡入动画
            binding.ivLogo.alpha = 0f
            binding.ivLogo.animate()
                .alpha(1f)
                .setDuration(800)
                .start()

            delay(300)

            // 欢迎文字淡入 + 从下往上
            binding.tvWelcome.apply {
                alpha = 0f
                translationY = 50f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }

            delay(400)

            // 副标题淡入
            binding.tvSubtitle.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            // 等待动画完成后跳转
            delay(1500)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()

            // 添加淡出过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
