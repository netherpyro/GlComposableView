package com.netherpyro.glcv

import com.google.android.exoplayer2.SimpleExoPlayer
import com.netherpyro.glcv.layer.ExoPLayer
import com.netherpyro.glcv.layer.Layer

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class GlRenderMediator(private val renderHost: RenderHost) : Invalidator {

    private val layers = mutableListOf<Layer>()

    override fun invalidate() {
        render()
    }

    fun addLayer(layer: Layer) {
        layers.add(layer)
    }

    fun addExoPlayerLayer(player: SimpleExoPlayer) {
        addLayer(ExoPLayer(player, this))
    }

    fun onSurfaceCreated() {
        layers.forEach { it.onGlPrepared() }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        renderHost.onSurfaceChanged(width, height)
    }

    fun onViewportChanged(viewport: GlViewport) {
        val aspect = viewport.width / viewport.height.toFloat()
        layers.forEach { it.onViewportAspectRatioChanged(aspect) }

        render()
    }

    fun onDrawFrame(fbo: FramebufferObject) {
        layers.forEach { it.onDrawFrame() }
    }

    private fun render() {
        renderHost.requestDraw()
    }
}