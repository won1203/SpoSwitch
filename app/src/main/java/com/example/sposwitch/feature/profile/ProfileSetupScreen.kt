package com.example.sposwitch.feature.profile

import android.content.res.ColorStateList
import android.widget.ProgressBar
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.domain.RecommendationPolicy
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.ui.component.choiceChips

internal object ProfileSetupScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        if (!state.profile.hasSupportedAge) state.profileStep = 0
        ui.add(content, ui.text("${state.profileStep + 1} / 4  ·  맞춤 운동 처방 설정", 13, ui.accent, true))
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 4
            progress = state.profileStep + 1
            progressTintList = ColorStateList.valueOf(ui.accent)
            contentDescription = "맞춤 설정 ${state.profileStep + 1}단계 중 4단계"
        }
        ui.add(content, progress, top = 10)
        ui.gap(content, 22)

        when (state.profileStep) {
            0 -> renderAge()
            1 -> renderFitnessLevel()
            2 -> renderGoal()
            else -> renderExerciseEnvironment()
        }
    }

    private fun FeatureUiScope.renderAge() {
        ui.add(content, ui.text("연령대를\n선택해 주세요", 28, bold = true))
        ui.add(content, ui.text("현재 연령대에 맞는 운동을 찾는 기준이에요.", 14, ui.muted), top = 12)
        if (!state.profile.hasSupportedAge) {
            ui.add(content, ui.text("운동 계획서를 이용하려면 연령대를 다시 선택해 주세요.", 13, ui.accent), top = 12)
        }
        ui.gap(content, 28)
        ui.section(content, "연령대")
        ui.gap(content, 12)
        choiceChips(ProfileOptions.ages, state.profile.age) {
            state.profile = state.profile.copy(age = it)
            rerender()
        }
        ui.add(content, ui.button("체력 수준 선택하기") {
            if (state.profile.hasSupportedAge) {
                state.profileStep = 1
                rerender()
            }
        }.apply {
            isEnabled = state.profile.hasSupportedAge
            alpha = if (isEnabled) 1f else 0.5f
        }, top = 32)
    }

    private fun FeatureUiScope.renderFitnessLevel() {
        ui.add(content, ui.text("현재 체력 수준은\n어느 쪽에 가까운가요?", 28, bold = true))
        ui.add(content, ui.text("정확한 측정값이 없어도 평소 운동 횟수로 쉽게 선택할 수 있어요.", 14, ui.muted), top = 12)
        ui.gap(content, 24)
        ProfileOptions.fitnessLevels.forEachIndexed { index, level ->
            val selected = state.profile.fitnessLevel == level
            val description = ProfileOptions.fitnessLevelDescription(level)
            val option = ui.column(16).apply {
                background = ui.surface(if (selected) ui.lime else ui.pale)
                ui.add(this, ui.text(level, 18, ui.green, true))
                ui.add(this, ui.text(description, 14, if (selected) ui.green else ui.muted), top = 7)
                ui.click(this, "$level, $description${if (selected) ", 선택됨" else ""}") {
                    state.profile = state.profile.copy(fitnessLevel = level)
                    rerender()
                }
            }
            ui.add(content, option, top = if (index == 0) 0 else 10)
        }
        ui.add(content, ui.button("운동 목적 선택하기") {
            state.profileStep = 2
            rerender()
        }, top = 28)
    }

    private fun FeatureUiScope.renderGoal() {
        ui.add(content, ui.text("가장 중요한\n운동 목적은 무엇인가요?", 28, bold = true))
        ui.add(content, ui.text("선택한 목적을 국민체력100 영상 정보와 비교합니다.", 14, ui.muted), top = 12)
        ui.gap(content, 26)
        ui.section(content, "운동 목적")
        ui.gap(content, 12)
        choiceChips(ProfileOptions.goals, state.profile.goal) {
            state.profile = state.profile.copy(goal = it)
            rerender()
        }
        ui.add(content, ui.button("운동 환경 선택하기") {
            state.profileStep = 3
            rerender()
        }, top = 26)
    }

    private fun FeatureUiScope.renderExerciseEnvironment() {
        ui.add(content, ui.text("어디에서\n운동할까요?", 28, bold = true))
        ui.add(content, ui.text("선택한 장소에 맞춰 가능한 운동을 먼저 보여드려요.", 14, ui.muted), top = 12)
        ui.gap(content, 26)
        ui.section(content, "운동 장소")
        ui.gap(content, 12)
        choiceChips(ProfileOptions.exercisePlaces, state.profile.placePreference.label) {
            val place = ExercisePlacePreference.fromStored(it)
            state.profile = state.profile.copy(
                placePreference = place,
                equipment = if (place == ExercisePlacePreference.HOME) {
                    state.profile.equipment
                } else {
                    ExerciseEquipment.NONE
                },
            )
            rerender()
        }
        ui.add(content, ui.text("‘날씨에 맞게 추천’을 선택하면 날씨와 대기 상태가 좋은 날에는 실내·야외 운동을 함께, 악천후나 미세먼지가 나쁜 날에는 실내 시설과 무기구 집 운동을 추천해요.", 12, ui.muted), top = 10)
        if (state.profile.placePreference == ExercisePlacePreference.HOME) {
            ui.gap(content, 24)
            ui.section(content, "집에서 사용할 수 있는 기구")
            ui.gap(content, 12)
            choiceChips(ProfileOptions.equipmentOptions, state.profile.equipment.label) {
                state.profile = state.profile.copy(equipment = ExerciseEquipment.fromStored(it))
                rerender()
            }
        }
        ui.add(content, ui.button("맞춤 처방 확인하기") {
            state.profile = state.profile.copy(isComplete = true)
            saveProfile()
            state.prescriptionEnvironment = RecommendationPolicy
                .recommend(state.profile, state.weather)
                .primaryEnvironment
            navigate(AppRoute.HOME, asTab = true)
        }, top = 26)
    }
}
