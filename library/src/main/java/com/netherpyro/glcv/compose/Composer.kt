package com.netherpyro.glcv.compose

import android.opengl.EGLContext
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.GlRenderer
import java.io.File

/**
 * @author mmikhailov on 28.03.2020.
 */
class Composer {

    companion object {
        private const val TAG = "Composer"
    }

    private var glComposableView: GlComposableView? = null

    @ColorInt
    var viewportColor: Int = 0
        set(value) {
            field = value
            glComposableView?.setViewportColor(value)
        }

    @ColorInt
    var baseColor: Int = 0
        set(value) {
            field = value
            glComposableView?.setBaseColor(value)
        }

    var aspectRatio: Float = 1f
        set(value) {
            field = value
            glComposableView?.setAspectRatio(value)
        }

    private val mediaLayers = mutableListOf<MediaLayer>()

    fun getSharedEglContext(): EGLContext? = // todo get blocking
            null//glComposableView?.enqueueEvent( Runnable { EGL14.eglGetCurrentContext() })

    fun takeLayerTemplate() = LayerTemplate.from(mediaLayers)

    fun takeSnapshot() = Snapshot(takeLayerTemplate(), viewportColor, aspectRatio)

    fun applyLayerTemplate(template: LayerTemplate) {
        // todo validate template param
        // todo set up media layers according template
    }

    /**
     * Sets GlSurfaceView
     *
     * If you want set GlSurfaceView up with template should call [applyLayerTemplate] first,
     * will be set with current media layers otherwise
     *
     * */
    fun setGlView(glView: GlComposableView) {
        glComposableView = glView

        glView.setViewportColor(viewportColor)
        glView.setBaseColor(baseColor)
        glView.setAspectRatio(aspectRatio)

        // todo add media layers to glView
    }

    fun addLayer(tag: String, src: File, zOrderPosition: ZOrderPosition = ZOrderPosition.TOP) {
        // todo modify media layer list
        // todo apply to glView if exists
    }

    fun removeLayer(tag: String) {
        // todo modify media layer list
        // todo apply to glView if exists
    }

    fun moveLayerStepForward(tag: String) {
        // todo modify media layer list
        // todo apply to glView if exists
    }

    fun moveLayerStepBackwards(tag: String) {
        // todo modify media layer list
        // todo apply to glView if exists
    }

    class Snapshot(
            template: LayerTemplate,
            @ColorInt
            val viewportColor: Int,
            val aspectRatio: Float
    ) {
        private val layers: List<MediaLayer> = template.toLayers()
        private val timeMask = TimeMask.from(layers)

        fun invalidateLayersVisibility(timestampMs: Long) {
            timeMask.takeVisibilityStatus(timestampMs)
                .forEach { entry ->
                    layers.find { l -> l.tag == entry.key }?.isVisible = entry.value
                }
        }

        fun totalDurationMs() = timeMask.durationMs

        fun setupWithLayers(glRenderer: GlRenderer) {
            // todo add media layers to renderer
        }
    }
}
