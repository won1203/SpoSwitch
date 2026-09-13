package com.example.sposwitch.feature.facility

import com.example.sposwitch.R
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.ui.component.choiceChips
import com.example.sposwitch.ui.component.facilityMap
import com.example.sposwitch.ui.component.facilityRow

internal object FacilityListScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text("내 주변, 움직일 곳", 27, bold = true))
        ui.add(content, ui.text("${state.location} · 반경 3km", 14, ui.muted), top = 9)
        ui.gap(content, 20)
        choiceChips(listOf("전체", "야외", "실내"), state.facilityFilter) {
            state.facilityFilter = it
            rerender()
        }
        ui.gap(content, 16)
        if (state.mapVisible) facilityMap(state.facilityFilter)
        ui.add(content, ui.button(
            if (state.mapVisible) "목록만 보기" else "지도 함께 보기",
            false,
            R.drawable.ic_location_on,
        ) {
            state.mapVisible = !state.mapVisible
            rerender()
        }, top = 12)

        val indices = MockContent.facilities.indices.filter {
            state.facilityFilter == "전체" || MockContent.facilities[it].category == state.facilityFilter
        }
        ui.gap(content, 20)
        ui.section(content, "가까운 시설 ${indices.size}곳")
        indices.sortedBy { index ->
            MockContent.facilities[index].distance.let { distance ->
                if (distance.endsWith("km")) distance.removeSuffix("km").toDouble() * 1000 else distance.removeSuffix("m").toDouble()
            }
        }.forEach {
            facilityRow(it)
            ui.divider(content, 0)
        }
        ui.gap(content, 16)
        ui.add(content, ui.text("시설명·거리·지도는 화면 확인용 예시입니다.", 12, ui.muted))
    }
}
