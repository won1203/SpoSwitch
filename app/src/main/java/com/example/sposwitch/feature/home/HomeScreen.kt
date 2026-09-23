package com.example.sposwitch.feature.home

import android.widget.LinearLayout
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.domain.RecommendationPolicy
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.ExerciseRecommendation
import com.example.sposwitch.model.RecommendationReason
import com.example.sposwitch.ui.component.exerciseHero
import com.example.sposwitch.ui.component.facilityRow
import com.example.sposwitch.ui.component.profileSetupCard
import com.example.sposwitch.ui.component.weatherCard

internal object HomeScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        weatherCard()
        ui.gap(content, 22)
        val profile = state.profile
        if (!profile.isComplete) {
            profileSetupCard()
            ui.gap(content, 16)
            ui.note(content, "상태 입력을 완료하면 운동 처방과 시설 추천이 열려요.", R.drawable.ic_person)
            return@with
        }

        val summary = ui.row().apply {
            background = ui.surface()
            setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
            val exerciseEnvironment = if (profile.placePreference == ExercisePlacePreference.HOME) {
                "${profile.placePreference.label} · ${profile.equipment.label}"
            } else {
                profile.placePreference.label
            }
            addView(
                ui.text("${profile.age} · ${profile.fitnessLevel}\n$exerciseEnvironment", 13, ui.green),
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
        ui.add(content, ui.text("${profile.goal}에 맞는 국민체력100 운동 영상을 찾아요.", 13, ui.muted), top = 8)

        val recommendation = RecommendationPolicy.recommend(profile, state.weather)
        recommendation.environments.forEachIndexed { index, environment ->
            ui.gap(content, if (index == 0) 14 else 20)
            exerciseHero(environment)
            ui.add(content, ui.button("${environment.label} 처방과 영상 보기", icon = R.drawable.ic_play_arrow) {
                openPrescription(environment)
            }, top = 12)
        }

        val facilityEnvironment = recommendation.environments.firstOrNull {
            it == ExerciseEnvironment.INDOOR_FACILITY || it == ExerciseEnvironment.OUTDOOR
        }
        if (facilityEnvironment != null) {
            val indoor = facilityEnvironment == ExerciseEnvironment.INDOOR_FACILITY
            ui.gap(content, 20)
            ui.section(content, if (indoor) "오늘은 가까운 실내 시설" else "오늘은 가까운 야외 시설") {
                state.facilityFilter = if (indoor) "실내" else "야외"
                navigate(AppRoute.FACILITIES)
            }
            facilityRow(if (indoor) 1 else 0, compact = true)
            ui.divider(content, 8)
        }

        ui.gap(content, 10)
        ui.note(
            content,
            recommendationMessage(recommendation),
            state.weather?.icon ?: R.drawable.ic_sunny,
        ) { navigate(AppRoute.WEATHER) }
    }

    private fun recommendationMessage(recommendation: ExerciseRecommendation): String =
        when (recommendation.reason) {
            RecommendationReason.GOOD_WEATHER -> "현재 날씨가 운동하기 좋아 야외 운동과 시설을 추천했어요."
            RecommendationReason.WEATHER_RISK ->
                "${recommendation.weatherRisks.joinToString("·")} 때문에 실내 시설과 무기구 집 운동을 추천했어요."
            RecommendationReason.WEATHER_UNAVAILABLE ->
                "날씨를 확인하기 전에는 실내 시설과 무기구 집 운동을 우선 추천해요."
            RecommendationReason.USER_PREFERENCE ->
                "선택한 운동 장소에 맞춰 ${recommendation.primaryEnvironment.label}을 추천했어요."
        }
}
