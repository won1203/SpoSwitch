package com.example.sposwitch.feature.facility

import android.app.Dialog
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.R
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.data.remote.isOnline
import com.example.sposwitch.model.NearbyFacility
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

private const val MY_LOCATION_COLOR = 0xFF1E6FD9.toInt()
private var kakaoSdkInitialized = false

/**
 * Kakao map preview for the facility screens. The preview ignores touches so the screen scrolls normally;
 * tapping it opens [openFullscreenMap], where the map pans and zooms freely and a pin tap opens its detail.
 * Needs KAKAO_NATIVE_APP_KEY (see app/build.gradle.kts), this build's key hash registered in Kakao Developers,
 * and the app's 카카오맵 service enabled.
 */
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
        // Covers the map so drags scroll the screen instead of fighting the map for the gesture.
        frame.addView(View(activity).also { cover ->
            ui.click(cover, "지도 크게 보기") { openFullscreenMap(facilities, focus) }
        }, FrameLayout.LayoutParams(-1, -1))
        frame.addView(
            ui.text("크게 보기", 12, ui.green, true).apply {
                background = ui.surface(ui.white, 999)
                setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6))
                elevation = ui.dp(2).toFloat()
            },
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, ui.dp(10), ui.dp(10)) },
        )
        offlineNotice(frame)
    }
    frame.addView(message, FrameLayout.LayoutParams(-1, -1))
    ui.add(content, frame, height)
    ui.add(content, mapLegend(if (focus == null) " · 지도를 누르면 크게 볼 수 있어요" else ""), top = 6)
}

/** Full-screen map in its own window, so vertical drags move the map rather than the screen behind it. */
private fun FeatureUiScope.openFullscreenMap(facilities: List<NearbyFacility>, focus: NearbyFacility?) {
    val dialog = Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar)
    val root = FrameLayout(activity).apply { setBackgroundColor(ui.white) }
    root.addView(kakaoMapView(facilities, focus, onPin = { dialog.dismiss() }) { error ->
        activity.runOnUiThread {
            root.addView(ui.text("지도를 불러오지 못했어요.\n$error", 13, ui.muted).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(-1, -1))
        }
    }, FrameLayout.LayoutParams(-1, -1))
    // Controls sit inside the system bars; the map itself stays edge to edge.
    val overlay = FrameLayout(activity)
    ViewCompat.setOnApplyWindowInsetsListener(overlay) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
    root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    overlay.addView(
        ui.iconButton(R.drawable.ic_close, "지도 닫기") { dialog.dismiss() }.apply {
            background = ui.surface(ui.white, 999)
            elevation = ui.dp(3).toFloat()
        },
        FrameLayout.LayoutParams(ui.dp(48), ui.dp(48), Gravity.TOP or Gravity.START).apply { setMargins(ui.dp(16), ui.dp(16), 0, 0) },
    )
    offlineNotice(overlay, topMargin = 76)
    val hint = if (focus == null) " · 핀을 누르면 상세로 이동해요" else ""
    overlay.addView(
        mapLegend(hint).apply {
            background = ui.surface(ui.white, 999)
            setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8))
            elevation = ui.dp(3).toFloat()
        },
        FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ui.dp(16) },
    )
    dialog.setContentView(root)
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    // A dialog outliving its Activity leaks the window, e.g. on rotation.
    activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) = dialog.dismiss()
    })
    dialog.show()
}

/** Kakao keeps showing cached tiles offline and reports no error, so say why the rest of the map is blank. */
private fun FeatureUiScope.offlineNotice(parent: FrameLayout, topMargin: Int = 10) {
    if (activity.isOnline()) return
    parent.addView(
        ui.text("인터넷에 연결되지 않아 지도가 일부만 보일 수 있어요.", 12, ui.green, true).apply {
            background = ui.surface(ui.white, 999)
            setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6))
            elevation = ui.dp(2).toFloat()
        },
        FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { this.topMargin = ui.dp(topMargin) },
    )
}

private fun FeatureUiScope.mapLegend(suffix: String): TextView =
    ui.text("● 실내  ● 야외  ● 내 위치$suffix", 11, ui.muted).apply {
        text = android.text.SpannableString(text).apply {
            setSpan(android.text.style.ForegroundColorSpan(ui.green), 0, 1, 0)
            setSpan(android.text.style.ForegroundColorSpan(ui.accent), 6, 7, 0)
            setSpan(android.text.style.ForegroundColorSpan(MY_LOCATION_COLOR), 12, 13, 0)
        }
    }

private fun FeatureUiScope.kakaoMapView(
    facilities: List<NearbyFacility>,
    focus: NearbyFacility?,
    onPin: () -> Unit = {},
    onError: (String) -> Unit,
): MapView {
    val me = state.latitude?.let { latitude -> state.longitude?.let { LatLng.from(latitude, it) } }
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
                    onPin()
                    FacilityStore.selected = facility
                    state.selectedFacility = FacilityStore.REAL_SELECTION
                    navigate(AppRoute.FACILITY_DETAIL)
                }
                true
            }
            // A fixed zoom hides facilities near the edge of the 3 km radius, so frame every pin.
            // Padding exceeds the pin height: pins hang above their point and were clipped at the top edge.
            if (focus == null) {
                val points = facilities.map { LatLng.from(it.latitude, it.longitude) } + listOfNotNull(me)
                if (points.size > 1) {
                    map.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), ui.dp(48)))
                }
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
    return mapView
}

private fun FeatureUiScope.pinStyle(resource: Int, color: Int, bottom: Boolean = true): LabelStyles {
    val size = ui.dp(if (bottom) 34 else 26)
    val drawable = ContextCompat.getDrawable(activity, resource)!!.mutate().also { DrawableCompat.setTint(it, color) }
    val bitmap: Bitmap = drawable.toBitmap(size, size)
    return LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, if (bottom) 1f else 0.5f))
}
