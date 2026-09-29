package com.example.sposwitch.feature.profile

import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.domain.RecommendationPolicy
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.ui.component.profileSetupCard
import com.example.sposwitch.ui.component.settingToggle
import com.example.sposwitch.ui.component.summaryRow

internal object ProfileScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        val profile = state.profile
        ui.add(content, ui.text(if (profile.isComplete) "내 운동 기준" else "내 상태를 입력하고\n맞춤 처방을 받아보세요", 28, bold = true))
        ui.add(content, ui.text("입력한 운동 기준은 이 기기에 저장되어 다음 실행에도 유지됩니다.", 14, ui.muted), top = 12)
        ui.gap(content, 24)

        if (profile.isComplete) {
            val card = ui.column(18).apply { background = ui.surface() }
            summaryRow(card, "연령대", profile.age)
            ui.divider(card, 14)
            summaryRow(card, "체력 수준", profile.fitnessLevel)
            ui.divider(card, 14)
            summaryRow(card, "운동 목적", profile.goal)
            ui.divider(card, 14)
            summaryRow(card, "운동 장소", profile.placePreference.label)
            if (profile.placePreference == ExercisePlacePreference.HOME) {
                ui.divider(card, 14)
                summaryRow(card, "사용 기구", profile.equipment.label)
            }
            ui.add(card, ui.text("입력한 기준으로 실제 운동 영상을 조회합니다.", 13, ui.accent, true), top = 8)
            ui.add(content, card)
            ui.add(content, ui.button("내 상태 수정하기", false, R.drawable.ic_tune) {
                state.profileStep = 0
                navigate(AppRoute.PROFILE_SETUP)
            }, top = 20)
            ui.add(content, ui.button("맞춤 운동 영상 보기", icon = R.drawable.ic_fitness_center) {
                openPrescription(RecommendationPolicy.recommend(profile, state.weather).primaryEnvironment)
            }, top = 12)
        } else {
            profileSetupCard()
        }

        ui.gap(content, 26)
        ui.section(content, "내 운동 동네")
        ui.add(content, ui.button(state.location, false, R.drawable.ic_location_on) { chooseDistrict() }, top = 12)
        ui.add(content, ui.text("누르면 서울 지역을 직접 고를 수 있어요.", 12, ui.muted), top = 8)
        if (state.manualDistrict != null) {
            ui.add(content, ui.button("기기의 현재 위치 사용", false, R.drawable.ic_near_me) { useDeviceLocation() }, top = 12)
        }
        ui.gap(content, 20)
        settingToggle("날씨 변화 알림", state.notificationsEnabled) { state.notificationsEnabled = it }
        ui.add(content, ui.text("목업 설정이며 기기 알림 권한은 변경하지 않습니다.", 12, ui.muted), top = 8)
    }
}
