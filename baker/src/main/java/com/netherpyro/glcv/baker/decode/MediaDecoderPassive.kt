/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netherpyro.glcv.baker.decode

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.baker.Baker.Companion.VERBOSE_LOGGING
import com.netherpyro.glcv.baker.encode.AudioBuffer
import com.netherpyro.glcv.baker.encode.AudioBufferProvider
import java.io.IOException
import java.nio.ByteBuffer

/**
 * @author mmikhailov on 25.04.2020.
 *
 * Plays the video track from a movie file to a Surface in a passive way.
 * You should call [MediaDecoderPassive#advance] to grab next frame.
 */
// todo resolve audio decoding/encoding issues
internal class MediaDecoderPassive(
        private val context: Context,
        private val tag: String,
        private val uri: Uri,
        private val decodeAudioTrack: Boolean
) : SurfaceConsumer {

    companion object {
        private const val TAG = "MediaDecoderPassive"
        private const val TIMEOUT_USEC = 10000L

        private val VERBOSE = VERBOSE_LOGGING
    }

    private val videoBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private val audioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private lateinit var videoExtractor: MediaExtractor
    private lateinit var audioExtractor: MediaExtractor
    private lateinit var videoDecoder: MediaCodec
    private lateinit var audioDecoder: MediaCodec
    private lateinit var audioBufferProvider: AudioBufferProvider

    private var speedController: SpeedController? = null
    private var outputSurface: Surface? = null
    private var audioBuffer: AudioBuffer? = null

    private var videoTrackInfo: TrackInfo? = null
    private var audioTrackInfo: TrackInfo? = null

    private var videoOutputDone = false
    private var videoInputDone = false
    private var audioOutputDone = false
    private var audioInputDone = false
    private var videoExtractorWarmedUp = false
    private var audioExtractorWarmedUp = false
    private var released = false

    var isUsed = false
        private set

    /**
     * @param surface The Surface where frames will be sent
     */
    override fun consume(surface: Surface) {
        outputSurface = surface
    }

    @Throws(IOException::class)
    fun prepare(audioBufferProvider: AudioBufferProvider) {
        if (::videoExtractor.isInitialized || ::audioExtractor.isInitialized) {
            Log.w(TAG, "advance::decoder already prepared")
            return
        }

        this.audioBufferProvider = audioBufferProvider

        prepareInternal()
    }

    fun release() {
        released = true

        if (::videoDecoder.isInitialized) {
            videoDecoder.stop()
            videoDecoder.release()
        }

        if (::audioDecoder.isInitialized) {
            audioDecoder.stop()
            audioDecoder.release()
        }

        if (::videoExtractor.isInitialized) {
            videoExtractor.release()
        }

        if (::audioExtractor.isInitialized) {
            audioExtractor.release()
        }
    }

    fun advance(ptsUsec: Long) {
        if (released) {
            Log.w(TAG, "advance::decoder was released!")
            return
        }

        if (videoOutputDone && audioOutputDone) {
            Log.w(TAG, "advance::nothing left for playback")
            return
        }

        isUsed = true

        if (speedController?.test(ptsUsec) == false) {
            if (VERBOSE) Log.i(TAG, "advance::skip frame due to frame threshold")
            return
        }

        if (audioTrackInfo != null) {
            advanceAudioExtractor()
        }

        if (videoTrackInfo != null) {
            advanceVideoExtractor()
        }
    }

    @Throws(IOException::class)
    private fun prepareInternal() {
        try {
            videoExtractor = MediaExtractor().apply { setDataSource(context, uri, null) }

            if (decodeAudioTrack) {
                // should be used own extractor instance to avoid performance issues
                audioExtractor = MediaExtractor().apply { setDataSource(context, uri, null) }
            }

            val videoTrackInfo = getTrackInfo(videoExtractor, "video/")
            val audioTrackInfo = if (decodeAudioTrack) getTrackInfo(audioExtractor, "audio/") else null

            if (videoTrackInfo == null && audioTrackInfo == null) {
                throw RuntimeException("prepareInternal::neither video nor audio track selected from $uri")
            }

            if (videoTrackInfo != null) {
                this.videoTrackInfo = videoTrackInfo
                videoExtractor.selectTrack(videoTrackInfo.index)

                val fps = videoTrackInfo.format.getInteger(MediaFormat.KEY_FRAME_RATE)

                Log.d(TAG, "prepareInternal::video frame rate = $fps")

                speedController = SpeedController(fps)

                // Create a MediaCodec decoder, and configure it with the MediaFormat from the
                // extractor. It's very important to use the format from the extractor because
                // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
                val mime = videoTrackInfo.format.getString(MediaFormat.KEY_MIME)
                videoDecoder = MediaCodec.createDecoderByType(mime)
                videoDecoder.configure(videoTrackInfo.format, outputSurface, null, 0)
                videoDecoder.start()

                while (!videoExtractorWarmedUp) {
                    advanceVideoExtractor()
                }
            }

            if (audioTrackInfo != null) {
                this.audioTrackInfo = audioTrackInfo
                this.audioBuffer = audioBufferProvider.provide(tag)
                audioExtractor.selectTrack(audioTrackInfo.index)

                val mime = audioTrackInfo.format.getString(MediaFormat.KEY_MIME)
                // todo handle "audio/unknown" mime
                audioDecoder = MediaCodec.createDecoderByType(mime)
                audioDecoder.configure(audioTrackInfo.format, null, null, 0)
                audioDecoder.start()

                while (!audioExtractorWarmedUp) {
                    advanceAudioExtractor()
                }
            }
        } catch (e: IOException) {
            release()
            throw e
        }
    }

    private fun advanceVideoExtractor() {
        // Feed more data to the decoder.
        if (!videoInputDone) {
            val inputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                val inputBuffer = videoDecoder.getInputBuffer(inputBufferIndex)
                    ?: throw RuntimeException("advanceVideoExtractor::decoderInputBuffer $inputBufferIndex was null")

                // Read the sample data into the ByteBuffer. This neither respects nor
                // updates input buffer's position, limit, etc.
                val chunkSize = videoExtractor.readSampleData(inputBuffer, 0)
                if (chunkSize < 0) {
                    // End of stream -- send empty frame with EOS flag set.
                    videoDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    videoInputDone = true

                    if (VERBOSE) Log.i(TAG, "advanceVideoExtractor::sent input EOS")
                } else {
                    val presentationTimeUs = videoExtractor.sampleTime
                    videoDecoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, presentationTimeUs, 0)

                    if (VERBOSE) Log.v(TAG, "advanceVideoExtractor::submitted chunk to decoder, size=$chunkSize")

                    videoExtractor.advance()
                }
            } else {
                if (VERBOSE) Log.i(TAG, "advanceVideoExtractor::input buffer not available")
            }
        }

        if (!videoOutputDone) {
            val outputBufferIndex = videoDecoder.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_USEC)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER ->
                    if (VERBOSE) Log.i(TAG, "advanceVideoExtractor::no output from decoder available")
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    videoExtractorWarmedUp = true
                    val newFormat = videoDecoder.outputFormat
                    if (VERBOSE) Log.i(TAG, "advanceVideoExtractor::decoder output format changed: $newFormat")
                }
                outputBufferIndex < 0 -> Log.w(TAG, "advanceVideoExtractor::" +
                        "unexpected result from decoder with status: $outputBufferIndex. Ignoring.")
                else -> {
                    if (VERBOSE) Log.v(TAG, "advanceVideoExtractor::surface decoder given buffer $outputBufferIndex " +
                            "with size=${videoBufferInfo.size}")

                    if (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "advanceVideoExtractor::output EOS")
                        videoOutputDone = true
                    }

                    val doRender = videoBufferInfo.size != 0
                    videoDecoder.releaseOutputBuffer(outputBufferIndex, doRender)
                }
            }
        }
    }

    private fun advanceAudioExtractor() {
        // Feed more data to the decoder.
        if (!audioInputDone) {
            val inputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                val inputBuffer = audioDecoder.getInputBuffer(inputBufferIndex)
                    ?: throw RuntimeException("advanceAudioExtractor::decoderInputBuffer $inputBufferIndex was null")

                // Read the sample data into the ByteBuffer. This neither respects nor
                // updates input buffer's position, limit, etc.
                val chunkSize = audioExtractor.readSampleData(inputBuffer, 0)
                if (chunkSize < 0) {
                    // End of stream -- send empty frame with EOS flag set.
                    audioDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    audioInputDone = true

                    if (VERBOSE) Log.i(TAG, "advanceAudioExtractor::sent input EOS")
                } else {
                    audioDecoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, audioExtractor.sampleTime, audioExtractor.sampleFlags)

                    if (VERBOSE) Log.v(TAG, "advanceAudioExtractor::submitted chunk to decoder, size=$chunkSize")

                    audioExtractor.advance()
                }
            } else {
                if (VERBOSE) Log.i(TAG, "advanceAudioExtractor::input buffer not available")
            }
        }

        if (!audioOutputDone) {
            val outputBufferIndex = audioDecoder.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_USEC)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER ->
                    if (VERBOSE) Log.i(TAG, "advanceAudioExtractor::no output from decoder available")
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    audioExtractorWarmedUp = true
                    val newFormat = audioDecoder.outputFormat
                    if (VERBOSE) Log.i(TAG, "advanceAudioExtractor::decoder output format changed: $newFormat")
                }
                outputBufferIndex < 0 -> Log.w(TAG, "advanceAudioExtractor::" +
                        "unexpected result from decoder with status: $outputBufferIndex. Ignoring.")
                else -> {
                    if (VERBOSE) Log.v(TAG, "advanceAudioExtractor::surface decoder given buffer $outputBufferIndex " +
                            "with size=${audioBufferInfo.size}")

                    if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "advanceAudioExtractor::output EOS")
                        audioOutputDone = true
                    }

                    val buffer: ByteBuffer? = audioDecoder.getOutputBuffer(outputBufferIndex)?.duplicate()
                    buffer?.position(audioBufferInfo.offset)
                    buffer?.limit(audioBufferInfo.offset + audioBufferInfo.size)

                    Log.i(TAG, "advanceAudioExtractor::Audio Buffer data=$buffer")
                    audioBuffer?.update(buffer, audioBufferInfo.size)
                    audioDecoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        }
    }

    private fun getTrackInfo(extractor: MediaExtractor, mimePrefix: String): TrackInfo? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(mimePrefix) == true) {
                return TrackInfo(i, format)
            }
        }

        return null
    }

    private data class TrackInfo(
            val index: Int,
            val format: MediaFormat
    )
}