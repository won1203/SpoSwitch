package com.example.sposwitch.ui.component

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.model.WeatherScene
import com.google.android.material.materialswitch.MaterialSwitch

internal fun FeatureUiScope.weatherCard() {
    val box = ui.column().apply {
        background = ui.surface()
        setPadding(ui.dp(14), ui.dp(4), ui.dp(14), ui.dp(12))
    }
    val header = ui.row()
    header.addView(ui.icon(R.drawable.ic_location_on, size = 20))
    header.addView(ui.text(state.location, 15), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(6) })
    header.addView(ui.icon(R.drawable.ic_expand_more, size = 20))
    ui.click(header, "위치 선택: ${state.location}") { chooseLocation() }
    ui.add(box, header, 48)

    val body = ui.row()
    val scene = state.weatherScene
    body.addView(ui.icon(scene.icon, if (scene == WeatherScene.SUNNY) 0xFFFFBE2C.toInt() else ui.green, 54))
    val details = ui.column()
    val temperature = ui.row()
    temperature.addView(ui.text(scene.temperature, 42, bold = true))
    temperature.addView(ui.text(scene.label, 19, bold = true), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = ui.dp(10) })
    ui.add(details, temperature)
    val airQuality = if (activity.resources.configuration.screenWidthDp < 360 || activity.resources.configuration.fontScale > 1.15f) {
        "미세먼지 ${scene.airQuality}\n초미세먼지 ${scene.airQuality}"
    } else {
        "미세먼지 ${scene.airQuality} · 초미세먼지 ${scene.airQuality}"
    }
    ui.add(details, ui.text(airQuality, 12), top = 5)
    body.addView(details, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
    ui.add(box, body, top = 2)
    ui.add(box, ui.text("위치·기상 공공데이터 · 실시간 연동 예정", 10, ui.muted), top = 9)
    ui.add(content, box)
}

internal fun FeatureUiScope.profileSetupCard() {
    val card = ui.column(18).apply { background = ui.surface() }
    ui.add(card, ui.text("내 상태를 알려주세요", 23, bold = true))
    ui.add(card, ui.text("국민체력100 운동처방 데이터와 연결될 개인화 기준을 먼저 설정해요.", 14, ui.muted), top = 9)
    listOf(
        "1" to "성별과 연령대",
        "2" to "키와 몸무게",
        "3" to "운동 목표",
    ).forEach { (number, label) ->
        val row = ui.row().apply { setPadding(0, ui.dp(12), 0, 0) }
        row.addView(ui.text(number, 12, ui.green, true).apply {
            gravity = Gravity.CENTER
            background = ui.surface(ui.lime, 18)
            minimumWidth = ui.dp(30)
            minimumHeight = ui.dp(30)
        })
        row.addView(ui.text(label, 15, ui.green, true), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
        card.addView(row)
    }
    ui.add(card, ui.button("맞춤 처방 시작하기") {
        state.profileStep = 0
        navigate(AppRoute.PROFILE_SETUP)
    }, top = 20)
    ui.add(content, card)
}

internal fun FeatureUiScope.exerciseHero(indoor: Boolean) {
    val frame = FrameLayout(activity).apply {
        background = ui.surface()
        clipToOutline = true
    }
    frame.addView(ui.image(if (indoor) R.drawable.indoor_hero else R.drawable.track_hero), FrameLayout.LayoutParams(-1, -1))
    val overlay = ui.column(14)
    overlay.addView(ui.text(if (indoor) "오늘의 실내 운동" else "오늘의 야외 운동", 11, ui.green, true).apply {
        background = ui.surface(ui.lime, 20)
        setPadding(ui.dp(10), ui.dp(5), ui.dp(10), ui.dp(5))
    }, LinearLayout.LayoutParams(-2, -2))
    val exercise = MockContent.exercise(indoor, state.goal)
    ui.add(overlay, ui.text(exercise, 22, ui.white, true).apply {
        setShadowLayer(ui.dp(3).toFloat(), 0f, 1f, 0xFF133B28.toInt())
    }, top = 6)
    ui.add(overlay, ui.text("${state.goal} · 20분", 13, ui.white).apply {
        setShadowLayer(ui.dp(3).toFloat(), 0f, 1f, 0xFF133B28.toInt())
    }, top = 5)
    frame.addView(overlay, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
    ui.click(frame, "$exercise 운동 처방 보기") { openPrescription(indoor) }
    ui.add(content, frame, 215)
}

internal fun FeatureUiScope.facilityRow(index: Int, compact: Boolean = false) {
    val facility = MockContent.facilities[index]
    val row = ui.row().apply {
        setPadding(0, ui.dp(if (compact) 6 else 12), 0, ui.dp(if (compact) 6 else 12))
    }
    row.addView(ui.image(facility.image), LinearLayout.LayoutParams(ui.dp(86), ui.dp(if (compact) 52 else 68)))
    val words = ui.column()
    ui.add(words, ui.text(facility.name, 16, bold = true))
    ui.add(words, ui.text("${facility.category} · ${facility.distance}", 13, ui.muted), top = 7)
    row.addView(words, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(14) })
    row.addView(ui.icon(R.drawable.ic_chevron_right, ui.muted, 22))
    ui.click(row, "${facility.name}, ${facility.category}, ${facility.distance}, 상세 보기") {
        state.selectedFacility = index
        navigate(AppRoute.FACILITY_DETAIL)
    }
    ui.add(content, row)
}

internal fun FeatureUiScope.choiceChips(values: List<String>, selected: String, action: (String) -> Unit) {
    val scroller = HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false }
    val row = ui.row()
    values.forEach { value ->
        row.addView(ui.chip(value, value == selected) { action(value) }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = ui.dp(7) })
    }
    scroller.addView(row)
    ui.add(content, scroller)
}

