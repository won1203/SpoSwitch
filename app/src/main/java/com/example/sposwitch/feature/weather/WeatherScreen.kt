package com.example.sposwitch.feature.weather

import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.ui.component.summaryRow
import com.example.sposwitch.ui.component.weatherCard

internal object WeatherScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text("현재 위치 날씨", 27, bold = true))
        ui.add(content, ui.text("기기의 현재 위치를 기준으로 최신 초단기예보를 확인합니다.", 14, ui.muted), top = 12)
        ui.gap(content, 18)
        weatherCard()

        state.weather?.let { weather ->
            ui.gap(content, 20)
            val details = ui.column(18).apply { background = ui.surface() }
            summaryRow(details, "강수량", weather.precipitationAmount)
            ui.divider(details, 14)
            summaryRow(details, "습도", "${weather.humidityPercent}%")
            ui.divider(details, 14)
            summaryRow(details, "풍속", weather.windSpeedText)
            ui.add(content, details)

            ui.gap(content, 20)
            if (weather.recommendsIndoor) {
                ui.note(content, "${weather.condition} 예보가 있어 실내 운동을 추천해요.", weather.icon)
                ui.add(content, ui.button("실내 추천 확인하기") { navigate(AppRoute.WEATHER_SWITCH) }, top = 16)
            } else {
                ui.note(content, "현재 예보에는 비나 눈이 없어 야외 운동이 가능해요.", weather.icon)
                ui.add(content, ui.button("오늘의 홈 보기") { navigate(AppRoute.HOME, asTab = true) }, top = 16)
            }
        }
        ui.add(content, ui.button("현재 위치 다시 확인", false, R.drawable.ic_near_me) { refreshWeather() }, top = 20)
    }
}
