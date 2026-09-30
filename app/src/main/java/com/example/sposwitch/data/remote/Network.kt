package com.example.sposwitch.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the device has an internet-capable network right now. Video streams and Kakao map tiles need it,
 * and neither reports a missing connection quickly (VideoView waits about a minute, the map just stays blank).
 */
internal fun Context.isOnline(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
