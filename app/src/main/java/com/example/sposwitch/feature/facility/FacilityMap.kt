package com.example.sposwitch.feature.facility

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.model.NearbyFacility
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

private const val MY_LOCATION_COLOR = 0xFF1E6FD9.toInt()
private var kakaoSdkInitialized = false

/**
 * Kakao map for the facility screens. Pins are the facilities passed in; tapping one opens its detail.
 * Needs KAKAO_NATIVE_APP_KEY (see app/build.gradle.kts), this build's key hash registered in Kakao Developers,
 * and the app's 카카오맵 service enabled.
 */
@SuppressLint("ClickableViewAccessibility")
internal fun FeatureUiScope.facilityMap(
    facilities: List<NearbyFacility>,
    focus: NearbyFacility? = null,
    height: Int = 260,
) {
    val frame = FrameLayout(activity).apply {
        background = ui.surface()
        clipToOutline = true
    }
    val message = ui.text("", 13, ui.muted).apply {
        gravity = Gravity.CENTER
        setPadding(ui.dp(20), 0, ui.dp(20), 0)
        visibility = View.GONE
    }
    fun showMessage(text: String) {
        message.text = text
        message.visibility = View.VISIBLE
    }

    val key = BuildConfig.KAKAO_NATIVE_APP_KEY
    if (key.isBlank()) {
        showMessage("지도를 표시하려면 카카오 네이티브 앱 키가 필요해요.\nlocal.properties에 KAKAO_NATIVE_APP_KEY를 넣어 주세요.")
    } else {
        if (!kakaoSdkInitialized) {
            KakaoMapSdk.init(activity.applicationContext, key)
            kakaoSdkInitialized = true
        }
        frame.addView(kakaoMapView(facilities, focus) { error ->
            activity.runOnUiThread { showMessage("지도를 불러오지 못했어요.\n$error") }
        }, FrameLayout.LayoutParams(-1, -1))
    }
    frame.addView(message, FrameLayout.LayoutParams(-1, -1))
    ui.add(content, frame, height)

    val legend = "● 실내  ● 야외  ● 내 위치" + if (focus == null) " · 핀을 누르면 상세로 이동해요" else ""
    ui.add(content, ui.text(legend, 11, ui.muted).apply {
        text = android.text.SpannableString(text).apply {
            setSpan(android.text.style.ForegroundColorSpan(ui.green), 0, 1, 0)
            setSpan(android.text.style.ForegroundColorSpan(ui.accent), 6, 7, 0)
            setSpan(android.text.style.ForegroundColorSpan(MY_LOCATION_COLOR), 12, 13, 0)
        }
    }, top = 6)
}

@SuppressLint("ClickableViewAccessibility")
private fun FeatureUiScope.kakaoMapView(
    facilities: List<NearbyFacility>,
    focus: NearbyFacility?,
    onError: (String) -> Unit,
): MapView {
    val me = state.weather?.let { LatLng.from(it.latitude, it.longitude) }
    val center = focus?.let { LatLng.from(it.latitude, it.longitude) } ?: me ?: LatLng.from(37.5665, 126.9780)
    val mapView = MapView(activity)
    mapView.contentDescription = if (focus != null) "${focus.name} 위치 지도" else "주변 운동 시설 지도"

    mapView.start(object : MapLifeCycleCallback() {
        override fun onMapDestroy() = Unit
        override fun onMapError(error: Exception) = onError(error.message ?: error.javaClass.simpleName)
    }, object : KakaoMapReadyCallback() {
        override fun getPosition(): LatLng = center
        override fun getZoomLevel(): Int = if (focus != null) 17 else 15

        override fun onMapReady(map: KakaoMap) {
            val labels = map.labelManager ?: return
            val layer = labels.layer ?: return
            val indoorPin = labels.addLabelStyles(pinStyle(R.drawable.ic_location_on, ui.green))
            val outdoorPin = labels.addLabelStyles(pinStyle(R.drawable.ic_location_on, ui.accent))
            val myPin = labels.addLabelStyles(pinStyle(R.drawable.ic_near_me, MY_LOCATION_COLOR, bottom = false))
            facilities.forEach { facility ->
                layer.addLabel(
                    LabelOptions.from(LatLng.from(facility.latitude, facility.longitude))
                        .setStyles(if (facility.indoor) indoorPin else outdoorPin)
                        .setTag(facility),
                )
            }
            if (me != null) {
                layer.addLabel(LabelOptions.from(me).setStyles(myPin))
            }
            map.setOnLabelClickListener { _, _, label ->
                val facility = label.tag as? NearbyFacility
                if (facility != null && facility != focus) {
                    FacilityStore.selected = facility
                    state.selectedFacility = FacilityStore.REAL_SELECTION
                    navigate(AppRoute.FACILITY_DETAIL)
                }
                true
            }
        }
    })

    // The SDK must follow the Activity's resume/pause; screens are rebuilt on every render, so tie it to this view.
    val lifecycle = activity.lifecycle
    val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) = mapView.resume()
        override fun onPause(owner: LifecycleOwner) = mapView.pause()
    }
    mapView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = lifecycle.addObserver(observer)
        override fun onViewDetachedFromWindow(view: View) {
            lifecycle.removeObserver(observer)
            mapView.finish()
        }
    })
    // Let the map pan inside the screen's ScrollView.
    mapView.setOnTouchListener { view, event ->
        view.parent.requestDisallowInterceptTouchEvent(
            event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL,
        )
        false
    }
    return mapView
}

private fun FeatureUiScope.pinStyle(resource: Int, color: Int, bottom: Boolean = true): LabelStyles {
    val size = ui.dp(if (bottom) 34 else 26)
    val drawable = ContextCompat.getDrawable(activity, resource)!!.mutate().also { DrawableCompat.setTint(it, color) }
    val bitmap: Bitmap = drawable.toBitmap(size, size)
    return LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, if (bottom) 1f else 0.5f))
}
