package com.example.sposwitch.feature.profile

import android.content.res.ColorStateList
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.ui.component.choiceChips

internal object ProfileSetupScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text("${state.profileStep + 1} / 3  ·  맞춤 운동 처방 설정", 13, ui.accent, true))
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 3
            progress = state.profileStep + 1
            progressTintList = ColorStateList.valueOf(ui.accent)
            contentDescription = "맞춤 설정 ${state.profileStep + 1}단계 중 3단계"
        }
        ui.add(content, progress, top = 10)
        ui.gap(content, 22)

        when (state.profileStep) {
            0 -> renderDemographics()
            1 -> renderBodyMeasurements()
            else -> renderGoal()
        }
    }

    private fun FeatureUiScope.renderDemographics() {
        ui.add(content, ui.text("성별과 연령대를\n선택해 주세요", 28, bold = true))
        ui.add(content, ui.text("운동 처방 데이터의 대상 조건을 맞추는 기준이에요.", 14, ui.muted), top = 12)
        ui.gap(content, 28)
        ui.section(content, "성별")
        ui.gap(content, 12)
        choiceChips(MockContent.genders, state.gender) {
            state.gender = it
            rerender()
        }
        ui.gap(content, 26)
        ui.section(content, "연령대")
        ui.gap(content, 12)
        choiceChips(MockContent.ages, state.age) {
            state.age = it
            rerender()
        }
        ui.add(content, ui.button("신체 정보 입력하기") {
            state.profileStep = 1
            rerender()
        }, top = 32)
    }

    private fun FeatureUiScope.renderBodyMeasurements() {
        ui.add(content, ui.text("키와 몸무게를\n입력해 주세요", 28, bold = true))
        ui.add(content, ui.text("운동 강도와 동작 안내를 개인화할 때 사용하는 정보예요.", 14, ui.muted), top = 12)
        ui.gap(content, 28)
        val heightInput = numberInput("키", "cm", state.heightCm)
        val weightInput = numberInput("몸무게", "kg", state.weightKg)
        ui.add(content, heightInput.first)
        ui.add(content, weightInput.first, top = 14)
        ui.add(content, ui.button("운동 목표 선택하기") {
            val heightValue = heightInput.second.text.toString().trim().toDoubleOrNull()
            val weightValue = weightInput.second.text.toString().trim().toDoubleOrNull()
            var valid = true
            if (heightValue == null || heightValue !in 100.0..230.0) {
                heightInput.second.error = "100~230cm 사이로 입력해 주세요"
                valid = false
            }
            if (weightValue == null || weightValue !in 25.0..250.0) {
                weightInput.second.error = "25~250kg 사이로 입력해 주세요"
                valid = false
            }
            if (valid) {
                state.heightCm = trimNumber(heightValue!!)
                state.weightKg = trimNumber(weightValue!!)
                state.profileStep = 2
                rerender()
            }
        }, top = 28)
    }

    private fun FeatureUiScope.renderGoal() {
        ui.add(content, ui.text("가장 중요한\n운동 목표는 무엇인가요?", 28, bold = true))
        ui.add(content, ui.text("선택한 목표를 국민체력100 체력요인과 연결해 보여줍니다.", 14, ui.muted), top = 12)
        ui.gap(content, 26)
        ui.section(content, "운동 목표")
        ui.gap(content, 12)
        choiceChips(MockContent.goals, state.goal) {
            state.goal = it
            rerender()
        }
        val match = ui.column(14).apply { background = ui.surface() }
        ui.add(match, ui.text("처방 매칭 체력요인", 12, ui.muted))
        ui.add(match, ui.text(MockContent.fitnessFactors(state.goal), 18, ui.green, true), top = 8)
        ui.add(content, match, top = 22)
        ui.add(content, ui.button("맞춤 처방 확인하기") {
            state.profileComplete = true
            state.prescriptionIndoor = recommendsIndoor
            navigate(AppRoute.HOME, asTab = true)
        }, top = 26)
    }

    private fun FeatureUiScope.numberInput(label: String, unit: String, value: String): Pair<LinearLayout, EditText> {
        val box = ui.column(14).apply { background = ui.surface() }
        val row = ui.row()
        row.addView(ui.text(label, 15, ui.green, true), LinearLayout.LayoutParams(0, -2, 1f))
        val input = EditText(activity).apply {
            setText(value)
            textSize = 24f
            setTextColor(ui.green)
            gravity = Gravity.END
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = null
            setSelectAllOnFocus(true)
            minWidth = ui.dp(100)
            contentDescription = "$label 입력"
        }
        row.addView(input, LinearLayout.LayoutParams(ui.dp(120), ui.dp(52)))
        row.addView(ui.text(unit, 14, ui.muted).apply { setPadding(ui.dp(8), 0, 0, 0) })
        box.addView(row)
        return box to input
    }

    private fun trimNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
