package com.example.sposwitch.model

import com.example.sposwitch.R

/** Facility information displayed by the location recommendation mock. */
internal data class Facility(
    val name: String,
    val indoor: Boolean,
    val distance: String,
    val sport: String,
    val address: String,
) {
    val image: Int get() = if (indoor) R.drawable.indoor_hero else R.drawable.track_hero
    val category: String get() = if (indoor) "실내" else "야외"
}
