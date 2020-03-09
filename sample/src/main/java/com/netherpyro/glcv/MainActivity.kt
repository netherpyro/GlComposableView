package com.netherpyro.glcv

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
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
import com.netherpyro.glcv.touches.LayerTouchListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

/**
 * @author mmikhailov on 2019-11-30.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer

    private val transformableList = mutableListOf<Transformable>()

    private val videoListener = object : VideoListener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                        pixelWidthHeightRatio: Float) {
            transformableList.findVideoTransformable()
                ?.setVideoSize(width * pixelWidthHeightRatio, height * pixelWidthHeightRatio)
        }

        override fun onRenderedFirstFrame() {}
        override fun onSurfaceSizeChanged(width: Int, height: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LibraryHelper.setContext(applicationContext)

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "GlComposableView"))
        val videoSource1: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video1())
        val videoSource2: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video2())
        val videoSource3: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video3())
        val silenceSource: MediaSource = MySilenceMediaSource(10_000_000)
        val concatenatedSource = ConcatenatingMediaSource(videoSource3, videoSource2, videoSource1, silenceSource)

        player = ExoPlayerFactory.newSimpleInstance(this)
        player.addVideoListener(videoListener)

        player.prepare(concatenatedSource)
        player.playWhenReady = true
        player.addListener(object : Player.EventListener {

            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray?) {
                transformableList.findVideoTransformable()
                    ?.setSkipDraw(trackGroups.isSilence())

                glView.requestRender()
            }
        })

        glView.enableGestures = true

        // add video layer
        glView.addVideoLayer(
                onSurfaceAvailable = { surface -> player.setVideoSurface(surface) }
        ) { transformable ->
            transformableList.add(transformable)
            // todo investigate video aspect
            Log.i("MainActivity", "layer aspect=${transformable.getLayerAspect()}")
            applyAspectRatio(transformable.getLayerAspect())
        }

        // add image 1 layer
        LibraryHelper.image1()
            ?.also { bitmap ->
                glView.addImageLayer(bitmap = bitmap) { transformable -> transformableList.add(transformable) }
            }
        // add image 2 layer
        LibraryHelper.image2()
            ?.also { bitmap ->
                glView.addImageLayer(bitmap = bitmap) { transformable -> transformableList.add(transformable) }
            }

        a1_1.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_1_1.value, true) }
        a3_2.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_3_2.value, true) }
        a2_3.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_2_3.value, true) }
        a4_5.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_4_5.value, true) }
        a5_4.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_5_4.value, true) }
        a9_16.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_9_16.value, true) }
        a16_9.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_16_9.value, true) }
        a18_9.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_18_9.value, true) }
        a9_18.setOnClickListener { glView.setAspectRatio(AspectRatio.RATIO_9_18.value, true) }
        v1.setOnClickListener { player.seekTo(0, 0) }
        v2.setOnClickListener { player.seekTo(1, 0) }
        v3.setOnClickListener { player.seekTo(2, 3000) }

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

    override fun onDestroy() {
        super.onDestroy()

        player.stop()
        player.release()
    }

    fun <T : View> T.alsoOnLaid(block: (T) -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                block.invoke(this@alsoOnLaid)
            }
        })
    }

    private fun applyAspectRatio(layerAspectValue: Float) {
        val nearestAspect = AspectRatio.values()
            .minBy { ar -> abs(layerAspectValue - ar.value) }!!

        Log.d("MainActivity", "nearest aspect=$nearestAspect")

        glView.setAspectRatio(nearestAspect.value)
    }

    private fun List<Transformable>.findVideoTransformable(): VideoTransformable? =
            (this.firstOrNull { it is VideoTransformable } as? VideoTransformable)

    private fun TrackGroupArray.isSilence() =
            !this.isEmpty && this[0].getFormat(0).id == MySilenceMediaSource.SILENCE_ID
}