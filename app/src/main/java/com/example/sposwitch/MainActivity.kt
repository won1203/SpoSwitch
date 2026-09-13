package com.example.sposwitch

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.AppState
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.feature.facility.FacilityDetailScreen
import com.example.sposwitch.feature.facility.FacilityListScreen
import com.example.sposwitch.feature.facility.WeatherSwitchScreen
import com.example.sposwitch.feature.home.HomeScreen
import com.example.sposwitch.feature.prescription.PrescriptionScreen
import com.example.sposwitch.feature.profile.ProfileScreen
import com.example.sposwitch.feature.profile.ProfileSetupScreen
import com.example.sposwitch.feature.weather.WeatherScreen
import com.example.sposwitch.ui.theme.MockUi
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Activity shell for the frontend-only mock. Feature packages own screen rendering. */
class MainActivity : AppCompatActivity() {
    private lateinit var ui: MockUi
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var state: AppState
    private val history = mutableListOf<AppRoute>()
    private var route = AppRoute.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepPrototypeVisible()
        ui = MockUi(this)
        state = AppState.from(savedInstanceState)
        route = AppRoute.fromKey(savedInstanceState?.getString("screen"))
        history.addAll(savedInstanceState?.getStringArrayList("history").orEmpty().map(AppRoute::fromKey))
        configureSystemBars()
        configureBackNavigation()
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        state.saveTo(outState)
        outState.putString("screen", route.key)
        outState.putStringArrayList("history", ArrayList(history.map { it.key }))
    }

    private fun keepPrototypeVisible() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    route == AppRoute.PROFILE_SETUP && state.profileStep > 0 -> {
                        state.profileStep--
                        render()
                    }
                    history.isNotEmpty() -> {
                        route = history.removeAt(history.lastIndex)
                        render()
                    }
                    route != AppRoute.HOME -> {
                        route = AppRoute.HOME
                        render()
                    }
                    else -> finish()
                }
            }
        })
    }

    private fun navigate(destination: AppRoute, asTab: Boolean = false) {
        if (asTab) history.clear() else if (route != destination) history.add(route)
        route = destination
        render()
    }

    private fun render() {
        root = ui.column().apply { setBackgroundColor(ui.white) }
        setContentView(root)
        applySystemInsets()
        renderTopBar()

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        content = ui.column(18)
        if (route == AppRoute.HOME) content.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val scope = FeatureUiScope(
            activity = this,
            ui = ui,
            content = content,
            state = state,
            navigateAction = ::navigate,
            renderAction = ::render,
            prescriptionAction = ::openPrescription,
            locationAction = ::chooseLocation,
        )
        when (route) {
            AppRoute.HOME -> HomeScreen.render(scope)
            AppRoute.PRESCRIPTION -> PrescriptionScreen.render(scope)
            AppRoute.FACILITIES -> FacilityListScreen.render(scope)
            AppRoute.PROFILE -> ProfileScreen.render(scope)
            AppRoute.WEATHER -> WeatherScreen.render(scope)
            AppRoute.WEATHER_SWITCH -> WeatherSwitchScreen.render(scope)
            AppRoute.FACILITY_DETAIL -> FacilityDetailScreen.render(scope)
            AppRoute.PROFILE_SETUP -> ProfileSetupScreen.render(scope)
        }

        ui.gap(content, 12)
        if (route.showsBottomNavigation) renderBottomNavigation()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun renderTopBar() {
        val top = ui.row().apply { setPadding(ui.dp(12), ui.dp(4), ui.dp(12), 0) }
        if (!route.isTopLevel) {
            top.addView(ui.iconButton(R.drawable.ic_arrow_back, "뒤로 가기") {
                onBackPressedDispatcher.onBackPressed()
            })
        }
        top.addView(
            ui.text(route.title, if (route == AppRoute.HOME) 23 else 20, bold = true).apply {
                setPadding(ui.dp(8), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        top.addView(ui.iconButton(R.drawable.ic_notifications, "알림 및 날씨 체험") {
            navigate(AppRoute.WEATHER)
        })
        ui.add(root, top, 60)
    }

    private fun renderBottomNavigation() {
        ui.divider(root, 0)
        val bar = ui.row()
        listOf(
            Triple(AppRoute.HOME, "홈", R.drawable.ic_home),
            Triple(AppRoute.PRESCRIPTION, "운동 처방", R.drawable.ic_fitness_center),
            Triple(AppRoute.FACILITIES, "시설", R.drawable.ic_location_on),
            Triple(AppRoute.PROFILE, "내 상태", R.drawable.ic_person),
        ).forEach { (destination, label, icon) ->
            val selected = route.bottomDestination == destination
            val item = ui.column().apply {
                gravity = Gravity.CENTER
                setPadding(0, ui.dp(10), 0, ui.dp(9))
                addView(ui.icon(icon, if (selected) ui.green else ui.muted, 25))
                ui.gap(this, 4)
                addView(ui.text(label, 12, if (selected) ui.green else ui.muted, selected).apply {
                    gravity = Gravity.CENTER
                })
                isSelected = selected
                ui.click(this, label) { navigate(destination, asTab = true) }
            }
            bar.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
        ui.add(root, bar, 67)
    }

    private fun openPrescription(indoor: Boolean) {
        if (!state.profileComplete) {
            state.profileStep = 0
            navigate(AppRoute.PROFILE_SETUP)
            return
        }
        state.prescriptionIndoor = indoor
        state.videoStarted = false
        state.prescriptionComplete = false
        navigate(AppRoute.PRESCRIPTION)
    }

    private fun chooseLocation() {
        val locations = arrayOf(
            "서울특별시 양천구 신월7동",
            "서울특별시 양천구 신정3동",
            "서울특별시 양천구 목5동",
            "서울특별시 강서구 화곡1동",
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("현재 위치 선택 · 목업")
            .setSingleChoiceItems(locations, locations.indexOf(state.location)) { dialog, which ->
                state.location = locations[which]
                dialog.dismiss()
                root.post { render() }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
