package com.example.sposwitch.feature.facility

import com.example.sposwitch.R
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.remote.FacilityApiClient
import com.example.sposwitch.model.NearbyFacilities
import com.example.sposwitch.model.NearbyFacility

/**
 * Facility screen state kept inside the feature so shared app state stays untouched.
 * ponytail: in-memory only; results are refetched after process death. Move into a ViewModel with the planned M4 refactor.
 */
internal object FacilityStore {
    /** Marks state.selectedFacility when the detail should show [selected] instead of a mock facility index. */
    const val REAL_SELECTION = -1

    sealed interface Load {
        data object Loading : Load
        data class Ready(val result: NearbyFacilities) : Load
        data class Failed(val message: String) : Load
    }

    private val client = FacilityApiClient()
    private val loads = mutableMapOf<String, Load>()
    var selected: NearbyFacility? = null

    /** Show only sports that fit the saved exercise goal. */
    var goalOnly = true

    /** True once the user picks a filter chip, so the weather default stops overriding them. */
    var filterTouched = false

    /** Returns the current load for this origin and filters, starting a request the first time. */
    fun nearby(
        scope: FeatureUiScope,
        latitude: Double,
        longitude: Double,
        environment: String?,
        goal: String? = null,
    ): Load {
        val key = "$latitude,$longitude,$environment,$goal"
        loads[key]?.let { return it }
        loads[key] = Load.Loading
        client.getNearby(latitude, longitude, environment, goal) { result ->
            loads[key] = result.fold(
                onSuccess = { Load.Ready(it) },
                onFailure = { Load.Failed(it.message ?: "주변 시설을 불러오지 못했습니다.") },
            )
            scope.rerender()
        }
        return Load.Loading
    }

    fun retry(scope: FeatureUiScope) {
        loads.entries.removeAll { it.value is Load.Failed }
        scope.rerender()
    }

    /**
     * Icon for the list thumbnail. The dataset has no facility photos, so a sport icon fills that slot
     * instead of a stock image that would read as a real photo of the place.
     */
    fun sportIcon(sport: String?): Int = when (sport) {
        "축구", "축구장", "풋살장", "농구", "야구", "야구장", "테니스장", "배드민턴", "구기체육관" -> R.drawable.ic_sport_ball
        "수영장", "수영" -> R.drawable.ic_sport_swim
        "골프", "골프장", "골프연습장", "스크린" -> R.drawable.ic_sport_golf
        "간이운동장", "기타시설", "롤러스케이트장", "롤러스케이트", "파크골프장", "국궁장",
        "전천후게이트볼장", "실외인공암벽장", "빙상장", "썰매장" -> R.drawable.ic_directions_run
        null -> R.drawable.ic_location_on
        else -> R.drawable.ic_fitness_center
    }

    /** Goal labels come from the profile screen; the backend takes the codes. */
    fun goalToCode(goal: String): String = when (goal) {
        "근력 및 근육 강화" -> "STRENGTH_MUSCLE"
        "체지방 감소" -> "FAT_LOSS"
        "유연성 및 자세 개선" -> "FLEXIBILITY_POSTURE"
        else -> "GENERAL_FITNESS"
    }

    fun filterToEnvironment(filter: String): String? = when (filter) {
        "실내" -> "INDOOR"
        "야외" -> "OUTDOOR"
        else -> null
    }
}
