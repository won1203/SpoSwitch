package com.example.sposwitch.app

import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import com.example.sposwitch.ui.theme.MockUi

/** Dependencies and navigation callbacks available to a feature screen renderer. */
internal class FeatureUiScope(
    val activity: AppCompatActivity,
    val ui: MockUi,
    val content: LinearLayout,
    val state: AppState,
    private val navigateAction: (AppRoute, Boolean) -> Unit,
    private val renderAction: () -> Unit,
    private val prescriptionAction: (Boolean) -> Unit,
    private val weatherRefreshAction: () -> Unit,
) {
    val recommendsIndoor: Boolean get() = state.recommendsIndoor

    fun navigate(route: AppRoute, asTab: Boolean = false) = navigateAction(route, asTab)
    fun rerender() = renderAction()
    fun openPrescription(indoor: Boolean) = prescriptionAction(indoor)
    fun refreshWeather() = weatherRefreshAction()
}
