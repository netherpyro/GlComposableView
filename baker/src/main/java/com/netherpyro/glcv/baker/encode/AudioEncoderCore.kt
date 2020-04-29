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

import android.media.AudioFormat
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
        private val muxer: MediaMuxer,
        private val muxerTrackAddedCallback: MuxerTrackAddedCallback
) {
    companion object {
        private const val TAG = "AudioEncoderCore"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val SAMPLE_RATE = 44100 // 44.1[KHz] is only setting guaranteed to be available on all devices.
        private const val BIT_RATE = 64 * 1024 // 65536
        private const val CHANNEL_COUNT = 2
        private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO
        private const val TIMEOUT_USEC = 10000L

        private val VERBOSE = Baker.VERBOSE_LOGGING
    }

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
    private val bufferInfo = MediaCodec.BufferInfo()

    private var trackIndex = -1
    private var trackAdded = false

    init {
        val format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, CHANNEL_MASK)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)

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

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer data
     * @param length length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    //todo resolve buffer overflow
    fun encode(buffer: ByteBuffer?, length: Int, presentationTimeUs: Long) {
        if (VERBOSE) Log.v(TAG, "encode::buffer=$buffer, length:$length")

        val inputBufferIndex: Int = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if (inputBufferIndex > 0) {
            val inputBuffer: ByteBuffer = encoder.getInputBuffer(inputBufferIndex)
                ?: throw RuntimeException("drainEncoder::encoderInputBuffer $inputBufferIndex was null")

            inputBuffer.clear()

            if (buffer != null) {
                inputBuffer.put(buffer)
            }

            if (length > 0) {
                encoder.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0)
            } else {
                encoder.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.w(TAG, "encode::encoder is not ready. Input index=$inputBufferIndex")
            // wait for MediaCodec encoder is ready to encode
            // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
            // will wait for maximum TIMEOUT_USEC on each call
        }
    }

    fun drain() {
        if (VERBOSE) Log.v(TAG, "drainEncoder()")

        while_loop@ while (true) {
            val outputBufferIndex: Int = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (VERBOSE) Log.v(TAG, "drainEncoder::no output available")
                    break@while_loop
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
                        if (VERBOSE) Log.v(TAG, "drainEncoder::end of stream reached")

                        break@while_loop
                    }
                }
            }
        }
    }
}