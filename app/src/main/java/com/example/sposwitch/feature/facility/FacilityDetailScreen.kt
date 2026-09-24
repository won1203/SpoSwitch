package com.example.sposwitch.feature.facility

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.mock.MockContent
import com.example.sposwitch.model.NearbyFacility
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal object FacilityDetailScreen {
    fun render(scope: FeatureUiScope) = with(scope) {
        if (state.selectedFacility == FacilityStore.REAL_SELECTION) {
            val facility = FacilityStore.selected
            if (facility != null) renderFacility(facility) else renderExpired()
        } else {
            renderMock()
        }
    }

    private fun FeatureUiScope.renderFacility(facility: NearbyFacility) {
        facilityMap(listOf(facility), focus = facility, height = 225)
        ui.gap(content, 20)
        ui.add(content, ui.text("${facility.environmentText} · 내 위치에서 ${facility.distanceText}", 13, ui.accent, true))
        ui.add(content, ui.text(facility.name, 27, bold = true), top = 8)
        ui.add(content, ui.text(facility.sportText, 15, ui.muted), top = 10)
        ui.divider(content, 22)
        ui.gap(content, 22)
        ui.section(content, "방문 전 확인해요")
        ui.add(content, ui.text(facility.addressText, 15), top = 16)
        if (facility.roadAddress != null && facility.lotAddress != null) {
            ui.add(content, ui.text("지번 ${facility.lotAddress}", 13, ui.muted), top = 6)
        }
        detailLine("운영", facility.ownershipText)
        if (facility.phone != null) {
            detailLine("전화", facility.phone, onClick = { dial(facility.phone) })
        }
        detailLine(
            "실내외",
            if (facility.environmentSource == "SPORT_RULE") "${facility.environmentText} (종목 기준 추정)" else facility.environmentText,
        )
        ui.add(content, ui.text("운영시간·이용요금은 시설에 확인해 주세요.", 14, ui.muted), top = 16)
        ui.add(
            content,
            ui.text("출처: 국민체육진흥공단 전국체육시설" + (facility.updatedOn?.let { " · $it 갱신" } ?: ""), 12, ui.muted),
            top = 8,
        )
        ui.add(content, ui.button("지도 앱으로 길안내", icon = R.drawable.ic_near_me) { openMap(facility) }, top = 24)
        ui.add(content, ui.button("이곳에서 할 운동 처방 보기", false, R.drawable.ic_play_arrow) {
            openPrescription(facility.indoor)
        }, top = 12)
    }

    private fun FeatureUiScope.detailLine(label: String, value: String, onClick: (() -> Unit)? = null) {
        val row = ui.row()
        row.addView(ui.text(label, 13, ui.muted), android.widget.LinearLayout.LayoutParams(ui.dp(64), -2))
        row.addView(ui.text(value, 14, if (onClick != null) ui.accent else ui.green, onClick != null))
        if (onClick != null) ui.click(row, "$label $value, 전화 걸기") { onClick() }
        ui.add(content, row, top = 10)
    }

    /** Opens the dialer with the number filled in; the user still decides whether to call. */
    private fun FeatureUiScope.dial(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, phone, Toast.LENGTH_LONG).show()
        }
    }

    /** Opens any installed map app; falls back to Kakao Map on the web. */
    private fun FeatureUiScope.openMap(facility: NearbyFacility) {
        val geo = Uri.parse("geo:${facility.latitude},${facility.longitude}?q=${facility.latitude},${facility.longitude}(${Uri.encode(facility.name)})")
        val web = Uri.parse("https://map.kakao.com/link/map/${Uri.encode(facility.name)},${facility.latitude},${facility.longitude}")
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, geo))
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, web))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(activity, facility.addressText, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** The in-memory selection is gone after the app process restarts. */
    private fun FeatureUiScope.renderExpired() {
        ui.add(content, ui.text("시설 정보를 다시 불러와 주세요.", 16), top = 8)
        ui.add(content, ui.button("주변 시설 보기", false, R.drawable.ic_location_on) {
            navigate(AppRoute.FACILITIES, true)
        }, top = 16)
    }

    /** Home still links to the mock facility card; keep its detail as it was. */
    private fun FeatureUiScope.renderMock() {
        val facility = MockContent.facilities.getOrElse(state.selectedFacility) { MockContent.facilities.first() }
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
