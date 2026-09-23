package com.example.sposwitch.feature.prescription

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Plays the public exercise file as a stream inside the app. */
class ExerciseVideoActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusView: TextView
    private lateinit var playButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var timeView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgress = object : Runnable {
        override fun run() {
            updatePlaybackUi()
            if (inForeground) handler.postDelayed(this, 500)
        }
    }
    private var prepared = false
    private var inForeground = false
    private var shouldPlay = true
    private var resumePosition = 0
    private var dragging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        resumePosition = savedInstanceState?.getInt(STATE_POSITION) ?: 0
        shouldPlay = savedInstanceState?.getBoolean(STATE_PLAYING) ?: true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(16), dp(8))
        }
        header.addView(Button(this).apply {
            text = "뒤로"
            contentDescription = "운동 처방으로 돌아가기"
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            setTextColor(Color.WHITE)
            textSize = 18f
            maxLines = 2
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(12)
        })
        root.addView(header)

        val playerFrame = FrameLayout(this)
        videoView = VideoView(this).apply {
            keepScreenOn = true
        }
        playerFrame.addView(videoView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        progressBar = ProgressBar(this)
        playerFrame.addView(progressBar, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(16), dp(24), dp(16))
            visibility = View.GONE
        }
        playerFrame.addView(statusView, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        root.addView(playerFrame, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
        }
        playButton = Button(this).apply {
            text = "재생"
            isEnabled = false
            setOnClickListener {
                if (videoView.isPlaying) {
                    shouldPlay = false
                    videoView.pause()
                } else {
                    if (videoView.currentPosition >= videoView.duration - 500) videoView.seekTo(0)
                    shouldPlay = true
                    videoView.start()
                }
                updatePlaybackUi()
            }
        }
        controls.addView(playButton)
        seekBar = SeekBar(this).apply {
            isEnabled = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) { dragging = true }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (prepared) videoView.seekTo(seekBar.progress)
                    dragging = false
                    updatePlaybackUi()
                }
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) timeView.text = "${formatTime(progress)} / ${formatTime(videoView.duration)}"
                }
            })
        }
        controls.addView(seekBar, LinearLayout.LayoutParams(0, -2, 1f))
        timeView = TextView(this).apply {
            text = "00:00 / 00:00"
            setTextColor(Color.WHITE)
            textSize = 12f
        }
        controls.addView(timeView)
        root.addView(controls)
        root.addView(TextView(this).apply {
            text = "영상 제공: 국민체력100 · 서울올림픽기념국민체육진흥공단"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(16))
        })
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        val uri = videoUri(intent.getStringExtra(EXTRA_VIDEO_URL))
        if (uri == null) {
            showError("재생할 영상 주소가 올바르지 않습니다.")
            return
        }
        videoView.setOnPreparedListener {
            prepared = true
            progressBar.visibility = View.GONE
            playButton.isEnabled = true
            seekBar.isEnabled = true
            seekBar.max = videoView.duration
            if (resumePosition > 0) videoView.seekTo(resumePosition)
            if (inForeground && shouldPlay) videoView.start()
            updatePlaybackUi()
        }
        videoView.setOnCompletionListener {
            shouldPlay = false
            updatePlaybackUi()
        }
        videoView.setOnErrorListener { _, _, _ ->
            showError("영상을 재생할 수 없습니다. 네트워크 연결을 확인해 주세요.")
            true
        }
        videoView.setVideoURI(uri)
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        if (prepared && shouldPlay) videoView.start()
        handler.post(updateProgress)
    }

    override fun onPause() {
        shouldPlay = videoView.isPlaying || (shouldPlay && !prepared)
        resumePosition = videoView.currentPosition
        inForeground = false
        handler.removeCallbacks(updateProgress)
        videoView.pause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_POSITION, resumePosition.coerceAtLeast(videoView.currentPosition))
        outState.putBoolean(STATE_PLAYING, shouldPlay || videoView.isPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateProgress)
        videoView.stopPlayback()
        super.onDestroy()
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        statusView.text = message
        statusView.visibility = View.VISIBLE
        playButton.isEnabled = false
        seekBar.isEnabled = false
    }

    private fun updatePlaybackUi() {
        if (!prepared) return
        val position = videoView.currentPosition
        val duration = videoView.duration.coerceAtLeast(0)
        if (!dragging) seekBar.progress = position
        playButton.text = when {
            videoView.isPlaying -> "일시정지"
            duration > 0 && position >= duration - 500 -> "다시 재생"
            else -> "재생"
        }
        if (!dragging) timeView.text = "${formatTime(position)} / ${formatTime(duration)}"
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds.coerceAtLeast(0) / 1000
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun videoUri(url: String?): Uri? {
        if (url.isNullOrBlank()) return null
        val uri = Uri.parse(url)
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) return null
        // The source API returns HTTP links, while the same video host supports HTTPS.
        return if (uri.scheme == "http" && uri.host.equals("openapi.kspo.or.kr", ignoreCase = true)) {
            uri.buildUpon().scheme("https").build()
        } else uri
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_TITLE = "exercise_title"
        const val EXTRA_VIDEO_URL = "exercise_video_url"
        private const val STATE_POSITION = "playback_position"
        private const val STATE_PLAYING = "playback_playing"
    }
}
