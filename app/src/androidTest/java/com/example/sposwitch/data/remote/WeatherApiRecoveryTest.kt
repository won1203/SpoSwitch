package com.example.sposwitch.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sposwitch.model.CurrentWeather
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherApiRecoveryTest {
    @Test
    fun slowLocationNameDoesNotBlockFirstWeatherResponse() {
        val locationStarted = CountDownLatch(1)
        val releaseLocation = CountDownLatch(1)
        StubServer { path, _ ->
            if (path.contains("/location?")) {
                locationStarted.countDown()
                releaseLocation.await(10, TimeUnit.SECONDS)
                Reply(200, """{"location":"서울특별시 중구 명동"}""")
            } else Reply(200, weatherJson)
        }.use { server ->
            val client = WeatherApiClient(server.url)
            try {
                client.getCurrentLocationName(37.5665, 126.9780) { }
                assertTrue(locationStarted.await(3, TimeUnit.SECONDS))
                assertEquals("맑음", weather(client, 3).getOrThrow().condition)
                assertEquals(1L, releaseLocation.count)
            } finally {
                releaseLocation.countDown()
                client.close()
            }
        }
    }

    @Test
    fun firstWeatherRequestWaitsForBackendInsteadOfFailingAtTenSeconds() {
        StubServer { _, _ ->
            Thread.sleep(11_000)
            Reply(200, weatherJson)
        }.use { server ->
            val client = WeatherApiClient(server.url)
            try {
                assertEquals("맑음", weather(client, 16).getOrThrow().condition)
                assertEquals(1, server.requests.get())
            } finally { client.close() }
        }
    }

    @Test
    fun transientFirstFailureRecoversWithoutAppRelaunch() {
        StubServer { _, attempt ->
            if (attempt == 1) Reply(503, """{"message":"서버 준비 중"}""") else Reply(200, weatherJson)
        }.use { server ->
            val client = WeatherApiClient(server.url)
            try {
                assertEquals("맑음", weather(client, 5).getOrThrow().condition)
                assertEquals(2, server.requests.get())
            } finally { client.close() }
        }
    }

    @Test
    fun persistentTransientFailureStopsAfterOneRetry() {
        StubServer { _, _ -> Reply(503, """{"message":"서버 준비 중"}""") }.use { server ->
            val client = WeatherApiClient(server.url)
            try {
                val result = weather(client, 5)
                assertTrue(result.isFailure)
                assertEquals("서버 준비 중", result.exceptionOrNull()?.message)
                assertEquals(2, server.requests.get())
            } finally { client.close() }
        }
    }

    @Test
    fun invalidRequestIsReportedWithoutRetry() {
        StubServer { _, _ -> Reply(400, """{"message":"잘못된 좌표"}""") }.use { server ->
            val client = WeatherApiClient(server.url)
            try {
                assertFalse(weather(client, 3).isSuccess)
                assertEquals(1, server.requests.get())
            } finally { client.close() }
        }
    }

    private fun weather(client: WeatherApiClient, timeoutSeconds: Long): Result<CurrentWeather> {
        val completed = CountDownLatch(1)
        var result: Result<CurrentWeather>? = null
        client.getCurrentWeather(37.5665, 126.9780) { result = it; completed.countDown() }
        assertTrue("Weather callback timed out", completed.await(timeoutSeconds, TimeUnit.SECONDS))
        return result!!
    }

    private data class Reply(val status: Int, val body: String)

    private class StubServer(private val reply: (String, Int) -> Reply) : AutoCloseable {
        // This loopback alias stays on the device instead of mapping localhost to the emulator host.
        private val socket = ServerSocket(0, 10, InetAddress.getByName("127.0.0.2"))
        private val workers = Executors.newCachedThreadPool()
        val requests = AtomicInteger()
        val url = "http://127.0.0.2:${socket.localPort}"

        init {
            workers.execute {
                while (!socket.isClosed) {
                    val connection = try { socket.accept() } catch (_: java.io.IOException) { break }
                    workers.execute {
                        try { connection.use {
                            it.soTimeout = 3_000
                            val reader = it.getInputStream().bufferedReader()
                            val path = reader.readLine().split(' ')[1]
                            while (!reader.readLine().isNullOrEmpty()) { }
                            val response = reply(path, requests.incrementAndGet())
                            val bytes = response.body.toByteArray(Charsets.UTF_8)
                            val headers = "HTTP/1.1 ${response.status} Response\r\n" +
                                "Content-Type: application/json\r\nContent-Length: ${bytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                            it.getOutputStream().write(headers.toByteArray(Charsets.US_ASCII))
                            it.getOutputStream().write(bytes)
                            it.getOutputStream().flush()
                        } } catch (_: java.io.IOException) {
                            // The client can close its socket during fixture teardown.
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }
        }

        override fun close() {
            socket.close()
            workers.shutdownNow()
        }
    }

    private val weatherJson = """{"location":"서울특별시 중구 명동","latitude":37.5665,"longitude":126.978,
        "temperatureC":20.0,"condition":"맑음","conditionCode":"CLEAR","humidityPercent":30,
        "windSpeedMps":2.0,"precipitationAmount":"강수없음","forecastAt":"2026-10-02T17:00:00+09:00",
        "source":"테스트"}"""
}
