package com.netherpyro.glcv.compose

import android.net.Uri
import android.opengl.EGLContext
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.template.Template

/**
 * @author mmikhailov on 28.03.2020.
 */
class Composer {

    companion object {
        private const val TAG = "Composer"
    }

    @ColorInt
    var viewportColor: Int = 0
        set(value) {
            field = value
            glView?.setViewportColor(value)
        }

    @ColorInt
    var baseColor: Int = 0
        set(value) {
            field = value
            glView?.setBaseColor(value)
        }

    var aspectRatio: Float = 1f
        set(value) {
            field = value
            glView?.setAspectRatio(value)
        }

    private val mediaSeqs = mutableSetOf<Sequence>()
    private val transformables = mutableSetOf<Transformable>()

    private var glView: GlComposableView? = null

    fun setGlView(glComposableView: GlComposableView) {
        glView = glComposableView

        glComposableView.setViewportColor(viewportColor)
        glComposableView.setBaseColor(baseColor)
        glComposableView.setAspectRatio(aspectRatio)

        try {
            applyTemplate(takeTemplate())
        } catch (e: IllegalStateException) {
            Log.i(TAG, "setGlView::this is first attempt setting GlComposableView")
        }
    }

    fun getSharedEglContext(): EGLContext? = // todo get blocking
            null//glComposableView?.enqueueEvent( Runnable { EGL14.eglGetCurrentContext() })

    fun takeTemplate(): Template {
        if (transformables.isEmpty()) {
            throw IllegalStateException(
                    "Cannot take template without GL layers added. Use Composer#addMedia method to add a layer"
            )
        }

        return Template.from(aspectRatio, mediaSeqs, transformables)
    }

    fun applyTemplate(template: Template) {
        checkGlView("applyTemplate") {
            mediaSeqs.clear()
            mediaSeqs.addAll(template.toSequences())
            // todo add layers to gl view
        }
    }

    fun addMedia(tag: String, src: Uri, zOrderDirection: ZOrderDirection = ZOrderDirection.TOP) {
        checkGlView("addMedia") {
            // todo convert input data to sequence
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun removeMedia(tag: String) {
        checkGlView("removeMedia") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun moveMediaLayerStepForward(tag: String) {
        checkGlView("moveMediaLayerStepForward") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun moveMediaLayerStepBackwards(tag: String) {
        checkGlView("moveMediaLayerStepBackwards") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    private inline fun checkGlView(op: String, block: () -> Unit) {
        if (glView == null) {
            Log.e(TAG, "$op::Cannot perform action without GlComposableView set")
            return
        }

        block()
    }
}
