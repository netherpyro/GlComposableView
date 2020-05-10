/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.netherpyro.glcv.baker.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import com.netherpyro.glcv.baker.Baker
import java.nio.ByteBuffer

/**
 * @author mmikhailov on 29.03.2020.
 *
 * This class wraps up the core components used for surface-input video encoding.
 *
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 *
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
@Suppress("ConstantConditionIf")
internal class VideoEncoderCore internal constructor(
        width: Int,
        height: Int,
        fps: Int,
        bitRate: Int,
        iFrameIntervalSecs: Int,
        private val muxer: MediaMuxer,
        private val muxerTrackAddedCallback: MuxerTrackAddedCallback
) {
    companion object {
        private const val TAG = "VideoEncoderCore"
        private const val MIME_TYPE = "video/avc"
        private const val TIMEOUT_USEC = 10000L

        private val VERBOSE = Baker.VERBOSE_LOGGING

        const val DEFAULT_SIDE_MIN_SIZE = 1080
        const val DEFAULT_FPS = 30
        const val DEFAULT_I_FRAME_INTERVAL_SEC = 2
        const val DEFAULT_BIT_RATE = 921600
    }

    /**
     * The encoder's input surface.
     */
    val inputSurface: Surface

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
    private val bufferInfo = MediaCodec.BufferInfo()

    private var trackIndex = -1
    private var trackAdded = false

    init {
        // Configure encoder and prepares the input Surface

        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
        // Set some properties. Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSecs)

        if (VERBOSE) Log.v(TAG, "prepareEncoder::format=$format")

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()
    }

    /**
     * Releases encoder resources. May be called after partial / failed initialization.
     */
    fun release() {
        if (VERBOSE) Log.v(TAG, "releaseEncoder::releasing encoder objects")

        encoder.stop()
        encoder.release()
        inputSurface.release()
    }

    /**
     * Extracts all pending data from the encoder.
     *
     * If endOfStream is not set, this returns when there is no more data to drain. If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    fun drain(endOfStream: Boolean) {
        if (VERBOSE) Log.v(TAG, "drainEncoder($endOfStream)")

        if (endOfStream) {
            if (VERBOSE) Log.v(TAG, "drainEncoder::sending EOS to encoder")
            encoder.signalEndOfInputStream()
        }

        while_loop@ while (true) {
            val outputBufferIndex: Int = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (VERBOSE) Log.v(TAG, "drainEncoder::no output available")
                    if (!endOfStream) {
                        break@while_loop
                    }
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // should happen before receiving buffers, and should only happen once
                    if (trackAdded) {
                        throw RuntimeException("drainEncoder::format changed twice")
                    }

                    val newFormat: MediaFormat = encoder.outputFormat
                    Log.d(TAG, "drainEncoder::encoder output format changed: $newFormat")

                    trackIndex = muxer.addTrack(newFormat)
                    trackAdded = true
                    muxerTrackAddedCallback.onTrackAdded()
                    break@while_loop
                }
                outputBufferIndex < 0 -> Log.w(TAG, "drainEncoder::" +
                        "unexpected result from encoder with status: $outputBufferIndex. Ignoring.")
                else -> {
                    val encodedData: ByteBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        ?: throw RuntimeException("drainEncoder::encoderOutputBuffer $outputBufferIndex was null")

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                        if (VERBOSE) Log.v(TAG, "drainEncoder::ignoring BUFFER_FLAG_CODEC_CONFIG")

                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        if (!trackAdded) {
                            throw RuntimeException("drainEncoder::track wasn't added to muxer")
                        }

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)

                        if (VERBOSE) Log.v(TAG, "drainEncoder::sent ${bufferInfo.size} bytes to muxer, " +
                                "ts=${bufferInfo.presentationTimeUs}")
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "drainEncoder::reached end of stream unexpectedly")
                        } else if (VERBOSE) Log.v(TAG, "drainEncoder::end of stream reached")

                        break@while_loop
                    }
                }
            }
        }
    }
}