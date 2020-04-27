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
import java.io.IOException
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
@Suppress("ConstantConditionIf", "PrivatePropertyName")
class VideoEncoderCore internal constructor(
        private val outputPath: String,
        private val width: Int,
        private val height: Int,
        private val fps: Int,
        private val bitRate: Int,
        private val iFrameIntervalSecs: Int
) {
    private val VERBOSE = Baker.VERBOSE_LOGGING

    companion object {
        const val DEFAULT_SIDE_MIN_SIZE = 1080
        const val DEFAULT_FPS = 30
        const val DEFAULT_I_FRAME_INTERVAL_SEC = 5
        const val DEFAULT_BIT_RATE = 4000000

        private const val TAG = "VideoEncoderCore"
        private const val MIME_TYPE = "video/avc"

    }

    /**
     * The encoder's input surface.
     */
    lateinit var inputSurface: Surface
        private set

    private lateinit var encoder: MediaCodec
    private lateinit var muxer: MediaMuxer

    // allocate one of these up front so we don't need to do it every time
    private lateinit var bufferInfo: MediaCodec.BufferInfo

    private var trackIndex = 0
    private var muxerStarted = false

    init {
        prepare()
    }

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private fun prepare() {
        bufferInfo = MediaCodec.BufferInfo()

        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSecs)

        if (VERBOSE) Log.v(
                TAG, "prepareEncoder::format=$format")

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()

        Log.d(TAG, "prepareEncoder::output file=$outputPath")

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies. These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            throw RuntimeException("prepareEncoder::MediaMuxer creation failed", e)
        }

        trackIndex = -1
        muxerStarted = false
    }

    /**
     * Releases encoder resources. May be called after partial / failed initialization.
     */
    fun release() {
        if (VERBOSE) Log.v(
                TAG, "releaseEncoder::releasing encoder objects")

        if (::encoder.isInitialized) {
            encoder.stop()
            encoder.release()
        }

        if (::inputSurface.isInitialized) {
            inputSurface.release()
        }

        if (::muxer.isInitialized) {
            muxer.stop()
            muxer.release()
        }
    }

    /**
     * Extracts all pending data from the encoder.
     *
     * If endOfStream is not set, this returns when there is no more data to drain. If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    @Suppress("DEPRECATION")
    fun drain(endOfStream: Boolean) {
        val timeoutUs = 10000L

        if (VERBOSE) Log.v(
                TAG, "drainEncoder($endOfStream)")

        if (endOfStream) {
            if (VERBOSE) Log.v(
                    TAG, "drainEncoder::sending EOS to encoder")
            encoder.signalEndOfInputStream()
        }

        var encoderOutputBuffers: Array<ByteBuffer?> = encoder.outputBuffers

        while (true) {
            val encoderStatus: Int = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                } else if (VERBOSE) Log.v(
                        TAG, "drainEncoder::no output available, spinning to await EOS")

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = encoder.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) {
                    throw RuntimeException("drainEncoder::format changed twice")
                }

                val newFormat: MediaFormat = encoder.outputFormat

                Log.d(TAG, "drainEncoder::encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
            } else if (encoderStatus < 0) {
                Log.w(TAG, "drainEncoder::unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                // let's ignore it
            } else {
                val encodedData: ByteBuffer = encoderOutputBuffers[encoderStatus]
                    ?: throw RuntimeException("drainEncoder::encoderOutputBuffer $encoderStatus was null")

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                    if (VERBOSE) Log.v(
                            TAG, "drainEncoder::ignoring BUFFER_FLAG_CODEC_CONFIG")

                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw RuntimeException("drainEncoder::muxer hasn't started")
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)

                    if (VERBOSE) Log.v(
                            TAG, "drainEncoder::sent ${bufferInfo.size} bytes to muxer, " +
                            "ts=${bufferInfo.presentationTimeUs}")
                }

                encoder.releaseOutputBuffer(encoderStatus, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "drainEncoder::reached end of stream unexpectedly")
                    } else if (VERBOSE) Log.v(
                            TAG, "drainEncoder::end of stream reached")

                    break // out of while
                }
            }
        }
    }
}