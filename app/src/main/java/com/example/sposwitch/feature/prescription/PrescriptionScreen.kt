package com.example.sposwitch.feature.prescription

import android.widget.ProgressBar
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.ExerciseLoadState
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseVideo

internal object PrescriptionScreen {
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

        ui.add(content, ui.text("맞춤 운동 처방과 영상", 27, bold = true))
        ui.add(content, ui.text(
            "${profile.age} · ${profile.fitnessLevel} · ${profile.goal} · ${state.prescriptionEnvironment.label}",
            14, ui.muted,
        ), top = 10)
        ui.gap(content, 22)

        when (state.exerciseLoadState) {
            ExerciseLoadState.IDLE, ExerciseLoadState.LOADING -> {
                ui.add(content, ProgressBar(activity), top = 20)
                ui.add(content, ui.text("국민체력100 운동 정보를 불러오는 중입니다.", 14, ui.muted), top = 14)
            }
            ExerciseLoadState.ERROR -> {
                ui.note(content, state.exerciseError ?: "운동 정보를 불러오지 못했습니다.", R.drawable.ic_close)
                ui.add(content, ui.button("다시 시도") { refreshPrescription() }, top = 16)
            }
            ExerciseLoadState.READY -> {
                val result = state.exerciseResult
                if (result == null || result.videos.isEmpty()) {
                    val facilityAlternative = profile.goal == "근력 및 근육 강화" &&
                        state.prescriptionEnvironment != ExerciseEnvironment.INDOOR_FACILITY
                    ui.note(
                        content,
                        if (facilityAlternative) {
                            "${state.prescriptionEnvironment.label} 조건에 맞는 근력 영상을 찾지 못했습니다. 운동 목적은 유지하고 실내 시설 기준으로 다시 찾을 수 있어요. 영상별 준비물을 확인해 주세요."
                        } else {
                            "입력한 조건에 맞고 영상 주소가 있는 운동을 찾지 못했습니다. 운동 장소나 목적을 바꿔 다시 검색해 주세요."
                        },
                        R.drawable.ic_fitness_center,
                    )
                    if (facilityAlternative) {
                        ui.add(content, ui.button("실내 시설 근력 영상 보기") {
                            openPrescription(ExerciseEnvironment.INDOOR_FACILITY)
                        }, top = 16)
                    }
                    ui.add(content, ui.button("조건 수정하기") {
                        state.profileStep = 0
                        navigate(AppRoute.PROFILE_SETUP)
                    }, top = if (facilityAlternative) 10 else 16)
                } else {
                    result.videos.forEachIndexed { index, video -> renderVideo(index, video) }
                    if (state.prescriptionComplete) {
                        ui.note(content, "오늘 운동을 완료했어요.", R.drawable.ic_check)
                    } else {
                        ui.add(content, ui.button("오늘 운동 완료 표시", icon = R.drawable.ic_check) {
                            state.prescriptionComplete = true
                            rerender()
                        }, top = 20)
                    }
                }
                ui.add(content, ui.text(result?.source ?: "국민체력100 · 서울올림픽기념국민체육진흥공단", 12, ui.accent, true), top = 22)
                ui.add(content, ui.text(result?.note.orEmpty(), 12, ui.muted), top = 8)
                ui.add(content, ui.button("운동 목록 새로고침", false, R.drawable.ic_tune) {
                    refreshPrescription()
                }, top = 18)
            }
        }
    }

    private fun FeatureUiScope.renderVideo(index: Int, video: ExerciseVideo) {
        val card = ui.column(16).apply { background = ui.surface() }
        ui.add(card, ui.text("${index + 1}. ${video.title}", 19, ui.green, true))
        val details = listOf(video.category, video.ageGroup, video.place, video.purpose, video.fitnessFactor, video.duration)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (details.isNotBlank()) ui.add(card, ui.text(details, 12, ui.muted), top = 7)
        if (video.description.isNotBlank()) {
            ui.add(card, ui.text(video.description, 14, ui.muted), top = 10)
        }
        if (video.equipment.isNotBlank()) {
            ui.add(card, ui.text("준비물: ${video.equipment}", 13, ui.muted), top = 8)
        }
        ui.add(card, ui.button("운동 영상 보기", icon = R.drawable.ic_play_arrow) {
            openVideo(video)
        }, top = 14)
        ui.add(content, card, top = if (index == 0) 0 else 14)
    }
}
