package com.netherpyro.glcv

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

        glView.addExoPlayerLayer(player)

        LibraryHelper.image1()?.also {
            glView.addImageLayer(it)
        }

        a1_1.setOnClickListener { glView.setAspectRatio(1f, true) }
        a3_2.setOnClickListener { glView.setAspectRatio(3 / 2f, true) }
        a2_3.setOnClickListener { glView.setAspectRatio(2 / 3f, true) }
        a4_5.setOnClickListener { glView.setAspectRatio(4 / 5f, true) }
        a5_4.setOnClickListener { glView.setAspectRatio(5 / 4f, true) }
        a9_16.setOnClickListener { glView.setAspectRatio(9 / 16f, true) }
        a16_9.setOnClickListener { glView.setAspectRatio(16 / 9f, true) }
        a18_9.setOnClickListener { glView.setAspectRatio(18 / 9f, true) }
        a9_18.setOnClickListener { glView.setAspectRatio(9 / 18f, true) }
        v1.setOnClickListener { player.seekTo(0, 0) }
        v2.setOnClickListener { player.seekTo(1, 0) }
        v3.setOnClickListener { player.seekTo(2, 3000) }

        bottomView.alsoOnLaid { bottomView ->
            val maxHeight = container.height / 2
            bottomSeek.progress = ((bottomView.height / maxHeight.toFloat()) * 100).toInt()
            glView.setViewportPadding(bottom = bottomView.height)

            bottomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    bottomView.layoutParams = bottomView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportPadding(bottom = viewHeight)
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
            glView.setViewportPadding(top = topView.height)

            topSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    topView.layoutParams = topView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportPadding(top = viewHeight)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }
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