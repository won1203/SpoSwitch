package com.example.sposwitch.model

import com.example.sposwitch.R

/** Fixed presentation scenarios used by the frontend-only weather flow. */
internal enum class WeatherScene(
    val label: String,
    val temperature: String,
    val airQuality: String,
    val icon: Int,
) {
    SUNNY("맑음", "24°", "좋음", R.drawable.ic_sunny),
    RAIN("비", "21°", "좋음", R.drawable.ic_rainy),
    DUST("미세먼지", "23°", "나쁨", R.drawable.ic_air),
}
