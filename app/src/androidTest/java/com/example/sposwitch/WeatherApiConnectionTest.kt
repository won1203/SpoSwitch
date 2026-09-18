package com.example.sposwitch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sposwitch.data.remote.WeatherApiClient
import com.example.sposwitch.model.CurrentWeather
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in live test: requires a running backend and an accessible configured URL. */
@RunWith(AndroidJUnit4::class)
class WeatherApiConnectionTest {
    @Test
    fun deviceCanFetchWeatherFromConfiguredBackend() {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("verifyLocalBackend") == "true",
        )
        val client = WeatherApiClient()
        val completed = CountDownLatch(1)
        var response: Result<CurrentWeather>? = null
        try {
            client.getCurrentWeather(37.5665, 126.9780) {
                response = it
                completed.countDown()
            }
            assertTrue("기기에서 서버 응답을 받지 못했습니다.", completed.await(25, TimeUnit.SECONDS))
            assertNotNull(response)
            val weather = response!!.getOrThrow()
            assertTrue(weather.location.isNotBlank())
            assertTrue(weather.condition.isNotBlank())
            assertTrue(weather.temperatureC.isFinite())
        } finally {
            client.close()
        }
    }
}
