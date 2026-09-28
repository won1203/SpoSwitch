package com.example.sposwitch.feature.prescription

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sposwitch.R

/** Plays the public exercise file as a stream inside the app. */
class ExerciseVideoActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusView: TextView
    private lateinit var playButton: AppCompatImageButton
    private lateinit var rewindButton: AppCompatImageButton
    private lateinit var forwardButton: AppCompatImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var timeView: TextView
    private lateinit var durationView: TextView
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
    private var mediaPlayer: MediaPlayer? = null
    private var pendingSeekPosition: Int? = null
    private var activeSeekPosition: Int? = null
    private var dragging = false
    private var atEnd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        resumePosition = savedInstanceState?.getInt(STATE_POSITION) ?: 0
        shouldPlay = savedInstanceState?.getBoolean(STATE_PLAYING) ?: true
        atEnd = savedInstanceState?.getBoolean(STATE_AT_END) ?: false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(16), dp(8))
        }
        header.addView(playbackButton(R.drawable.exercise_video_back, "운동 영상으로 돌아가기", false).apply {
            isEnabled = true
            alpha = 1f
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
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
        val timeline = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, dp(24), 0)
        }
        seekBar = SeekBar(this).apply {
            contentDescription = "영상 재생 위치"
            isEnabled = false
            progressTintList = ColorStateList.valueOf(Color.rgb(196, 242, 101))
            thumbTintList = progressTintList
            progressBackgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
            secondaryProgressTintList = ColorStateList.valueOf(Color.GRAY)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) { dragging = true }
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) timeView.text = formatTime(progress)
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    dragging = false
                    if (prepared) seekToPosition(seekBar.progress)
                }
            })
        }
        timeline.addView(seekBar, LinearLayout.LayoutParams(-1, dp(48)))
        val times = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        timeView = playbackTimeView()
        durationView = playbackTimeView().apply { gravity = Gravity.END }
        times.addView(timeView, LinearLayout.LayoutParams(0, -2, 1f))
        times.addView(durationView, LinearLayout.LayoutParams(0, -2, 1f))
        timeline.addView(times)
        root.addView(timeline)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        rewindButton = playbackButton(R.drawable.exercise_video_rewind, "10초 뒤로", false).apply {
            setOnClickListener { seekBy(-SKIP_MILLISECONDS) }
        }
        controls.addView(rewindButton, LinearLayout.LayoutParams(dp(56), dp(56)))
        playButton = playbackButton(R.drawable.exercise_video_play, "재생", true).apply {
            setOnClickListener {
                if (shouldPlay && !atEnd) {
                    shouldPlay = false
                    videoView.pause()
                } else {
                    shouldPlay = true
                    if (atEnd || (pendingSeekPosition ?: videoView.currentPosition) >= videoView.duration) seekToPosition(0)
                    if (activeSeekPosition == null) videoView.start()
                }
                updatePlaybackUi()
            }
        }
        controls.addView(playButton, LinearLayout.LayoutParams(dp(72), dp(72)).apply {
            marginStart = dp(24)
            marginEnd = dp(24)
        })
        forwardButton = playbackButton(R.drawable.exercise_video_forward, "10초 앞으로", false).apply {
            setOnClickListener { seekBy(SKIP_MILLISECONDS) }
        }
        controls.addView(forwardButton, LinearLayout.LayoutParams(dp(56), dp(56)))
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
        videoView.setOnPreparedListener { player ->
            mediaPlayer = player
            player.setOnSeekCompleteListener {
                val completedTarget = activeSeekPosition
                activeSeekPosition = null
                if (pendingSeekPosition != completedTarget) {
                    performPendingSeek()
                } else {
                    pendingSeekPosition = null
                    resumePosition = videoView.currentPosition
                    if (inForeground && shouldPlay && !atEnd) videoView.start()
                }
                updatePlaybackUi()
            }
            prepared = true
            progressBar.visibility = View.GONE
            seekBar.max = videoView.duration.coerceAtLeast(0)
            setControlsEnabled(true)
            if (atEnd) seekToPosition(videoView.duration) else if (resumePosition > 0) seekToPosition(resumePosition)
            if (inForeground && shouldPlay && activeSeekPosition == null) videoView.start()
            updatePlaybackUi()
        }
        videoView.setOnCompletionListener {
            if (pendingSeekPosition?.let { it < videoView.duration } == true) return@setOnCompletionListener
            atEnd = true
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
        if (prepared && shouldPlay && activeSeekPosition == null) videoView.start()
        handler.post(updateProgress)
    }

    override fun onPause() {
        shouldPlay = shouldPlay && !atEnd
        if (prepared) resumePosition = pendingSeekPosition ?: videoView.currentPosition
        inForeground = false
        handler.removeCallbacks(updateProgress)
        videoView.pause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_POSITION, if (prepared) pendingSeekPosition ?: videoView.currentPosition else resumePosition)
        outState.putBoolean(STATE_PLAYING, shouldPlay)
        outState.putBoolean(STATE_AT_END, atEnd)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateProgress)
        mediaPlayer = null
        videoView.stopPlayback()
        super.onDestroy()
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        statusView.text = message
        statusView.visibility = View.VISIBLE
        prepared = false
        pendingSeekPosition = null
        activeSeekPosition = null
        setControlsEnabled(false)
    }

    private fun playbackTimeView(): TextView = TextView(this).apply {
        text = "00:00"
        setTextColor(Color.LTGRAY)
        textSize = 12f
    }

    private fun playbackButton(icon: Int, description: String, primary: Boolean): AppCompatImageButton =
        AppCompatImageButton(this).apply {
            setImageResource(icon)
            contentDescription = description
            imageTintList = ColorStateList.valueOf(if (primary) Color.rgb(8, 61, 46) else Color.WHITE)
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (primary) Color.rgb(196, 242, 101) else Color.rgb(38, 38, 38))
            }
            background = RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), circle, null)
            setPadding(dp(if (primary) 20 else 14), dp(if (primary) 20 else 14),
                dp(if (primary) 20 else 14), dp(if (primary) 20 else 14))
            isEnabled = false
            alpha = 0.35f
        }

    private fun setControlsEnabled(enabled: Boolean) {
        seekBar.isEnabled = enabled
        listOf(rewindButton, playButton, forwardButton).forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.35f
        }
    }

    private fun seekBy(offset: Int) {
        if (!prepared) return
        seekToPosition((pendingSeekPosition ?: videoView.currentPosition) + offset)
    }

    private fun seekToPosition(position: Int) {
        pendingSeekPosition = position.coerceIn(0, videoView.duration.coerceAtLeast(0))
        resumePosition = pendingSeekPosition ?: 0
        atEnd = videoView.duration > 0 && resumePosition >= videoView.duration
        if (atEnd) {
            shouldPlay = false
            videoView.pause()
        }
        if (activeSeekPosition == null) performPendingSeek()
        updatePlaybackUi()
    }

    // Serialize seeks so repeated taps accumulate even while a network video is seeking.
    private fun performPendingSeek() {
        val target = pendingSeekPosition ?: return
        val player = mediaPlayer ?: return
        activeSeekPosition = target
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            player.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            videoView.seekTo(target)
        }
    }

    private fun updatePlaybackUi() {
        if (!prepared) return
        val position = if (atEnd) videoView.duration else pendingSeekPosition ?: videoView.currentPosition
        val duration = videoView.duration.coerceAtLeast(0)
        val playing = shouldPlay && !atEnd
        playButton.setImageResource(if (playing) R.drawable.exercise_video_pause else R.drawable.exercise_video_play)
        playButton.contentDescription = when {
            playing -> "일시정지"
            atEnd -> "다시 재생"
            else -> "재생"
        }
        if (!dragging) {
            seekBar.progress = position
            timeView.text = formatTime(position)
        }
        seekBar.secondaryProgress = (duration.toLong() * videoView.bufferPercentage / 100).toInt()
        durationView.text = formatTime(duration)
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
        private const val SKIP_MILLISECONDS = 10_000
        private const val STATE_AT_END = "playback_at_end"
        private const val STATE_POSITION = "playback_position"
        private const val STATE_PLAYING = "playback_playing"
    }
}
