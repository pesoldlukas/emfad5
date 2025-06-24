package com.emfad.app.ui.models

import com.emfad.app.models.MeasurementResult
import com.emfad.app.models.MeasurementStatistics

data class AnalysisState(
    val selectedSession: String? = null,
    val sessionMeasurements: List<MeasurementResult> = emptyList(),
    val sessionStatistics: MeasurementStatistics? = null,
    val measurementUnit: String = "µT",
    val isLoading: Boolean = false,
    val error: String? = null
) 