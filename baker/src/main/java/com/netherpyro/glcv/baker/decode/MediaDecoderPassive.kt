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
import java.io.IOException

/**
 * @author mmikhailov on 25.04.2020.
 *
 * Plays the video track from a movie file to a Surface in a passive way.
 * You should call [MediaDecoderPassive#advance] to grab next frame.
 */
internal class MediaDecoderPassive(
        private val context: Context,
        private val uri: Uri
) : SurfaceConsumer {

    companion object {
        private const val TAG = "MediaDecoderPassive"
        private const val TIMEOUT_USEC = 10000L

        private val VERBOSE = VERBOSE_LOGGING
    }

    private val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private lateinit var extractor: MediaExtractor
    private lateinit var decoder: MediaCodec
    private lateinit var mSpeedController: SpeedController

    private var outputSurface: Surface? = null

    private var trackIndex = -1
    private var inputChunk = 0
    private var outputDone = false
    private var inputDone = false
    private var warmedUp = false
    private var released = false

    var isUsed = false
        private set

    /**
     * @param surface The Surface where frames will be sent.
     */
    override fun consume(surface: Surface) {
        outputSurface = surface
    }

    @Throws(IOException::class)
    fun prepare() {
        if (::decoder.isInitialized || ::extractor.isInitialized) {
            Log.w(TAG, "advance::decoder already prepared")
            return
        }

        prepareInternal()
    }

    fun release() {
        released = true
        decoder.stop()
        decoder.release()
        extractor.release()
    }

    fun advance(ptsUsec: Long) {
        if (released) {
            Log.w(TAG, "advance::decoder was released!")
            return
        }

        if (outputDone) {
            Log.w(TAG, "advance::nothing left for playback")
            return
        }

        if (!mSpeedController.test(ptsUsec)) {
            if (VERBOSE) Log.i(TAG, "advance::skip frame due to frame threshold")
            return
        }

        isUsed = true
        advanceInternal()
    }

    @Throws(IOException::class)
    private fun prepareInternal() {
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            trackIndex = getTrackIndex(extractor, "video/")

            if (trackIndex < 0) {
                throw RuntimeException("raiseDecoder::No video track found in $uri")
            }

            extractor.selectTrack(trackIndex)

            val format = extractor.getTrackFormat(trackIndex)
            val fps = format.getInteger(MediaFormat.KEY_FRAME_RATE)

            Log.d(TAG, "raiseDecoder::video frame rate = $fps")

            mSpeedController = SpeedController(fps)

            // Create a MediaCodec decoder, and configure it with the MediaFormat from the
            // extractor. It's very important to use the format from the extractor because
            // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
            val mime = format.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, outputSurface, null, 0)
            decoder.start()

            while (!warmedUp) {
                advanceInternal()
            }

        } catch (e: IOException) {
            release()
            throw e
        }
    }

    private fun advanceInternal() {
        // Feed more data to the decoder.
        if (!inputDone) {
            val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    ?: throw RuntimeException("advanceInternal::decoderInputBuffer $inputBufferIndex was null")

                // Read the sample data into the ByteBuffer. This neither respects nor
                // updates input buffer's position, limit, etc.
                val chunkSize = extractor.readSampleData(inputBuffer, 0)
                if (chunkSize < 0) {
                    // End of stream -- send empty frame with EOS flag set.
                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true

                    if (VERBOSE) Log.v(TAG, "advanceInternal::sent input EOS")
                } else {
                    if (extractor.sampleTrackIndex != trackIndex) {
                        Log.w(TAG, "advanceInternal::WEIRD: got sample from track " +
                                extractor.sampleTrackIndex + ", expected " + trackIndex)
                    }

                    val presentationTimeUs = extractor.sampleTime
                    decoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, presentationTimeUs, 0)

                    if (VERBOSE) Log.v(TAG, "advanceInternal::submitted frame $inputChunk to dec, size=$chunkSize")

                    inputChunk++
                    extractor.advance()
                }
            } else {
                if (VERBOSE) Log.v(TAG, "advanceInternal::input buffer not available")
            }
        }

        if (!outputDone) {
            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER ->
                    if (VERBOSE) Log.v(TAG, "advanceInternal::no output from decoder available")
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    warmedUp = true
                    val newFormat = decoder.outputFormat
                    if (VERBOSE) Log.v(TAG, "advanceInternal::decoder output format changed: $newFormat")
                }
                outputBufferIndex < 0 -> Log.w(TAG,
                        "advanceInternal::unexpected result from decoder with status: $outputBufferIndex. Ignoring.")
                else -> {
                    if (VERBOSE) Log.v(TAG,
                            "advanceInternal::surface decoder given buffer $outputBufferIndex (size=${bufferInfo.size})")

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "advanceInternal::output EOS")
                        outputDone = true
                    }

                    val doRender = bufferInfo.size != 0
                    decoder.releaseOutputBuffer(outputBufferIndex, doRender)
                }
            }
        }
    }

    private fun getTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i)
                .getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(mimePrefix) == true) {
                return i
            }
        }

        return -1
    }
}