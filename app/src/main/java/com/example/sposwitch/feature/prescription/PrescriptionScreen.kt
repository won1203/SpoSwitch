package com.example.sposwitch.feature.prescription

import android.content.res.ColorStateList
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent

internal object PrescriptionScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        if (!state.profileComplete) {
            ui.add(content, ui.text("아직 맞춤 처방을\n만들기 전이에요", 28, bold = true))
            ui.add(content, ui.text("성별·연령대, 신체 정보와 운동 목표를 입력해 주세요.", 14, ui.muted), top = 12)
            ui.add(content, ui.button("내 상태 입력하기") {
                state.profileStep = 0
                navigate(AppRoute.PROFILE_SETUP)
            }, top = 24)
            return@with
        }

        ui.add(content, ui.image(if (state.prescriptionIndoor) R.drawable.indoor_hero else R.drawable.track_hero), 200)
        ui.gap(content, 20)
        ui.add(content, ui.text("국민체력100 운동처방 데이터 · 연동 화면 예시", 12, ui.accent, true))
        ui.add(content, ui.text(MockContent.exercise(state.prescriptionIndoor, state.goal), 27, bold = true), top = 8)
        ui.add(content, ui.text("${MockContent.fitnessFactors(state.goal)} · 20분 · ${state.gender} ${state.age} 맞춤", 14, ui.muted), top = 10)
        ui.add(content, ui.text("입력 정보 ${state.heightCm}cm · ${state.weightKg}kg", 12, ui.muted), top = 7)
        ui.gap(content, 20)

        if (state.prescriptionComplete) {
            ui.note(content, "오늘도 움직였어요. 수고했어요!", R.drawable.ic_check)
            ui.add(content, ui.button("홈으로 돌아가기") { navigate(AppRoute.HOME, asTab = true) }, top = 20)
        } else {
            renderActivePrescription()
        }
        ui.add(content, ui.text("처방과 영상은 화면 확인용 예시입니다. 국민체력100 API·실제 영상 재생·운동 기록은 연결하지 않았습니다.", 12, ui.muted), top = 16)
    }

    private fun FeatureUiScope.renderActivePrescription() {
        if (state.videoStarted) {
            ui.note(content, "운동 영상 재생 중 · 목업 플레이어", R.drawable.ic_play_arrow)
            val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = 36
                progressTintList = ColorStateList.valueOf(ui.accent)
                contentDescription = "영상 재생 진행률 36퍼센트"
            }
            ui.add(content, progress, top = 12)
            ui.add(content, ui.text("04:18 / 12:00", 12, ui.muted), top = 6)
        }
        ui.gap(content, 18)
        ui.section(content, "처방 구성")
        prescriptionSteps().forEachIndexed { index, (label, time) ->
            val row = ui.row().apply { setPadding(0, ui.dp(16), 0, ui.dp(16)) }
            row.addView(ui.text("0${index + 1}", 15, ui.accent, true))
            row.addView(ui.text(label, 15), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
            row.addView(ui.text(time, 13, ui.muted))
            ui.add(content, row)
            ui.divider(content, 0)
        }
        ui.add(content, ui.button(
            if (state.videoStarted) "운동 완료" else "운동 영상 재생",
            icon = if (state.videoStarted) R.drawable.ic_check else R.drawable.ic_play_arrow,
        ) {
            if (state.videoStarted) state.prescriptionComplete = true else state.videoStarted = true
            rerender()
        }, top = 20)
    }

    private fun FeatureUiScope.prescriptionSteps(): List<Pair<String, String>> = when (state.goal) {
        "근력 및 근육 강화" -> listOf("관절 가동성 준비" to "4분", "스쿼트와 런지" to "12분", "하체 스트레칭" to "4분")
        "체지방 감소" -> listOf("동적 준비 운동" to "4분", "유산소 인터벌" to "12분", "호흡 정리" to "4분")
        "유연성 및 자세 개선" -> listOf("목과 어깨 이완" to "5분", "전신 가동성 운동" to "10분", "천천히 호흡" to "5분")
        else -> listOf(
            "가볍게 몸 풀기" to "5분",
            (if (state.prescriptionIndoor) "편안한 속도로 페달 밟기" else "편안한 속도로 달리기") to "10분",
            "천천히 마무리" to "5분",
        )
    }
}
