package com.netherpyro.glcv.compose.media

/**
 * @author mmikhailov on 05.04.2020.
 */
enum class Orientation(value: Int) {
    DEG_0(0),
    DEG_90(90),
    DEG_180(180),
    DEG_270(270)
}

fun Int.toOrientation(): Orientation {
    return when (this) {
        90 -> Orientation.DEG_90
        180 -> Orientation.DEG_180
        270 -> Orientation.DEG_270
        else -> Orientation.DEG_0
    }
}