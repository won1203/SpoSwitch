package com.example.sposwitch.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

internal class DeviceLocationProvider(context: Context) {
    private val applicationContext = context.applicationContext
    private val locationManager = applicationContext.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val signals = mutableListOf<CancellationSignal>()
    private var timeout: Runnable? = null
    private var requestId = 0

    fun cancel() {
        requestId++
        timeout?.let(handler::removeCallbacks)
        timeout = null
        signals.forEach { it.cancel() }
        signals.clear()
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Result<Location>) -> Unit) {
        cancel()
        val currentRequest = requestId
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            callback(Result.failure(IllegalStateException("기기의 위치 서비스를 켜 주세요.")))
            return
        }

        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { locationManager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            callback(Result.failure(IllegalStateException("사용 가능한 위치 제공자를 찾을 수 없습니다.")))
            return
        }

        fun finish(result: Result<Location>) {
            if (currentRequest != requestId) return
            cancel()
            callback(result)
        }

        fun finishWithRecentLocation() {
            val recent = providers
                .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
                .filter(::isRecent)
                .maxByOrNull { it.elapsedRealtimeNanos }
            finish(
                recent?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("현재 위치를 확인하지 못했습니다. 위치 신호를 확인한 뒤 다시 시도해 주세요.")),
            )
        }

        // A recent device fix is usable immediately while location providers warm up on launch.
        val recent = providers
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .filter(::isRecent)
            .maxByOrNull { it.elapsedRealtimeNanos }
        if (recent != null) {
            finish(Result.success(recent))
            return
        }

        timeout = Runnable { if (currentRequest == requestId) finishWithRecentLocation() }
            .also { handler.postDelayed(it, 30_000L) }

        // Request both sources: an enabled network provider may never produce a fix.
        var pending = providers.size
        providers.forEach { provider ->
            if (currentRequest != requestId) return@forEach
            val signal = CancellationSignal().also(signals::add)
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(applicationContext),
                ) { location ->
                    if (currentRequest != requestId) return@getCurrentLocation
                    if (location != null && isRecent(location)) {
                        finish(Result.success(location))
                    } else if (--pending == 0) {
                        finishWithRecentLocation()
                    }
                }
            } catch (exception: SecurityException) {
                // Approximate permission can still allow the other provider to succeed.
                if (--pending == 0) finishWithRecentLocation()
            } catch (exception: IllegalArgumentException) {
                if (--pending == 0) finishWithRecentLocation()
            }
        }
    }

    private fun isRecent(location: Location): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return ageNanos in 0L..120_000_000_000L
    }
}
