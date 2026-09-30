package com.example.sposwitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sposwitch.app.AppRoute
import com.example.sposwitch.app.AppState
import com.example.sposwitch.app.ExerciseLoadState
import com.example.sposwitch.app.FeatureUiScope
import com.example.sposwitch.app.WeatherLoadState
import com.example.sposwitch.data.local.UserProfileRepository
import com.example.sposwitch.domain.RecommendationPolicy
import com.example.sposwitch.data.location.DeviceLocationProvider
import com.example.sposwitch.data.remote.WeatherApiClient
import com.example.sposwitch.data.remote.ExerciseApiClient
import com.example.sposwitch.feature.facility.FacilityDetailScreen
import com.example.sposwitch.feature.facility.FacilityListScreen
import com.example.sposwitch.feature.facility.WeatherSwitchScreen
import com.example.sposwitch.feature.home.HomeScreen
import com.example.sposwitch.feature.prescription.PrescriptionScreen
import com.example.sposwitch.feature.prescription.ExercisePlanScreen
import com.example.sposwitch.feature.prescription.ExerciseVideoActivity
import com.example.sposwitch.feature.profile.ProfileScreen
import com.example.sposwitch.feature.profile.ProfileSetupScreen
import com.example.sposwitch.feature.weather.WeatherScreen
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseVideo
import com.example.sposwitch.model.SeoulDistrict
import com.example.sposwitch.ui.theme.MockUi

