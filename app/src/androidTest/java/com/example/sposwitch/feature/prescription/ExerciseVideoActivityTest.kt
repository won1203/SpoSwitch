package com.example.sposwitch.feature.prescription

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseVideoActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun invalidVideoDisablesAllPlaybackControls() {
        val activity = launch("invalid-video-url")
        try {
            onMain {
                listOf("재생", "10초 뒤로", "10초 앞으로", "영상 재생 위치").forEach { description ->
                    assertFalse(find(activity.window.decorView) { it.contentDescription == description }.isEnabled)
                }
            }
        } finally {
            onMain { activity.finish() }
        }
    }

    // Supply -e exerciseVideoUrl <real API video URL> for this streaming integration check.
    @Test
    fun streamingControlsPauseSeekClampAndResume() {
        val url = InstrumentationRegistry.getArguments().getString("exerciseVideoUrl")
        assumeTrue("A real video URL is required for the streaming check", !url.isNullOrBlank())
        val activity = launch(url!!)
        try {
            lateinit var video: VideoView
            waitFor {
                video = find(activity.window.decorView) { it is VideoView } as VideoView
                video.isPlaying
            }
            onMain {
                click(activity, "일시정지")
                assertFalse(video.isPlaying)
                click(activity, "10초 뒤로")
            }
            waitFor { video.currentPosition < 1000 }
            onMain {
                click(activity, "10초 앞으로")
                click(activity, "10초 앞으로")
            }
            waitFor { video.currentPosition in 19_000..21_000 }
            onMain {
                assertFalse("Seeking must keep a paused video paused", video.isPlaying)
                click(activity, "10초 뒤로")
            }
            waitFor { video.currentPosition in 9_000..11_000 }
            onMain {
                val time = find(activity.window.decorView) { it is TextView && it.text == "00:10" }
                assertTrue(time is TextView)
                scrubTo(activity, 30_000)
            }
            waitFor { video.currentPosition in 28_000..32_000 }
            onMain {
                assertFalse("Scrubbing must keep a paused video paused", video.isPlaying)
                click(activity, "재생")
            }
            waitFor { video.isPlaying }
            onMain { click(activity, "일시정지") }
            val pausedPosition = video.currentPosition
            // A seek to the end verifies forward clamping and replay without hundreds of taps.
            onMain { scrubTo(activity, video.duration - 5000) }
            waitFor { video.currentPosition >= video.duration - 8000 }
            onMain { click(activity, "10초 앞으로") }
            waitFor { video.currentPosition >= video.duration - 1000 }
            onMain {
                assertFalse(video.isPlaying)
                click(activity, "다시 재생")
            }
            waitFor { video.isPlaying && video.currentPosition < 3000 }
            onMain {
                click(activity, "일시정지")
                assertTrue(pausedPosition >= 9000)
                click(activity, "10초 앞으로")
            }
            waitFor { video.currentPosition in 9_000..13_000 }
        } finally {
            onMain { activity.finish() }
        }
    }

    private fun launch(url: String): ExerciseVideoActivity =
        instrumentation.startActivitySync(Intent(instrumentation.targetContext, ExerciseVideoActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ExerciseVideoActivity.EXTRA_TITLE, "국민체력100 운동 영상")
            putExtra(ExerciseVideoActivity.EXTRA_VIDEO_URL, url)
        }) as ExerciseVideoActivity

    private fun click(activity: ExerciseVideoActivity, description: String) {
        assertTrue(find(activity.window.decorView) { it.contentDescription == description }.performClick())
    }

    private fun scrubTo(activity: ExerciseVideoActivity, position: Int) {
        val seekBar = find(activity.window.decorView) { it is SeekBar } as SeekBar
        val trackWidth = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
        val startX = seekBar.paddingLeft + trackWidth * seekBar.progress.toFloat() / seekBar.max
        val endX = seekBar.paddingLeft + trackWidth * position.toFloat() / seekBar.max
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN to startX, MotionEvent.ACTION_MOVE to endX, MotionEvent.ACTION_UP to endX)
            .forEachIndexed { index, (action, x) ->
                val event = MotionEvent.obtain(downTime, downTime + index * 16, action, x, seekBar.height / 2f, 0)
                seekBar.dispatchTouchEvent(event)
                event.recycle()
            }
    }

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 60_000
        do {
            var success = false
            onMain { success = condition() }
            if (success) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Playback did not reach the expected state within 60 seconds")
    }

    private fun find(root: View, predicate: (View) -> Boolean): View =
        walk(root).firstOrNull(predicate) ?: throw AssertionError("Playback control was not found")

    private fun walk(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(walk(view.getChildAt(index)))
    }
}
