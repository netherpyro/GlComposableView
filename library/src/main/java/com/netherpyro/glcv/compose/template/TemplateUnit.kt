package com.netherpyro.glcv.compose.template

import android.net.Uri

/**
 * @author mmikhailov on 05.04.2020.
 */
data class TemplateUnit(
        val tag: String,
        val uri: Uri,
        val startDelayMs: Long,
        val trimmedDurationMs: Long,
        val zPosition: Int,
        val scaleFactor: Float,
        val rotationDeg: Float,
        val translateFactorX: Float,
        val translateFactorY: Float,
        val opacity: Float
)