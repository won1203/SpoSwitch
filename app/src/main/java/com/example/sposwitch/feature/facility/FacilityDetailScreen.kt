package com.example.sposwitch.feature.facility

import com.example.sposwitch.R
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal object FacilityDetailScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        val facility = MockContent.facilities[state.selectedFacility]
        ui.add(content, ui.image(facility.image), 225)
        ui.gap(content, 20)
        ui.add(content, ui.text("${facility.category} · 내 위치에서 ${facility.distance}", 13, ui.accent, true))
        ui.add(content, ui.text(facility.name, 27, bold = true), top = 8)
        ui.add(content, ui.text(facility.sport, 15, ui.muted), top = 10)
        ui.divider(content, 22)
        ui.gap(content, 22)
        ui.section(content, "방문 전 확인해요")
        ui.add(content, ui.text(facility.address, 15), top = 16)
        ui.add(content, ui.text("운영시간·이용요금은 시설에 확인해 주세요.", 14, ui.muted), top = 10)
        ui.add(content, ui.text("시설 정보와 사진은 목업용 예시입니다.", 12, ui.muted), top = 10)
        ui.add(content, ui.button("길안내 미리보기", icon = R.drawable.ic_near_me) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("길안내 미리보기")
                .setMessage("${state.location} → ${facility.name}\n예시 거리 ${facility.distance}\n\n목업에서는 이동 흐름만 확인합니다. 실제 지도 앱이나 길안내는 연결하지 않았습니다.")
                .setPositiveButton("확인", null)
                .show()
        }, top = 24)
        ui.add(content, ui.button("이곳에서 할 운동 처방 보기", false, R.drawable.ic_play_arrow) {
            openPrescription(facility.indoor)
        }, top = 12)
    }
}
