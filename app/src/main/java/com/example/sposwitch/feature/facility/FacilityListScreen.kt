package com.example.sposwitch.feature.facility

import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.domain.RecommendationPolicy
import com.example.sposwitch.model.NearbyFacilities
import com.example.sposwitch.model.NearbyFacility
import com.example.sposwitch.ui.component.choiceChips

internal object FacilityListScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        ui.add(content, ui.text("내 주변, 움직일 곳", 27, bold = true))
        ui.add(content, ui.text("${state.location} · 반경 3km", 14, ui.muted), top = 9)
        ui.gap(content, 20)

        // Bad weather opens the list on indoor facilities until the user picks a filter themselves.
        val indoorByWeather = state.weather?.let { RecommendationPolicy.weatherRisks(it).isNotEmpty() } == true
        if (indoorByWeather && !FacilityStore.filterTouched && state.facilityFilter == "전체") {
            state.facilityFilter = "실내"
        }
        choiceChips(listOf("전체", "야외", "실내"), state.facilityFilter) {
            state.facilityFilter = it
            FacilityStore.filterTouched = true
            rerender()
        }
        if (indoorByWeather) {
            ui.add(content, ui.text("현재 날씨를 고려해 실내 시설을 먼저 보여드려요.", 13, ui.accent, true), top = 10)
        }
        ui.gap(content, 12)
        val goal = goalFilter()
        ui.gap(content, 16)

        val result = nearbyFacilities(FacilityStore.filterToEnvironment(state.facilityFilter), goal) ?: return@with
        facilityMap(result.items)
        ui.gap(content, 20)
        ui.section(content, "가까운 시설 ${result.items.size}곳")
        if (result.totalCount > result.items.size) {
            ui.add(content, ui.text("반경 3km 안 ${result.totalCount}곳 중 가까운 순으로 보여드려요.", 13, ui.muted), top = 6)
        }
        if (result.items.isEmpty()) {
            val reason = if (goal != null) {
                "반경 3km 안에 ${state.profile.goal} 목표에 맞는 ${state.facilityFilter} 시설이 없어요. 목표 필터를 끄거나 다른 조건을 선택해 보세요."
            } else {
                "반경 3km 안에 조건에 맞는 시설이 없어요. 다른 필터를 선택해 보세요."
            }
            ui.add(content, ui.text(reason, 14, ui.muted), top = 12)
        }
        result.items.forEach {
            nearbyFacilityRow(it)
            ui.divider(content, 0)
        }
        ui.gap(content, 16)
        ui.add(content, ui.text("출처: 국민체육진흥공단 전국체육시설 · ${result.dataLoadedAt.take(10)} 조회", 12, ui.muted))
    }
}

/** Goal toggle. Returns the goal code to filter by, or null to show every sport. */
private fun FeatureUiScope.goalFilter(): String? {
    if (!state.profile.isComplete) {
        ui.add(content, ui.button("내 상태를 입력하면 목표에 맞는 시설만 볼 수 있어요", false, R.drawable.ic_person) {
            state.profileStep = 0
            navigate(AppRoute.PROFILE_SETUP)
        }, top = 4)
        return null
    }
    val row = ui.row()
    row.addView(ui.chip("내 목표에 맞는 곳만", FacilityStore.goalOnly) {
        FacilityStore.goalOnly = !FacilityStore.goalOnly
        rerender()
    })
    row.addView(
        ui.text(if (FacilityStore.goalOnly) state.profile.goal else "모든 종목", 13, ui.muted),
        LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(10) },
    )
    ui.add(content, row, top = 4)
    return if (FacilityStore.goalOnly) FacilityStore.goalToCode(state.profile.goal) else null
}

/**
 * Renders location/loading/error states and returns the result once it is ready.
 * Uses the coordinates the weather card already resolved, so it follows GPS without its own location request.
 */
internal fun FeatureUiScope.nearbyFacilities(environment: String?, goal: String? = null): NearbyFacilities? {
    val weather = state.weather
    if (weather == null) {
        ui.add(content, ui.text("현재 위치를 확인하면 주변 시설을 보여드려요.", 14, ui.muted), top = 4)
        ui.add(content, ui.button("현재 위치 확인", false, R.drawable.ic_near_me) { refreshWeather() }, top = 12)
        return null
    }
    return when (val load = FacilityStore.nearby(this, weather.latitude, weather.longitude, environment, goal)) {
        FacilityStore.Load.Loading -> {
            val row = ui.row()
            row.addView(ProgressBar(activity).apply { contentDescription = "주변 시설 불러오는 중" })
            row.addView(ui.text("주변 시설을 찾는 중이에요.", 14, ui.muted), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = ui.dp(12) })
            ui.add(content, row, top = 4)
            null
        }
        is FacilityStore.Load.Failed -> {
            ui.add(content, ui.text(load.message, 14, ui.muted), top = 4)
            ui.add(content, ui.button("다시 시도", false, R.drawable.ic_near_me) { FacilityStore.retry(this) }, top = 12)
            null
        }
        is FacilityStore.Load.Ready -> load.result
    }
}

internal fun FeatureUiScope.nearbyFacilityRow(facility: NearbyFacility) {
    val row = ui.row().apply { setPadding(0, ui.dp(12), 0, ui.dp(12)) }
    val tint = if (facility.indoor) ui.green else ui.accent
    row.addView(
        ui.icon(FacilityStore.sportIcon(facility.sport), tint, 24).apply {
            background = ui.surface(if (facility.indoor) ui.pale else ui.lime, 12)
            setPadding(ui.dp(9), ui.dp(9), ui.dp(9), ui.dp(9))
        },
        LinearLayout.LayoutParams(ui.dp(46), ui.dp(46)).apply { rightMargin = ui.dp(12) },
    )
    val words = ui.column()
    ui.add(words, ui.text(facility.name, 16, bold = true))
    ui.add(words, ui.text("${facility.sportText} · ${facility.environmentText} · ${facility.distanceText}", 13, ui.accent, true), top = 5)
    ui.add(words, ui.text(facility.addressText, 13, ui.muted), top = 4)
    row.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    row.addView(ui.icon(R.drawable.ic_chevron_right, ui.muted, 22))
    ui.click(row, "${facility.name}, ${facility.sportText}, ${facility.environmentText}, ${facility.distanceText}, 상세 보기") {
        FacilityStore.selected = facility
        state.selectedFacility = FacilityStore.REAL_SELECTION
        navigate(AppRoute.FACILITY_DETAIL)
    }
    ui.add(content, row)
}
