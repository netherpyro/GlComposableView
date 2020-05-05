package com.netherpyro.glcv;

import android.view.Surface;

import androidx.annotation.NonNull;

/**
 * @author mmikhailov on 04.04.2020.
 */
public interface SurfaceConsumer {
    void consume(@NonNull Surface surface);
}
