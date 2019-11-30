package com.netherpyro.glcv

import android.content.Context
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.google.android.exoplayer2.SimpleExoPlayer
import com.netherpyro.glcv.util.EConfigChooser
import com.netherpyro.glcv.util.EContextFactory

/**
 * @author mmikhailov on 2019-10-26.
 *
 */
class GlComposableView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), RenderHost {

    private val renderer: GlRenderer
    private val renderMediator: GlRenderMediator
    private val layoutHelper: GlLayoutHelper

    @ColorInt
    private val defaultBaseColor: Int = Color.parseColor("#5555ff")
    @ColorInt
    private val defaultViewportColor: Int = Color.parseColor("#ff5555")
    private val defaultViewportAspectRatio = 1f

    init {
        setEGLContextFactory(EContextFactory())
        setEGLConfigChooser(EConfigChooser())

        layoutHelper = GlLayoutHelper(defaultViewportAspectRatio)
        renderMediator = GlRenderMediator(this)
        renderer = GlRenderer(renderMediator, defaultBaseColor, defaultViewportColor)

        setRenderer(renderer)
    }

    override fun requestDraw() {
        queueEvent { requestRender() }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        val viewport = layoutHelper.onSurfaceChanged(width, height)
        renderMediator.onViewportChanged(viewport)
        renderer.setViewport(viewport)

        requestDraw()
    }

    fun addVideoLayer(player: SimpleExoPlayer) {
        renderMediator.addVideoLayer(player)
    }

    fun setAspectRatio(aspect: Float) {
        val viewport = layoutHelper.changeAspectRatio(aspect)
        renderMediator.onViewportChanged(viewport)
        renderer.setViewport(viewport)

        requestDraw()
    }

    fun setBaseColor(@ColorInt color: Int) {
        renderer.backgroundColor = color

        requestDraw()
    }

    fun setViewportColor(@ColorInt color: Int) {
        renderer.viewportColor = color

        requestDraw()
    }
}