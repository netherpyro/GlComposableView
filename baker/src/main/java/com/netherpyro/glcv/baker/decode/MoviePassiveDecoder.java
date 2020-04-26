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

package com.netherpyro.glcv.baker.decode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.netherpyro.glcv.baker.Baker;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author mmikhailov on 25.04.2020.
 *
 * Plays the video track from a movie file to a Surface in a passive way.
 * You should call [MoviePassiveDecoder#advance] to grab next frame.
 */
// todo resolve black frame at the beginning
class MoviePassiveDecoder extends MoviePlayer {
    private static final String TAG = "MoviePassiveDecoder";
    private static final boolean VERBOSE = Baker.Companion.getVERBOSE_LOGGING();
    private final static int TIMEOUT_USEC = 10000;

    private final Uri uri;
    private final Context context;
    private final MediaCodec.BufferInfo bufferInfo;

    private Surface outputSurface;
    private MediaExtractor extractor;
    private MediaCodec decoder;

    private int trackIndex = -1;

    private SpeedController mSpeedController;
    private ByteBuffer[] decoderInputBuffers;
    private int inputChunk = 0;
    private long firstInputTimeNsec = -1;
    private boolean outputDone = false;
    private boolean inputDone = false;
    private boolean used = false;

    /**
     * @param surface The Surface where frames will be sent.
     */
    @Override
    public void consume(@NotNull Surface surface) {
        outputSurface = surface;
    }

    /**
     * Constructs a Sync passive video decoder .
     *
     * @param uri The content-uri of video file path to open.
     */
    MoviePassiveDecoder(Context context, Uri uri) {
        this.uri = uri;
        this.context = context;
        this.bufferInfo = new MediaCodec.BufferInfo();
    }

    void raiseDecoder() throws IOException {
        if (decoder != null || extractor != null) {
            Log.w(TAG, "advance::decoder already prepared");
            return;
        }

        raiseDecoderInternal();
    }

    void release() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
    }

    boolean isUsed() {
        return used;
    }

    void advance(long ptsUsec) {
        if (decoder == null || extractor == null) {
            Log.w(TAG, "advance::decoder was released!");
            return;
        }

        if (outputDone) {
            Log.w(TAG, "advance::nothing left for playback");
            return;
        }

        if (!mSpeedController.test(ptsUsec)) {
            if (VERBOSE) Log.i(TAG, "advance::skip frame due to frame threshold");
            return;
        }

        used = true;

        advanceInternal();
    }

    private void raiseDecoderInternal() throws IOException {
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(context, uri, null);
            trackIndex = selectTrack(extractor);

            if (trackIndex < 0) {
                throw new RuntimeException("raiseDecoder::No video track found in " + uri.toString());
            }

            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);

            int fps = format.getInteger(MediaFormat.KEY_FRAME_RATE);
            Log.i(TAG, "raiseDecoder::video frame rate = " + fps);

            mSpeedController = new SpeedController(fps);

            // Create a MediaCodec decoder, and configure it with the MediaFormat from the
            // extractor. It's very important to use the format from the extractor because
            // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface, null, 0);
            decoder.start();
            decoderInputBuffers = decoder.getInputBuffers();
        } catch (IOException e) {
            release();
            throw e;
        }
    }

    private void advanceInternal() {
        if (VERBOSE) Log.d(TAG, "advance::loop");
        // Feed more data to the decoder.
        if (!inputDone) {
            int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufIndex >= 0) {
                if (firstInputTimeNsec == -1) {
                    firstInputTimeNsec = System.nanoTime();
                }

                ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                // Read the sample data into the ByteBuffer.  This neither respects nor
                // updates inputBuf's position, limit, etc.
                int chunkSize = extractor.readSampleData(inputBuf, 0);
                if (chunkSize < 0) {
                    // End of stream -- send empty frame with EOS flag set.
                    decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    inputDone = true;
                    if (VERBOSE) Log.d(TAG, "advance::sent input EOS");
                } else {
                    if (extractor.getSampleTrackIndex() != trackIndex) {
                        Log.w(TAG, "advance::WEIRD: got sample from track " +
                                extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                    }
                    long presentationTimeUs = extractor.getSampleTime();
                    decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                            presentationTimeUs, 0);
                    if (VERBOSE) {
                        Log.d(TAG, "advance::submitted frame " + inputChunk + " to dec, size=" +
                                chunkSize);
                    }
                    inputChunk++;
                    extractor.advance();
                }
            } else {
                if (VERBOSE) Log.d(TAG, "advance::input buffer not available");
            }
        }

        if (!outputDone) {
            int decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (VERBOSE) Log.d(TAG, "advance::no output from decoder available");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not important for us, since we're using Surface
                if (VERBOSE) Log.d(TAG, "advance::decoder output buffers changed");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = decoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "advance::decoder output format changed: " + newFormat);
            } else if (decoderStatus < 0) {
                throw new RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus);
            } else { // decoderStatus >= 0
                if (firstInputTimeNsec != 0) {
                    // Log the delay from the first buffer of input to the first buffer
                    // of output.
                    long nowNsec = System.nanoTime();
                    Log.d(TAG, "advance::startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                    firstInputTimeNsec = 0;
                }

                if (VERBOSE) Log.d(TAG, "advance::surface decoder given buffer " + decoderStatus +
                        " (size=" + bufferInfo.size + ")");

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "advance::output EOS");
                    outputDone = true;
                }

                boolean doRender = (bufferInfo.size != 0);
                decoder.releaseOutputBuffer(decoderStatus, doRender);
            }
        }
    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "selectTrack::extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }

        return -1;
    }
}
