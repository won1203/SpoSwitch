package com.example.sposwitch.feature.profile

import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.ui.component.profileSetupCard
import com.example.sposwitch.ui.component.settingToggle
import com.example.sposwitch.ui.component.summaryRow

internal object ProfileScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text(if (state.profileComplete) "내 운동 기준" else "내 상태를 입력하고\n맞춤 처방을 받아보세요", 28, bold = true))
        ui.add(content, ui.text("입력 정보는 현재 실행 중인 목업에만 유지됩니다.", 14, ui.muted), top = 12)
        ui.gap(content, 24)

        if (state.profileComplete) {
            val card = ui.column(18).apply { background = ui.surface() }
            summaryRow(card, "성별 · 연령대", "${state.gender} · ${state.age}")
            ui.divider(card, 14)
            summaryRow(card, "신체 정보", "${state.heightCm}cm · ${state.weightKg}kg")
            ui.divider(card, 14)
            summaryRow(card, "운동 목표", state.goal)
            ui.add(card, ui.text("처방 매칭 · ${MockContent.fitnessFactors(state.goal)}", 13, ui.accent, true), top = 8)
            ui.add(content, card)
            ui.add(content, ui.button("내 상태 수정하기", false, R.drawable.ic_tune) {
                state.profileStep = 0
                navigate(AppRoute.PROFILE_SETUP)
            }, top = 20)
            ui.add(content, ui.button("맞춤 운동 처방 보기", icon = R.drawable.ic_fitness_center) {
                openPrescription(recommendsIndoor)
            }, top = 12)
        } else {
            profileSetupCard()
        }

        ui.gap(content, 26)
        ui.section(content, "내 운동 동네")
        ui.add(content, ui.button(state.location, false, R.drawable.ic_location_on) { chooseLocation() }, top = 12)
        ui.gap(content, 20)
        settingToggle("날씨 변화 알림", state.notificationsEnabled) { state.notificationsEnabled = it }
        ui.add(content, ui.text("목업 설정이며 기기 알림 권한은 변경하지 않습니다.", 12, ui.muted), top = 8)
    }
}
