package com.netherpyro.glcv

import android.os.Bundle
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

        glView.addVideoLayer(player)

        a1_1.setOnClickListener { glView.setAspectRatio(1f) }
        a3_2.setOnClickListener { glView.setAspectRatio(3 / 2f) }
        a2_3.setOnClickListener { glView.setAspectRatio(2 / 3f) }
        a4_5.setOnClickListener { glView.setAspectRatio(4 / 5f) }
        a5_4.setOnClickListener { glView.setAspectRatio(5 / 4f) }
        a9_16.setOnClickListener { glView.setAspectRatio(9 / 16f) }
        a16_9.setOnClickListener { glView.setAspectRatio(16 / 9f) }
        a18_9.setOnClickListener { glView.setAspectRatio(18 / 9f) }
        a9_18.setOnClickListener { glView.setAspectRatio(9 / 18f) }
        v1.setOnClickListener { player.seekTo(0, 0) }
        v2.setOnClickListener { player.seekTo(1, 0) }
        v3.setOnClickListener { player.seekTo(2, 3000) }
        ml.setOnClickListener {
            //ePlayerView.setGlPadding();
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

    private val currentMl = 0
    private val currentMt = 0
    private val currentMr = 0
    private val currentMb = 0
    private val margins = intArrayOf(0, 150, 300)

    private fun nextMargin(curIdx: Int): Int {
        val res: Int = if (curIdx == margins.size - 1) 0
        else curIdx + 1

        return margins[res]
    }
}