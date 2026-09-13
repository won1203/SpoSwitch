package com.example.sposwitch.feature.weather

import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.model.WeatherScene
import com.example.sposwitch.ui.component.choiceChips

internal object WeatherScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text("날씨가 바뀌면\n스위치가 알려드려요", 27, bold = true))
        ui.add(content, ui.text("아래에서 날씨 변화와 알림 흐름을 체험해 보세요.", 14, ui.muted), top = 12)
        ui.gap(content, 22)
        choiceChips(WeatherScene.entries.map { it.label }, state.weatherScene.label) { label ->
            state.weatherScene = WeatherScene.entries.first { it.label == label }
            rerender()
        }
        ui.gap(content, 22)
        if (recommendsIndoor) {
            val box = ui.column(18).apply { background = ui.surface() }
            ui.add(box, ui.text(if (state.weatherScene == WeatherScene.RAIN) "비가 내려요" else "미세먼지가 나빠요", 21, bold = true))
            ui.add(box, ui.text("근처 실내 체육시설과 오늘의 운동을 확인하세요.", 15), top = 12)
            ui.add(box, ui.text("스포스위치 · 체험 알림", 12, ui.muted), top = 12)
            ui.add(box, ui.button("실내 추천 확인하기") { navigate(AppRoute.WEATHER_SWITCH) }, top = 20)
            ui.add(content, box)
        } else {
            ui.note(content, "지금은 야외 운동하기 좋은 날씨예요.\n새로운 스위칭 알림이 없어요.", R.drawable.ic_sunny)
            ui.add(content, ui.button("오늘의 홈 보기") { navigate(AppRoute.HOME, asTab = true) }, top = 20)
        }
        ui.add(content, ui.text("선택한 날씨는 홈에도 반영됩니다. 실제 알림을 발송하거나 기상 정보를 조회하지 않습니다.", 12, ui.muted), top = 20)
    }
}
