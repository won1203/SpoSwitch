package com.example.sposwitch.data.remote

import android.os.Handler
import android.os.Looper
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.model.CurrentWeather
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

internal class WeatherApiClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (Result<CurrentWeather>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching {
                val endpoint = baseUrl.trimEnd('/') +
                    "/api/v1/weather/current?latitude=$latitude&longitude=$longitude"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val status = connection.responseCode
                    val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    if (status !in 200..299) {
                        val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                        throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "서버 응답 오류($status)")
                    }
                    parseWeather(JSONObject(responseBody))
                } finally {
                    connection.disconnect()
                }
            }
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        executor.shutdownNow()
    }

    private fun parseWeather(json: JSONObject) = CurrentWeather(
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
}
