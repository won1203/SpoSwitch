package com.example.sposwitch.feature.facility

import com.example.sposwitch.R
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.model.WeatherScene
import com.example.sposwitch.ui.component.facilityMap
import com.example.sposwitch.ui.component.facilityRow

internal object WeatherSwitchScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.note(
            content,
            if (state.weatherScene == WeatherScene.RAIN) "비가 내려 실내 운동으로 바꿨어요." else "미세먼지가 나빠 실내 운동을 추천해요.",
            state.weatherScene.icon,
        )
        ui.gap(content, 20)
        ui.add(content, ui.text("장소를 바꿔도,\n운동은 계속", 28, bold = true))
        ui.add(content, ui.text("${state.location}\n가까운 실내 공간을 모았어요.", 14, ui.muted), top = 10)
        ui.gap(content, 20)
        facilityMap("실내")
        ui.gap(content, 22)
        ui.section(content, "반경 3km 실내 시설")
        facilityRow(1)
        facilityRow(2)
        ui.gap(content, 16)
        ui.section(content, "함께 하면 좋은 운동")
        ui.add(content, ui.button(MockContent.exercise(true, state.goal), false, R.drawable.ic_play_arrow) {
            openPrescription(true)
        }, top = 12)
        ui.add(content, ui.text("${MockContent.fitnessFactors(state.goal)} · 20분 · 예시 처방", 13, ui.muted), top = 10)
    }
}
