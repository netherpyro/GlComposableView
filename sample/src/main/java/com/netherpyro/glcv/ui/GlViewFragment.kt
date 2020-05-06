package com.netherpyro.glcv.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.netherpyro.glcv.AspectRatio
import com.netherpyro.glcv.LibraryHelper
import com.netherpyro.glcv.R
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.addDivider
import com.netherpyro.glcv.alsoOnLaid
import com.netherpyro.glcv.compose.playback.TaggedSilenceMediaSource
import com.netherpyro.glcv.touches.LayerTouchListener
import kotlinx.android.synthetic.main.f_glcv.*
import kotlin.math.abs

/**
 * @author mmikhailov on 30.04.2020.
 */
class GlViewFragment : Fragment() {

    companion object {
        private const val TAG = "GlViewFragment"
    }

    private lateinit var player: SimpleExoPlayer

    private val transformableList = mutableListOf<Transformable>()

    private val videoTag = "VIDEO_TAG"

    private val videoListener = object : VideoListener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                        pixelWidthHeightRatio: Float) {
            transformableList.findTransformable(videoTag)
                ?.setSize((width * pixelWidthHeightRatio).toInt(), (height * pixelWidthHeightRatio).toInt())
        }

        override fun onRenderedFirstFrame() {}
        override fun onSurfaceSizeChanged(width: Int, height: Int) {}
    }

    private val aspectRatioAdapter: AspectRatioAdapter by lazy {
        AspectRatioAdapter(
                AspectRatio.values()
                    .map { ar ->
                        AspectRatioItem(ar, ar.title, ar.value in glView.aspectRatio - 0.1f..glView.aspectRatio + 0.1f)
                    }
        ) { selectedAspectRatio -> glView.setAspectRatio(selectedAspectRatio.value, true) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.f_glcv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        LibraryHelper.setContext(requireContext())

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                requireContext(), Util.getUserAgent(requireContext(), "GlComposableView"))
        val videoSource1: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video1())
        val videoSource2: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video2())
        val videoSource3: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video3())
        val silenceSource: MediaSource = TaggedSilenceMediaSource(
                10_000_000, "shh")
        val concatenatedSource = ConcatenatingMediaSource(videoSource3, videoSource2, videoSource1, silenceSource)

        player = SimpleExoPlayer.Builder(requireContext()).build()
        player.addVideoListener(videoListener)
        player.prepare(concatenatedSource)
        player.playWhenReady = true
        player.addListener(object : Player.EventListener {
            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
                transformableList.findTransformable(videoTag)
                    ?.setSkipDraw(trackGroups.isSilence())

                glView.requestRender()
            }
        })

        glView.enableGestures = true

        // add bitmap 1 layer
        LibraryHelper.image1()
            ?.also { bitmap ->
                glView.addBitmapLayer(bitmap = bitmap) { transformable -> transformableList.add(transformable) }
            }
        // add bitmap 2 layer
        LibraryHelper.image2()
            ?.also { bitmap ->
                glView.addBitmapLayer(bitmap = bitmap) { transformable ->
                    transformableList.add(transformable)
                    applyAspectRatio(transformable.getLayerAspect()) // use initial aspect of second image added
                }
            }

        // add surface layer
        glView.addSurfaceLayer(
                tag = videoTag,
                surfaceConsumer = SurfaceConsumer { player.setVideoSurface(it) }
        ) { transformable -> transformableList.add(transformable) }

        v1.setOnClickListener { player.seekTo(0, 0) }
        v2.setOnClickListener { player.seekTo(1, 0) }
        v3.setOnClickListener { player.seekTo(2, 3000) }

        with(rv_aspect_ratio) {
            addDivider()
            adapter = aspectRatioAdapter
        }

        bottomView.alsoOnLaid { bottomView ->
            val maxHeight = container.height / 2
            bottomSeek.progress = ((bottomView.height / maxHeight.toFloat()) * 100).toInt()
            glView.setViewportMargin(bottom = bottomView.height)

            bottomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    bottomView.layoutParams = bottomView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportMargin(bottom = viewHeight)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        topView.alsoOnLaid { topView ->
            val maxHeight = container.height / 2
            topSeek.progress = ((topView.height / maxHeight.toFloat()) * 100).toInt()
            glView.setViewportMargin(top = topView.height)

            topSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    topView.layoutParams = topView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportMargin(top = viewHeight)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }

        glView.listenTouches(object : LayerTouchListener {
            override fun onLayerTap(transformable: Transformable): Boolean {
                transformableList.forEach {
                    val clicked = it.id == transformable.id
                    it.enableGesturesTransform = clicked
                    it.setBorder(if (clicked) 1f else 0f, Color.GREEN)

                    if (clicked) it.setLayerPosition(transformableList.lastIndex)
                }

                return true
            }

            override fun onViewportInsideTap(): Boolean {
                transformableList.forEach {
                    it.enableGesturesTransform = false
                    it.setBorder(0f, Color.GREEN)
                }

                return true
            }

            override fun onViewportOutsideTap(): Boolean {
                transformableList.forEach {
                    if (it.enableGesturesTransform) {
                        it.setBorder(1f, Color.BLUE)
                    }
                }

                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    override fun onDestroyView() {
        rv_aspect_ratio.adapter = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        player.stop()
        player.release()
    }

    private fun applyAspectRatio(layerAspectValue: Float) {
        val nearestAspect = AspectRatio.values()
            .minBy { ar -> abs(layerAspectValue - ar.value) }!!

        Log.d(TAG, "nearest aspect=$nearestAspect")

        glView.setAspectRatio(nearestAspect.value)
    }

    private fun List<Transformable>.findTransformable(tag: String): Transformable? =
            this.firstOrNull { it.tag == tag }

    private fun TrackGroupArray.isSilence() =
            !this.isEmpty && this[0].getFormat(0).id == "shh"
}