internal fun FeatureUiScope.facilityMap(category: String) {
    val frame = FrameLayout(activity).apply {
        background = ui.surface()
        clipToOutline = true
    }
    frame.addView(ui.image(R.drawable.neighborhood_map), FrameLayout.LayoutParams(-1, -1))
    if (category != "야외") {
        listOf(1 to (Gravity.TOP or Gravity.START), 2 to (Gravity.BOTTOM or Gravity.END)).forEach { (index, gravity) ->
            val pin = ui.chip(MockContent.facilities[index].name, true) {
                state.selectedFacility = index
                navigate(AppRoute.FACILITY_DETAIL)
            }
            frame.addView(pin, FrameLayout.LayoutParams(-2, -2, gravity).apply {
                setMargins(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(32))
            })
        }
    }
    if (category != "실내") {
        frame.addView(ui.chip("서서울호수공원", true) {
            state.selectedFacility = 0
            navigate(AppRoute.FACILITY_DETAIL)
        }, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
    }
    val label = ui.text("개념 지도 · 반경 3km", 11).apply {
        background = ui.surface(ui.white, 8)
        setPadding(ui.dp(8), ui.dp(5), ui.dp(8), ui.dp(5))
    }
    frame.addView(label, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply {
        setMargins(ui.dp(10), 0, 0, ui.dp(8))
    })
    ui.add(content, frame, 220)
}

internal fun FeatureUiScope.summaryRow(parent: LinearLayout, label: String, value: String) {
    val row = ui.row()
    row.addView(ui.text(label, 13, ui.muted), LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(ui.text(value, 15, ui.green, true))
    ui.add(parent, row)
}

internal fun FeatureUiScope.settingToggle(label: String, checked: Boolean, action: (Boolean) -> Unit) {
    ui.add(content, MaterialSwitch(activity).apply {
        text = label
        textSize = 16f
        setTextColor(ui.green)
        isChecked = checked
        minimumHeight = ui.dp(52)
        setOnCheckedChangeListener { _, value -> action(value) }
    })
}
