package com.example.sposwitch.data.remote

import android.os.Handler
import android.os.Looper
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.model.NearbyFacilities
import com.example.sposwitch.model.NearbyFacility
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

internal class FacilityApiClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** environment: "INDOOR", "OUTDOOR", or null for all. goal: an exercise goal code, or null for every sport. */
    fun getNearby(
        latitude: Double,
        longitude: Double,
        environment: String?,
        goal: String? = null,
        callback: (Result<NearbyFacilities>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching {
                val query = "latitude=$latitude&longitude=$longitude" +
                    (environment?.let { "&environment=$it" } ?: "") +
                    (goal?.let { "&goal=$it" } ?: "")
                val connection = (URL(baseUrl.trimEnd('/') + "/api/v1/facilities?" + query).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val status = connection.responseCode
                    val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    if (status !in 200..299) {
                        val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                        throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "서버 응답 오류($status)")
                    }
                    parse(JSONObject(body))
                } finally {
                    connection.disconnect()
                }
            }.recoverCatching { error ->
                if (error is IOException) {
                    throw IllegalStateException("서버에 연결할 수 없습니다. 연결 상태를 확인한 뒤 다시 시도해 주세요.", error)
                }
                throw error
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun parse(json: JSONObject): NearbyFacilities {
        val items = json.getJSONArray("items")
        return NearbyFacilities(
            totalCount = json.getInt("totalCount"),
            radiusMeters = json.getInt("radiusMeters"),
            dataLoadedAt = json.optString("dataLoadedAt"),
            items = List(items.length()) { index -> parseFacility(items.getJSONObject(index)) },
        )
    }

    private fun parseFacility(json: JSONObject) = NearbyFacility(
        id = json.getString("id"),
        name = json.getString("name"),
        sport = json.text("sport"),
        environment = json.getString("environment"),
        environmentSource = json.getString("environmentSource"),
        ownership = json.text("ownership"),
        operator = json.text("operator"),
        roadAddress = json.text("roadAddress"),
        lotAddress = json.text("lotAddress"),
        phone = json.text("phone"),
        latitude = json.getDouble("latitude"),
        longitude = json.getDouble("longitude"),
        updatedOn = json.text("updatedOn"),
        distanceMeters = json.getInt("distanceMeters"),
    )

    private fun JSONObject.text(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)
}
