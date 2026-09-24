package com.example.sposwitch.model

import java.util.Locale

/** A real facility from the backend (국민체육진흥공단 전국체육시설). Nullable fields are missing in the source. */
internal data class NearbyFacility(
    val id: String,
    val name: String,
    val sport: String?,
    val environment: String,
    val environmentSource: String,
    val ownership: String?,
    val operator: String?,
    val roadAddress: String?,
    val lotAddress: String?,
    val phone: String?,
    val latitude: Double,
    val longitude: Double,
    val updatedOn: String?,
    val distanceMeters: Int,
) {
    val indoor: Boolean get() = environment != "OUTDOOR"

    val distanceText: String
        get() = if (distanceMeters < 1000) "${distanceMeters}m" else String.format(Locale.KOREA, "%.1fkm", distanceMeters / 1000.0)

    val environmentText: String
        get() = when (environment) {
            "INDOOR" -> "실내"
            "OUTDOOR" -> "야외"
            "BOTH" -> "실내·야외"
            else -> "실내외 정보 없음"
        }

    val ownershipText: String
        get() = when (ownership) {
            "PUBLIC" -> listOfNotNull("공공", operator).joinToString(" · ")
            "PRIVATE" -> "민간"
            else -> "운영 구분 정보 없음"
        }

    val sportText: String get() = sport ?: "종목 정보 없음"

    val addressText: String get() = roadAddress ?: lotAddress ?: "주소 정보 없음"
}

internal data class NearbyFacilities(
    val totalCount: Int,
    val radiusMeters: Int,
    val dataLoadedAt: String,
    val items: List<NearbyFacility>,
)
