package com.netherpyro.glcv.compose

import android.net.Uri
import android.opengl.EGLContext
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.media.Constant
import com.netherpyro.glcv.compose.media.Type
import com.netherpyro.glcv.compose.media.Util
import com.netherpyro.glcv.compose.template.Template

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Wrapper around GlComposableView with timing capabilities
 */
class Composer {

    companion object {
        private const val TAG = "Composer"
    }

    @ColorInt
    var viewportColor: Int = 0
        private set

    @ColorInt
    var baseColor: Int = 0
        private set

    var aspectRatio: Float = 1f
        private set

    private var glView: GlComposableView? = null

    private val mediaSeqs = mutableSetOf<Sequence>()
    private val transformables = mutableSetOf<Transformable>()

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

    fun setAspectRatio(aspect: Float, animated: Boolean = false) {
        this.aspectRatio = aspect
        glView?.setAspectRatio(aspect, animated)
    }

    fun setBaseColor(@ColorInt color: Int) {
        this.baseColor = color
        glView?.setBaseColor(color)
    }

    fun setViewportColor(@ColorInt color: Int) {
        this.viewportColor = color
        glView?.setViewportColor(color)
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

    /**
     * Adds an image to project
     *
     * @param tag is a unique identifier for added image'
     * @param src is a CONTENT URI of image file;
     * @param zOrderDirection direction of z-order at layer list to add the image;
     * @param startMs specifies when image should start display at project timeline. In milliseconds. Cannot be < 0;
     * @param durationMs of image visibility at project timeline. In milliseconds. Cannot be < 1000;
     * */
    fun addImage(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startMs: Long = 0L,
            durationMs: Long = Constant.DEFAULT_IMAGE_DURATION_MS
    ) {
        checkGlView("addImage") {
            val view = glView!!
            val metadata = Util.getMetadata(
                    view.context,
                    src,
                    defaultImageDuration = Constant.DEFAULT_IMAGE_DURATION_MS
            )

            if (metadata.type != Type.IMAGE) {
                Log.e(TAG, "addImage::provided URI is not an image identifier")
                return
            }

            view.addBitmapLayer(
                    tag,
                    bitmap = Util.getBitmap(view.context, src),
                    position = zOrderDirection.toGlRenderPosition(),
                    onTransformable = { this@Composer.transformables.add(it) }
            )

            mediaSeqs.add(Sequence(
                    tag = tag,
                    uri = src,
                    startDelayMs = startMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(1000L)
            ))

            //todo return Controllable
        }
    }

    /*fun addVideo(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startDelayMs: Long = 0,
            duration: Long = Constant.DEFAULT_IMAGE_DURATION_MS
    ) {
        checkGlView("addMedia") {
            val view = glView!!
            val metadata = Util.getMetadata(
                    view.context,
                    src,
                    defaultImageDuration = Constant.DEFAULT_IMAGE_DURATION_MS
            )

            when (metadata.type) {
                Type.VIDEO -> view.addSurfaceLayer(
                        tag,
                        surfaceConsumer = decoders.createSurfaceConsumer(tag, view.context, src),
                        position = zOrderDirection.toGlRenderPosition(),
                        onTransformable = { this@Composer.transformables.add(it) }
                )
                Type.IMAGE -> view.addBitmapLayer(
                        tag,
                        bitmap = Util.getBitmap(view.context, src),
                        position = zOrderDirection.toGlRenderPosition(),
                        onTransformable = { this@Composer.transformables.add(it) }
                )
            }


            // todo convert input data to sequence
            // todo modify sequence list
            // todo apply to glView
        }
    }*/

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
