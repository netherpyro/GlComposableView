package com.netherpyro.glcv.util

import android.content.Context
import com.netherpyro.glcv.extensions.vibrate

/**
 * @author Alexei Korshun on 16.06.2020.
 */
internal class Haptic(
        private val context: Context
) {

    var enabled: Boolean = false

    private val throttler = Throttler(500)

    fun perform() {
        if (!enabled)
            return

        throttler.publish { context.vibrate() }
    }
}