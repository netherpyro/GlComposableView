package com.netherpyro.glcv.compose

import android.net.Uri

/**
 * @author mmikhailov on 03.04.2020.
 */
class MediaLayer(
        val tag: String,
        val uri: Uri,
        val type: LayerType,
        val width: Int,
        val height: Int,
        var zPosition: Int,
        var durationMs: Long,
        var startDelayMs: Long,
        var trimmedDuration: Long,
        var scaleFactor: Float,
        var rotationDeg: Float,
        var translateFactorX: Float,
        var translateFactorY: Float
) {
}

enum class LayerType {
    VIDEO, IMAGE
}