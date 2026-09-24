package com.example.sposwitch.feature.facility

import com.example.sposwitch.R
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent

internal object WeatherSwitchScreen {
    private const val INDOOR_PREVIEW = 3

    fun render(scope: FeatureUiScope) = with(scope) {
        val weather = state.weather
        ui.note(
            content,
            weather?.let { "${it.condition} 예보가 있어 실내 운동으로 바꿨어요." } ?: "현재 날씨에 맞춰 실내 운동을 추천해요.",
            weather?.icon ?: R.drawable.ic_rainy,
        )
        ui.gap(content, 20)
        ui.add(content, ui.text("장소를 바꿔도,\n운동은 계속", 28, bold = true))
        ui.add(content, ui.text("${state.location}\n가까운 실내 공간을 모았어요.", 14, ui.muted), top = 10)
        ui.gap(content, 22)
        ui.section(content, "반경 3km 실내 시설")
        val goal = if (state.profileComplete && FacilityStore.goalOnly) FacilityStore.goalToCode(state.goal) else null
        nearbyFacilities("INDOOR", goal)?.let { result ->
            if (result.items.isEmpty()) {
                ui.add(content, ui.text("반경 3km 안에 실내 시설이 없어요.", 14, ui.muted), top = 8)
            }
            val preview = result.items.take(INDOOR_PREVIEW)
            if (preview.isNotEmpty()) facilityMap(preview, height = 200)
            preview.forEach { nearbyFacilityRow(it) }
        }
        ui.gap(content, 16)
        ui.section(content, "함께 하면 좋은 운동")
        ui.add(content, ui.button(MockContent.exercise(true, state.goal), false, R.drawable.ic_play_arrow) {
            openPrescription(true)
        }, top = 12)
        ui.add(content, ui.text("${MockContent.fitnessFactors(state.goal)} · 20분 · 예시 처방", 13, ui.muted), top = 10)
    }
}
