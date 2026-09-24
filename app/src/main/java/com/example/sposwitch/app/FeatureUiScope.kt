package com.example.sposwitch.app

import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseVideo
import com.example.sposwitch.ui.theme.MockUi

/** Dependencies and navigation callbacks available to a feature screen renderer. */
internal class FeatureUiScope(
    val activity: AppCompatActivity,
    val ui: MockUi,
    val content: LinearLayout,
    val state: AppState,
    private val navigateAction: (AppRoute, Boolean) -> Unit,
    private val renderAction: () -> Unit,
    private val prescriptionAction: (ExerciseEnvironment) -> Unit,
    private val weatherRefreshAction: () -> Unit,
    private val profileSaveAction: () -> Unit,
    private val prescriptionRefreshAction: () -> Unit,
    private val planRefreshAction: () -> Unit,
    private val videoOpenAction: (ExerciseVideo) -> Unit,
    private val planVideoOpenAction: (String, String) -> Unit,
) {
    fun navigate(route: AppRoute, asTab: Boolean = false) = navigateAction(route, asTab)
    fun rerender() = renderAction()
    fun openPrescription(environment: ExerciseEnvironment) = prescriptionAction(environment)
    fun refreshWeather() = weatherRefreshAction()
    fun saveProfile() = profileSaveAction()
    fun refreshPrescription() = prescriptionRefreshAction()
    fun refreshPlan() = planRefreshAction()
    fun openVideo(video: ExerciseVideo) = videoOpenAction(video)
    fun openPlanVideo(title: String, url: String) = planVideoOpenAction(title, url)
}