/** Activity shell. Feature packages own screen rendering and the backend owns external API keys. */
class MainActivity : AppCompatActivity() {
    private lateinit var ui: MockUi
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var state: AppState
    private lateinit var deviceLocationProvider: DeviceLocationProvider
    private lateinit var weatherApiClient: WeatherApiClient
    private lateinit var exerciseApiClient: ExerciseApiClient
    private lateinit var userProfileRepository: UserProfileRepository
    private val history = mutableListOf<AppRoute>()
    private var route = AppRoute.HOME
    private var weatherRequestId = 0
    private var exerciseRequestId = 0
    private var planRequestId = 0
    private var lastWeatherRefreshAt = 0L
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            requestDeviceLocation()
        } else {
            state.location = "위치 권한 필요"
            showWeatherError("현재 위치의 날씨를 표시하려면 위치 권한이 필요합니다.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepPrototypeVisible()
        ui = MockUi(this)
        userProfileRepository = UserProfileRepository(this)
        state = AppState.from(savedInstanceState, userProfileRepository.load())
        state.manualDistrict = userProfileRepository.loadDistrict()
        deviceLocationProvider = DeviceLocationProvider(this)
        weatherApiClient = WeatherApiClient()
        exerciseApiClient = ExerciseApiClient()
        route = AppRoute.fromKey(savedInstanceState?.getString("screen"))
        history.addAll(savedInstanceState?.getStringArrayList("history").orEmpty().map(AppRoute::fromKey))
        configureSystemBars()
        configureBackNavigation()
        render()
        loadExerciseTabIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (SystemClock.elapsedRealtime() - lastWeatherRefreshAt >= WEATHER_REFRESH_INTERVAL_MS) {
            refreshWeather()
        }
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
                loadExerciseTabIfNeeded()
            }
        })
    }

    private fun navigate(destination: AppRoute, asTab: Boolean = false) {
        if (asTab) history.clear() else if (route != destination) history.add(route)
        route = destination
        render()
        loadExerciseTabIfNeeded()
    }

    private fun loadExerciseTabIfNeeded() {
        if (!state.profile.isComplete) return
        // Opening the tab directly (not from a home card) should follow today's pick, not the OUTDOOR default.
        if (state.exerciseLoadState == ExerciseLoadState.IDLE && state.planLoadState == ExerciseLoadState.IDLE) {
            state.prescriptionEnvironment = RecommendationPolicy.recommend(state.profile, state.weather).primaryEnvironment
        }
        when (route) {
            AppRoute.PRESCRIPTION -> if (state.exerciseLoadState == ExerciseLoadState.IDLE) refreshPrescription()
            AppRoute.PLAN -> if (state.planLoadState == ExerciseLoadState.IDLE) refreshPlan()
            else -> Unit
        }
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
            weatherRefreshAction = ::refreshWeather,
            districtChooseAction = ::chooseDistrict,
            deviceLocationAction = ::useDeviceLocation,
            profileSaveAction = ::saveProfile,
            prescriptionRefreshAction = ::refreshPrescription,
            planRefreshAction = ::refreshPlan,
            videoOpenAction = ::openVideo,
            planVideoOpenAction = ::openPlanVideo,
        )
        when (route) {
            AppRoute.HOME -> HomeScreen.render(scope)
            AppRoute.PRESCRIPTION -> PrescriptionScreen.render(scope)
            AppRoute.PLAN -> ExercisePlanScreen.render(scope)
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
        top.addView(ui.iconButton(R.drawable.ic_sunny, "현재 날씨 보기") {
            navigate(AppRoute.WEATHER)
        })
        ui.add(root, top, 60)
    }

    private fun renderBottomNavigation() {
        ui.divider(root, 0)
        val bar = ui.row()
        listOf(
            Triple(AppRoute.HOME, "홈", R.drawable.ic_home),
            Triple(AppRoute.PRESCRIPTION, "운동 영상", R.drawable.ic_play_arrow),
            Triple(AppRoute.PLAN, "계획서", R.drawable.ic_fitness_center),
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

    private fun openPrescription(environment: ExerciseEnvironment) {
        if (!state.profile.isComplete) {
            state.profileStep = 0
            navigate(AppRoute.PROFILE_SETUP)
            return
        }
        state.prescriptionEnvironment = environment
        state.planResult = null
        state.planLoadState = ExerciseLoadState.IDLE
        planRequestId++
        state.exerciseResult = null
        state.exerciseError = null
        state.exerciseLoadState = ExerciseLoadState.LOADING
        state.prescriptionComplete = false
        navigate(AppRoute.PRESCRIPTION)
        fetchPrescription()
    }

    private fun refreshPrescription() {
        state.exerciseResult = null
        state.exerciseError = null
        state.exerciseLoadState = ExerciseLoadState.LOADING
        render()
        fetchPrescription()
    }

    private fun fetchPrescription() {
        val requestId = ++exerciseRequestId
        exerciseApiClient.recommendations(state.profile, state.prescriptionEnvironment) { result ->
            if (requestId != exerciseRequestId || isFinishing || isDestroyed) return@recommendations
            result.fold(
                onSuccess = {
                    state.exerciseResult = it
                    state.exerciseLoadState = ExerciseLoadState.READY
                    state.exerciseError = null
                },
                onFailure = {
                    state.exerciseResult = null
                    state.exerciseLoadState = ExerciseLoadState.ERROR
                    state.exerciseError = it.message ?: "운동 정보를 불러오지 못했습니다."
                },
            )
            if (route == AppRoute.PRESCRIPTION) render()
        }
    }

    private fun refreshPlan() {
        state.planResult = null
        state.planError = null
        state.planLoadState = ExerciseLoadState.LOADING
        render()
        val requestId = ++planRequestId
        exerciseApiClient.plan(state.profile, state.prescriptionEnvironment) { result ->
            if (requestId != planRequestId || isFinishing || isDestroyed) return@plan
            result.fold(
                onSuccess = {
                    state.planResult = it
                    state.planLoadState = ExerciseLoadState.READY
                },
                onFailure = {
                    state.planLoadState = ExerciseLoadState.ERROR
                    state.planError = it.message ?: "운동 계획서를 불러오지 못했습니다."
                },
            )
            if (route == AppRoute.PLAN) render()
        }
    }

    private fun openVideo(video: ExerciseVideo) {
        openPlanVideo(video.title, video.videoUrl)
    }

    private fun openPlanVideo(title: String, url: String) {
        startActivity(Intent(this, ExerciseVideoActivity::class.java).apply {
            putExtra(ExerciseVideoActivity.EXTRA_TITLE, title)
            putExtra(ExerciseVideoActivity.EXTRA_VIDEO_URL, url)
        })
    }

    private fun refreshWeather() {
        lastWeatherRefreshAt = SystemClock.elapsedRealtime()
        weatherRequestId++
        deviceLocationProvider.cancel()
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        state.weatherLoadState = WeatherLoadState.LOADING
        state.weatherError = null
        render()

        val district = SeoulDistrict.find(state.manualDistrict)
        if (district != null) {
            loadWeatherAt(district.latitude, district.longitude, ++weatherRequestId, district.label)
        } else if (hasFineLocation || hasCoarseLocation) {
            requestDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun requestDeviceLocation() {
        val requestId = ++weatherRequestId
        deviceLocationProvider.getCurrentLocation { locationResult ->
            if (requestId != weatherRequestId) return@getCurrentLocation
            locationResult.fold(
                onSuccess = { location ->
                    state.location = "현재 위치 확인됨"
                    loadWeatherAt(location.latitude, location.longitude, requestId)
                },
                onFailure = {
                    state.location = "위치를 확인하지 못함"
                    showWeatherError(it.message ?: "현재 위치를 확인하지 못했습니다.")
                },
            )
        }
    }

    /** [fixedLabel] is shown for a picked district; a device fix is named by reverse geocoding instead. */
    private fun loadWeatherAt(latitude: Double, longitude: Double, requestId: Int, fixedLabel: String? = null) {
        state.latitude = latitude
        state.longitude = longitude
        fixedLabel?.let { state.location = it }
        render()
        if (fixedLabel == null) {
            weatherApiClient.getCurrentLocationName(latitude, longitude) { nameResult ->
                if (requestId != weatherRequestId) return@getCurrentLocationName
                nameResult.onSuccess { name ->
                    state.location = name
                    render()
                }
            }
        }
        weatherApiClient.getCurrentWeather(latitude, longitude) { weatherResult ->
            if (requestId != weatherRequestId) return@getCurrentWeather
            weatherResult.fold(
                onSuccess = { weather ->
                    state.weather = weather
                    state.location = fixedLabel ?: weather.location
                    state.weatherLoadState = WeatherLoadState.READY
                    state.weatherError = null
                    render()
                },
                onFailure = { showWeatherError(it.message ?: "날씨 정보를 불러오지 못했습니다.") },
            )
        }
    }

    private fun chooseDistrict() {
        val districts = SeoulDistrict.all
        AlertDialog.Builder(this)
            .setTitle("서울 지역 선택")
            .setItems(districts.map { it.name }.toTypedArray()) { _, index ->
                state.manualDistrict = districts[index].name
                userProfileRepository.saveDistrict(state.manualDistrict)
                refreshWeather()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun useDeviceLocation() {
        state.manualDistrict = null
        userProfileRepository.saveDistrict(null)
        state.location = "현재 위치 확인 중"
        refreshWeather()
    }

    private fun showWeatherError(message: String) {
        state.weatherLoadState = WeatherLoadState.ERROR
        state.weatherError = message
        render()
    }

    private fun saveProfile() {
        userProfileRepository.save(state.profile)
        exerciseRequestId++
        planRequestId++
        state.exerciseResult = null
        state.exerciseLoadState = ExerciseLoadState.IDLE
        state.planResult = null
        state.planLoadState = ExerciseLoadState.IDLE
    }

    override fun onDestroy() {
        weatherRequestId++
        exerciseRequestId++
        planRequestId++
        deviceLocationProvider.cancel()
        weatherApiClient.close()
        exerciseApiClient.close()
        super.onDestroy()
    }

    private companion object {
        const val WEATHER_REFRESH_INTERVAL_MS = 15 * 60 * 1000L
    }
}
