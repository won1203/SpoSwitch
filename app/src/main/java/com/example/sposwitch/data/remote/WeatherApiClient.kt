package com.example.sposwitch.data.remote

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.model.CurrentWeather
import java.net.HttpURLConnection
import java.io.IOException
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.SSLException
import org.json.JSONObject

internal class WeatherApiClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
) {
    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestBaseUrl = if (BuildConfig.DEBUG && isAndroidEmulator()) {
        emulatorLocalBaseUrl(baseUrl)
    } else {
        baseUrl
    }

    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (Result<CurrentWeather>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching {
                // A cold backend can call Kakao, KMA and AirKorea before returning weather.
                parseWeather(requestJson("current", latitude, longitude, 60_000))
            }
            val displayResult = result.recoverCatching { error ->
                if (error is IOException) {
                    throw IllegalStateException("서버에 연결할 수 없습니다. 연결 상태를 확인한 뒤 다시 시도해 주세요.", error)
                }
                throw error
            }
            mainHandler.post { callback(displayResult) }
        }
    }

    fun getCurrentLocationName(
        latitude: Double,
        longitude: Double,
        callback: (Result<String>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching {
                requestJson("location", latitude, longitude, 10_000).getString("location")
            }
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        executor.shutdownNow()
    }

    private fun requestJson(path: String, latitude: Double, longitude: Double, readTimeoutMs: Int): JSONObject {
        val endpoint = requestBaseUrl.trimEnd('/') +
            "/api/v1/weather/$path?latitude=$latitude&longitude=$longitude"
        for (attempt in 0..1) {
            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7_000
                    readTimeout = readTimeoutMs
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val status = connection.responseCode
                    val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) {
                        val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                        throw WeatherResponseException(status,
                            message?.takeIf(String::isNotBlank) ?: "서버 응답 오류($status)")
                    }
                    return JSONObject(body)
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                val retryable = when (error) {
                    is WeatherResponseException -> error.status in setOf(408, 502, 503, 504)
                    is IOException -> error !is SSLException
                    else -> false
                }
                if (!retryable || attempt == 1 || Thread.currentThread().isInterrupted) throw error
                Thread.sleep(1_000)
            }
        }
        error("Weather request did not complete")
    }

    private class WeatherResponseException(val status: Int, message: String) : IllegalStateException(message)

    private fun parseWeather(json: JSONObject): CurrentWeather {
        val air = json.optJSONObject("airQuality")
        return baseWeather(json).copy(
            airStation = air?.optString("stationName")?.takeIf(String::isNotBlank),
            pm10Grade = air?.takeUnless { it.isNull("pm10Grade") }?.getInt("pm10Grade"),
            pm25Grade = air?.takeUnless { it.isNull("pm25Grade") }?.getInt("pm25Grade"),
        )
    }

    private fun baseWeather(json: JSONObject) = CurrentWeather(
        location = json.getString("location"),
        latitude = json.getDouble("latitude"),
        longitude = json.getDouble("longitude"),
        temperatureC = json.getDouble("temperatureC"),
        condition = json.getString("condition"),
        conditionCode = json.getString("conditionCode"),
        humidityPercent = json.getInt("humidityPercent"),
        windSpeedMps = json.getDouble("windSpeedMps"),
        precipitationAmount = json.getString("precipitationAmount"),
        forecastAt = json.getString("forecastAt"),
        source = json.getString("source"),
    )

    private fun isAndroidEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.HARDWARE.equals("goldfish", ignoreCase = true) ||
            Build.HARDWARE.equals("ranchu", ignoreCase = true)

    private fun emulatorLocalBaseUrl(url: String): String = url.replace(
        Regex("(?<=//)(localhost|127\\.0\\.0\\.1)(?=[:/]|$)"),
        "10.0.2.2",
    )
}
