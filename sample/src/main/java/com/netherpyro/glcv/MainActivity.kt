package com.netherpyro.glcv

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author mmikhailov on 2019-11-30.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer

    private val transformableList = mutableListOf<Transformable>()

    private val maxTranslation = 2f
    private val minTranslation = -2f

    private var frontIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LibraryHelper.setContext(applicationContext)

        controlView.showTimeoutMs = -1
        controlView.setShowMultiWindowTimeBar(true)
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "GlComposableView"))
        val videoSource1: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video1())
        val videoSource2: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video2())
        val videoSource3: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(LibraryHelper.video3())
        val concatenatedSource = ConcatenatingMediaSource(videoSource1, videoSource2, videoSource3)

        player = ExoPlayerFactory.newSimpleInstance(this)

        controlView.player = player
        player.prepare(concatenatedSource)
        player.playWhenReady = true

        glView.setAspectsPreset(AspectRatio.values().map { it.value })

        // add video layer
        transformableList.add(glView.addVideoLayer(player = player, applyLayerAspect = true))
        // add image 1 layer
        LibraryHelper.image1()
            ?.also { transformableList.add(glView.addImageLayer(bitmap = it)) }
        // add image 2 layer
        LibraryHelper.image2()
            ?.also { transformableList.add(glView.addImageLayer(bitmap = it)) }

        frontIndex = transformableList.lastIndex

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
        layer1ToFront.setOnClickListener {
            frontIndex = 0
            glView.bringToFront(transformableList[frontIndex])
        }
        layer2ToFront.setOnClickListener {
            frontIndex = 1
            glView.bringToFront(transformableList[frontIndex])
        }
        layer3ToFront.setOnClickListener {
            frontIndex = 2
            glView.bringToFront(transformableList[frontIndex])
        }
        restoreReordering.setOnClickListener {
            frontIndex = transformableList.lastIndex
            glView.restoreOrder()
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

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
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

        translationXSeek.progress = 50
        translationXSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val f = progress / 100f
                val value = minTranslation * (1f - f) + maxTranslation * f
                translationYSeek.progress = 50
                transformableList[frontIndex].setTranslation(value, 0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                transformableList.forEachIndexed { index, transformable ->
                    if (index != frontIndex) transformable.setOpacity(0.3f)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                transformableList.forEach { it.setOpacity(1f) }
            }
        })

        translationYSeek.progress = 50
        translationYSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val f = progress / 100f
                val value = minTranslation * (1f - f) + maxTranslation * f
                translationXSeek.progress = 50
                transformableList[frontIndex].setTranslation(0f, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                transformableList.forEachIndexed { index, transformable ->
                    if (index != frontIndex) transformable.setOpacity(0.3f)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                transformableList.forEach { it.setOpacity(1f) }
            }
        })

        borderSeek.progress = 0
        borderSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                transformableList[frontIndex].setBorder(progress / 10f, Color.GREEN)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
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
}