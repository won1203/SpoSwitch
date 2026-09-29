package com.example.sposwitch.model

/** A Seoul district picked by hand when device location is denied or unavailable. Coordinates are each 구청. */
internal data class SeoulDistrict(val name: String, val latitude: Double, val longitude: Double) {
    val label: String get() = "서울특별시 $name · 선택한 지역"

    companion object {
        val all = listOf(
            SeoulDistrict("강남구", 37.5172, 127.0473),
            SeoulDistrict("강동구", 37.5301, 127.1238),
            SeoulDistrict("강북구", 37.6396, 127.0257),
            SeoulDistrict("강서구", 37.5509, 126.8495),
            SeoulDistrict("관악구", 37.4784, 126.9516),
            SeoulDistrict("광진구", 37.5385, 127.0823),
            SeoulDistrict("구로구", 37.4954, 126.8874),
            SeoulDistrict("금천구", 37.4569, 126.8955),
            SeoulDistrict("노원구", 37.6542, 127.0568),
            SeoulDistrict("도봉구", 37.6688, 127.0471),
            SeoulDistrict("동대문구", 37.5744, 127.0400),
            SeoulDistrict("동작구", 37.5124, 126.9393),
            SeoulDistrict("마포구", 37.5663, 126.9019),
            SeoulDistrict("서대문구", 37.5791, 126.9368),
            SeoulDistrict("서초구", 37.4837, 127.0324),
            SeoulDistrict("성동구", 37.5633, 127.0371),
            SeoulDistrict("성북구", 37.5894, 127.0167),
            SeoulDistrict("송파구", 37.5145, 127.1066),
            SeoulDistrict("양천구", 37.5170, 126.8665),
            SeoulDistrict("영등포구", 37.5264, 126.8962),
            SeoulDistrict("용산구", 37.5324, 126.9906),
            SeoulDistrict("은평구", 37.6027, 126.9291),
            SeoulDistrict("종로구", 37.5735, 126.9790),
            SeoulDistrict("중구", 37.5641, 126.9979),
            SeoulDistrict("중랑구", 37.6066, 127.0927),
        )

        fun find(name: String?): SeoulDistrict? = all.firstOrNull { it.name == name }
    }
}
