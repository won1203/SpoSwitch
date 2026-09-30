package com.example.sposwitch.feature.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.sposwitch.MainActivity
import com.example.sposwitch.R

/** 앱 시작 로딩화면. 스플래시 이미지를 잠시 보여준 뒤 MainActivity로 넘어간다. */
class SplashActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goToMain = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val image = ImageView(this).apply {
            setImageResource(R.drawable.splash_image)
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
        }
        setContentView(image)

        image.animate().alpha(1f).setDuration(FADE_IN_MS).start()
        handler.postDelayed(goToMain, SPLASH_DURATION_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goToMain)
        super.onDestroy()
    }

    private companion object {
        const val FADE_IN_MS = 400L
        const val SPLASH_DURATION_MS = 1800L
    }
}
