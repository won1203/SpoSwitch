package com.example.sposwitch.feature.home

import android.widget.LinearLayout
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.ui.component.exerciseHero
import com.example.sposwitch.ui.component.facilityRow
import com.example.sposwitch.ui.component.profileSetupCard
import com.example.sposwitch.ui.component.weatherCard

internal object HomeScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        weatherCard()
        ui.gap(content, 22)
        if (!state.profileComplete) {
            profileSetupCard()
            ui.gap(content, 16)
            ui.note(content, "상태 입력을 완료하면 운동 처방과 시설 추천이 열려요.", R.drawable.ic_person)
            return@with
        }

        val summary = ui.row().apply {
            background = ui.surface()
            setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
            addView(
                ui.text("${state.gender} · ${state.age} · ${state.heightCm}cm · ${state.weightKg}kg", 13, ui.green),
                LinearLayout.LayoutParams(0, -2, 1f),
            )
            addView(ui.text("수정", 12, ui.accent, true).apply {
                ui.click(this, "현재 상태 수정") {
                    state.profileStep = 0
                    navigate(AppRoute.PROFILE_SETUP)
                }
            })
        }
        ui.add(content, summary)
        ui.gap(content, 20)
        ui.add(content, ui.text("오늘의 맞춤 운동", 25, bold = true))
        ui.add(content, ui.text("${state.goal} → ${MockContent.fitnessFactors(state.goal)} 처방 매칭", 13, ui.muted), top = 8)
        ui.gap(content, 14)
        exerciseHero(recommendsIndoor)
        ui.add(content, ui.button("운동 처방과 영상 보기", icon = R.drawable.ic_play_arrow) {
            openPrescription(recommendsIndoor)
        }, top = 12)
        ui.gap(content, 20)
        ui.section(content, if (recommendsIndoor) "오늘은 가까운 실내 시설" else "오늘은 가까운 야외 시설") {
            state.facilityFilter = if (recommendsIndoor) "실내" else "야외"
            navigate(AppRoute.FACILITIES)
        }
        facilityRow(if (recommendsIndoor) 1 else 0, compact = true)
        ui.divider(content, 8)
        ui.gap(content, 10)
        ui.note(
            content,
            if (recommendsIndoor) "현재 날씨에 맞춰 실내 시설을 추천했어요." else "현재 날씨에 맞춰 야외 시설을 추천했어요.",
            state.weatherScene.icon,
        ) { navigate(AppRoute.WEATHER) }
    }
}
