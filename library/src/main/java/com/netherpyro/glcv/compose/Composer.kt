package com.netherpyro.glcv.compose

import android.net.Uri
import android.opengl.EGLContext
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.template.Template
import com.netherpyro.glcv.compose.template.ZOrderPosition

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

    private val mediaSeqs = mutableListOf<Sequence>()
    private val transformables = mutableListOf<Transformable>()

    fun getSharedEglContext(): EGLContext? = // todo get blocking
            null//glComposableView?.enqueueEvent( Runnable { EGL14.eglGetCurrentContext() })

    fun takeTemplate(): Template {
        if (transformables.isEmpty()) {
            throw IllegalStateException("Cannot take template without GL layers set")
        }

        return Template.from(aspectRatio, mediaSeqs, transformables)
    }

    fun applyTemplate(template: Template) {
        // todo validate template param

        mediaSeqs.clear()
        mediaSeqs.addAll(template.toSequences())
        // todo add layers to gl view if exists
    }

    /**
     * Sets GlSurfaceView
     *
     * If you want set GlSurfaceView up with template should apply template [applyTemplate] first,
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

    fun addMedia(tag: String, src: Uri, zOrderPosition: ZOrderPosition = ZOrderPosition.TOP) {
        // todo convert input data to sequence
        // todo modify sequence list
        // todo apply to glView if exists
    }

    fun removeMedia(tag: String) {
        // todo modify sequence list
        // todo apply to glView if exists
    }

    fun moveMediaLayerStepForward(tag: String) {
        // todo modify sequence list
        // todo apply to glView if exists
    }

    fun moveMediaLayerStepBackwards(tag: String) {
        // todo modify sequence list
        // todo apply to glView if exists
    }
}
