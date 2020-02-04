package com.netherpyro.glcv.extensions

import android.graphics.Color

/**
 * @author mmikhailov on 2019-11-30.
 */
fun Int.red(): Float = Color.red(this).normalize()
fun Int.green(): Float = Color.green(this).normalize()
fun Int.blue(): Float = Color.blue(this).normalize()
fun Int.alpha(): Float = Color.alpha(this).normalize()

private fun Int.normalize(): Float {
    return (this / 255f)
}
