package com.netherpyro.glcv.util

import android.content.Context
import com.netherpyro.glcv.extensions.vibrate

/**
 * @author Alexei Korshun on 16.06.2020.
 */
internal class HapticUtil(
        private val context: Context
) {

    private val throttler = Throttler(500)

    fun vibrate() {
        throttler.publish { context.vibrate() }
    }
}