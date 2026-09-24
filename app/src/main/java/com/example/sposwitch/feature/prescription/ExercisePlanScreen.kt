package com.example.sposwitch.feature.prescription

import android.widget.ProgressBar
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.ExerciseLoadState
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExercisePlanExercise
import com.example.sposwitch.ui.component.choiceChips

internal object ExercisePlanScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        val profile = state.profile
        if (!profile.isComplete) {
            ui.add(content, ui.text("운동 기준을 먼저 입력해 주세요", 28, bold = true))
            ui.add(content, ui.button("내 상태 입력하기") {
                state.profileStep = 0
                navigate(AppRoute.PROFILE_SETUP)
            }, top = 24)
            return@with
        }
        ui.add(content, ui.text("내 운동 계획서", 27, bold = true))
        ui.add(content, ui.text(
            "${profile.age} · ${profile.fitnessLevel} · ${profile.goal} · ${state.prescriptionEnvironment.label}",
            14, ui.muted,
        ), top = 9)
        if (state.prescriptionEnvironment == ExerciseEnvironment.HOME) {
            ui.add(content, ui.text("준비물: ${profile.equipment.label}", 13, ui.muted), top = 6)
        }
        ui.gap(content, 20)

        when (state.planLoadState) {
            ExerciseLoadState.IDLE, ExerciseLoadState.LOADING -> {
                ui.add(content, ProgressBar(activity))
                ui.add(content, ui.text("국민체력100 운동 계획 자료를 불러오는 중입니다.", 14, ui.muted), top = 12)
            }
            ExerciseLoadState.ERROR -> {
                ui.note(content, state.planError ?: "운동 계획서를 불러오지 못했습니다.", R.drawable.ic_close)
                ui.add(content, ui.button("다시 시도") { refreshPlan() }, top = 16)
            }
            ExerciseLoadState.READY -> {
                val plan = state.planResult ?: return@with
                ui.note(content, plan.note, R.drawable.ic_fitness_center)
                ui.gap(content, 22)
                ui.add(content, ui.text("연령대별 표준운동 프로그램", 21, bold = true))
                ui.add(content, ui.text(plan.standardNote, 13, ui.muted), top = 8)
                if (plan.weeks.isNotEmpty()) {
                    val selected = plan.weeks.firstOrNull { it.week == state.selectedPlanWeek } ?: plan.weeks.first()
                    ui.gap(content, 14)
                    choiceChips(plan.weeks.map { "${it.week}주차" }, "${selected.week}주차") { label ->
                        state.selectedPlanWeek = label.removeSuffix("주차").toInt()
                        rerender()
                    }
                    ui.add(content, ui.text(selected.title, 17, ui.green, true), top = 16)
                    if (selected.videoUrl.isNotBlank()) {
                        ui.add(content, ui.button("표준운동 프로그램 영상 보기", icon = R.drawable.ic_play_arrow) {
                            openPlanVideo(selected.title, selected.videoUrl)
                        }, top = 12)
                    }
                    val selectedPhase = selected.phases.firstOrNull { it.name == state.selectedPlanPhase }
                        ?: selected.phases.first()
                    ui.gap(content, 18)
                    choiceChips(selected.phases.map { "${it.name} ${it.exercises.size}개" },
                        "${selectedPhase.name} ${selectedPhase.exercises.size}개") { label ->
                        state.selectedPlanPhase = selected.phases.first { label.startsWith(it.name) }.name
                        rerender()
                    }
                    ui.add(content, ui.text(selectedPhase.name, 17, bold = true), top = 20)
                    selectedPhase.exercises.forEach { exercise -> renderExercise(exercise) }
                }
                ui.add(content, ui.text(
                    "현재 조건에 맞는 영상 ${plan.matchedVideos.size}개는 ‘운동 영상’ 탭에서 볼 수 있습니다.",
                    13, ui.muted,
                ), top = 24)
                ui.add(content, ui.text(plan.source, 12, ui.accent, true), top = 24)
                ui.add(content, ui.button("계획서 새로고침", false, R.drawable.ic_tune) { refreshPlan() }, top = 16)
            }
        }
    }

    private fun FeatureUiScope.renderExercise(exercise: ExercisePlanExercise) {
        val card = ui.column(12).apply { background = ui.surface() }
        ui.add(card, ui.text(exercise.name, 15, ui.green, true))
        val dose = listOf(
            exercise.duration.takeIf(String::isNotBlank)?.let { "시간 $it" },
            exercise.sets.takeIf(String::isNotBlank)?.let { "세트 $it" },
            exercise.repetitions.takeIf(String::isNotBlank)?.let { "반복 $it" },
        ).filterNotNull().joinToString(" · ")
        if (dose.isNotBlank()) ui.add(card, ui.text("원본 표기: $dose", 12, ui.muted), top = 6)
        ui.add(content, card, top = 9)
    }
}
