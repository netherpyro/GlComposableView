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
import com.netherpyro.glcv.baker.Baker
import java.nio.ByteBuffer

/**
 * @author mmikhailov on 27.04.2020.
 */
@Suppress("ConstantConditionIf")
internal class AudioEncoderCore internal constructor(
        sampleRate: Int,
        channelCount: Int,
        mimeType: String,
        private val muxer: MediaMuxer,
        private val muxerTrackAddedCallback: MuxerTrackAddedCallback
) {
    companion object {
        private const val TAG = "AudioEncoderCore"
       // private const val MIME_TYPE = "audio/mp4a-latm"
        private const val TIMEOUT_USEC = 10000L

        private val VERBOSE = Baker.VERBOSE_LOGGING
    }

    private val encoder = MediaCodec.createEncoderByType(mimeType)
    private val bufferInfo = MediaCodec.BufferInfo()

    private var trackIndex = -1
    private var trackAdded = false

    init {
        val format = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, 16)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024)

        if (VERBOSE) Log.v(TAG, "prepareEncoder::format=$format")

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
    }

    /**
     * Releases encoder resources. May be called after partial / failed initialization.
     */
    fun release() {
        if (VERBOSE) Log.v(TAG, "releaseEncoder::releasing encoder objects")

        encoder.stop()
        encoder.release()
    }

    fun encode(buffer: ByteBuffer, length: Int, presentationTimeUs: Long) {
        if (VERBOSE) Log.v(TAG, "encode::buffer=$buffer length:$length")

        val inputIndex: Int = encoder.dequeueInputBuffer(TIMEOUT_USEC)

        if (VERBOSE) Log.v(TAG, "encode::inputIndex=$inputIndex")

        if (inputIndex > 0) {
            val inputBuffer = encoder.getInputBuffer(inputIndex)!!

            if (VERBOSE) Log.v(TAG, "encode::inputBuffer=$inputBuffer")

//            inputBuffer.clear()
            inputBuffer.put(buffer)

            if (length <= 0) {
                encoder.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                encoder.queueInputBuffer(inputIndex, 0, length, presentationTimeUs, 0)
            }
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
        if (VERBOSE) Log.v(TAG, "drainEncoder($endOfStream)")

        if (endOfStream) {
            if (VERBOSE) Log.v(TAG, "drainEncoder::sending EOS to encoder")
            encoder.signalEndOfInputStream()
        }

        var encoderOutputBuffers: Array<ByteBuffer?> = encoder.outputBuffers

        while (true) {
            val encoderStatus: Int = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                } else if (VERBOSE) Log.v(TAG, "drainEncoder::no output available, spinning to await EOS")

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = encoder.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (trackAdded) {
                    throw RuntimeException("drainEncoder::format changed twice")
                }

                val newFormat: MediaFormat = encoder.outputFormat

                Log.d(TAG, "drainEncoder::encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, add track to muxer
                trackIndex = muxer.addTrack(newFormat)
                trackAdded = true
                muxerTrackAddedCallback.onTrackAdded()
            } else if (encoderStatus < 0) {
                Log.w(TAG, "drainEncoder::unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                // let's ignore it
            } else {
                val encodedData: ByteBuffer = encoderOutputBuffers[encoderStatus]
                    ?: throw RuntimeException("drainEncoder::encoderOutputBuffer $encoderStatus was null")

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

                encoder.releaseOutputBuffer(encoderStatus, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "drainEncoder::reached end of stream unexpectedly")
                    } else if (VERBOSE) Log.v(TAG, "drainEncoder::end of stream reached")

                    break // out of while
                }
            }
        }
    }
}