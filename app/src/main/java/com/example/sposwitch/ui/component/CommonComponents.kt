package com.example.sposwitch.ui.component

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.app.WeatherLoadState
import com.example.sposwitch.model.ExerciseEnvironment

internal fun FeatureUiScope.weatherCard() {
    val box = ui.column().apply {
        background = ui.surface()
        setPadding(ui.dp(14), ui.dp(4), ui.dp(14), ui.dp(12))
    }
    val header = ui.row()
    header.addView(ui.icon(R.drawable.ic_location_on, size = 20))
    header.addView(ui.text(state.location, 15), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(6) })
    header.addView(ui.icon(R.drawable.ic_near_me, size = 20))
    ui.click(header, "현재 위치와 날씨 새로고침") { refreshWeather() }
    ui.add(box, header, 48)

    when (state.weatherLoadState) {
        WeatherLoadState.READY -> {
            val weather = state.weather
            if (weather != null) {
                val body = ui.row()
                val iconColor = if (weather.conditionCode == "CLEAR") 0xFFFFBE2C.toInt() else ui.green
                body.addView(ui.icon(weather.icon, iconColor, 54))
                val details = ui.column()
                val temperature = ui.row()
                temperature.addView(ui.text(weather.temperatureText, 42, bold = true))
                temperature.addView(
                    ui.text(weather.condition, 19, bold = true),
                    LinearLayout.LayoutParams(-2, -2).apply { leftMargin = ui.dp(10) },
                )
                ui.add(details, temperature)
                val currentDetails = if (
                    activity.resources.configuration.screenWidthDp < 360 ||
                    activity.resources.configuration.fontScale > 1.15f
                ) {
                    "습도 ${weather.humidityPercent}%\n풍속 ${weather.windSpeedText}"
                } else {
                    "습도 ${weather.humidityPercent}% · 풍속 ${weather.windSpeedText}"
                }
                ui.add(details, ui.text(currentDetails, 12), top = 5)
                ui.add(details, ui.text(weather.airQualityText, 12), top = 3)
                body.addView(details, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
                ui.add(box, body, top = 2)
            }
        }
        WeatherLoadState.ERROR -> {
            ui.add(box, ui.text(state.weatherError ?: "날씨 정보를 불러오지 못했습니다.", 14, ui.muted), top = 8)
            ui.add(box, ui.button("다시 시도", false, R.drawable.ic_near_me) { refreshWeather() }, top = 12)
            if (state.manualDistrict == null) {
                ui.add(box, ui.button("서울 지역 직접 선택", false, R.drawable.ic_location_on) { chooseDistrict() }, top = 8)
            }
        }
        WeatherLoadState.IDLE, WeatherLoadState.LOADING -> {
            val loading = ui.row().apply {
                addView(ProgressBar(activity).apply { contentDescription = "현재 위치와 날씨 불러오는 중" })
                addView(
                    ui.text("현재 위치와 날씨를 불러오는 중이에요.", 14, ui.muted),
                    LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) },
                )
            }
            ui.add(box, loading, top = 8)
        }
    }
    ui.add(content, box)
}

internal fun FeatureUiScope.profileSetupCard() {
    val card = ui.column(18).apply { background = ui.surface() }
    ui.add(card, ui.text("내 상태를 알려주세요", 23, bold = true))
    ui.add(card, ui.text("국민체력100 운동처방 데이터와 연결될 개인화 기준을 먼저 설정해요.", 14, ui.muted), top = 9)
    listOf(
        "1" to "연령대",
        "2" to "체력 수준",
        "3" to "운동 목적",
        "4" to "운동 장소와 홈 기구",
    ).forEach { (number, label) ->
        val row = ui.row().apply { setPadding(0, ui.dp(12), 0, 0) }
        row.addView(ui.text(number, 12, ui.green, true).apply {
            gravity = Gravity.CENTER
            background = ui.surface(ui.lime, 18)
            minimumWidth = ui.dp(30)
            minimumHeight = ui.dp(30)
        })
        row.addView(ui.text(label, 15, ui.green, true), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
        card.addView(row)
    }
    ui.add(card, ui.button("맞춤 처방 시작하기") {
        state.profileStep = 0
        navigate(AppRoute.PROFILE_SETUP)
    }, top = 20)
    ui.add(content, card)
}

internal fun FeatureUiScope.exerciseHero(environment: ExerciseEnvironment) {
    val frame = FrameLayout(activity).apply {
        background = ui.surface()
        clipToOutline = true
    }
    frame.addView(
        ui.image(if (environment.isIndoor) R.drawable.indoor_hero else R.drawable.track_hero),
        FrameLayout.LayoutParams(-1, -1),
    )
    val overlay = ui.column(14)
    overlay.addView(ui.text("오늘의 ${environment.label}", 11, ui.green, true).apply {
        background = ui.surface(ui.lime, 20)
        setPadding(ui.dp(10), ui.dp(5), ui.dp(10), ui.dp(5))
    }, LinearLayout.LayoutParams(-2, -2))
    val profile = state.profile
    ui.add(overlay, ui.text(profile.goal, 22, ui.white, true).apply {
        setShadowLayer(ui.dp(3).toFloat(), 0f, 1f, 0xFF133B28.toInt())
    }, top = 6)
    ui.add(overlay, ui.text("국민체력100 운동 영상 찾기", 13, ui.white).apply {
        setShadowLayer(ui.dp(3).toFloat(), 0f, 1f, 0xFF133B28.toInt())
    }, top = 5)
    frame.addView(overlay, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
    ui.click(frame, "${profile.goal} 운동 영상 보기") { openPrescription(environment) }
    ui.add(content, frame, 215)
}

internal fun FeatureUiScope.choiceChips(values: List<String>, selected: String, action: (String) -> Unit) {
    val scroller = HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false }
    val row = ui.row()
    values.forEach { value ->
        row.addView(ui.chip(value, value == selected) { action(value) }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = ui.dp(7) })
    }
    scroller.addView(row)
    ui.add(content, scroller)
}

internal fun FeatureUiScope.summaryRow(parent: LinearLayout, label: String, value: String) {
    val row = ui.row()
    row.addView(ui.text(label, 13, ui.muted), LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(ui.text(value, 15, ui.green, true))
    ui.add(parent, row)
}
