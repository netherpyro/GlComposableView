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

package com.netherpyro.glcv.baker.encode;

import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.netherpyro.glcv.GlRenderer;
import com.netherpyro.glcv.GlViewport;
import com.netherpyro.glcv.baker.Baker;
import com.netherpyro.glcv.baker.gles.EglCore;
import com.netherpyro.glcv.baker.gles.WindowSurface;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * – create TextureMovieEncoder object
 * – create an EncoderConfig
 * – call TextureMovieEncoder#startRecording() with the config
 * – call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * – for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 */
public class Encoder implements Runnable {
    private static final String TAG = "Encoder";
    private static final boolean VERBOSE = Baker.Companion.getVERBOSE_LOGGING();

    private static final int MSG_PREPARE = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    private final GlRenderer glRenderer;
    private final GlViewport glViewport;
    private final EncoderConfig config;
    private final int encodedTrackCount; // mandatory video track and optional audio track

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private VideoEncoderCore mVideoEncoder;
    private AudioEncoderCore mAudioEncoder;
    private MediaMuxer muxer;
    private AudioProcessor audioProcessor;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private final Object mReadyFence = new Object(); // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private int addedTrackCount = 0;
    private long lastPts = 0;

    private final PostRenderCallback mPostRenderCallback;
    private final PrepareCallback mPrepareCallback;

    public Encoder(
            GlRenderer glRenderer,
            GlViewport viewport,
            boolean withAudio,
            EncoderConfig config,
            PostRenderCallback postRenderCallback,
            PrepareCallback prepareCallback
    ) {
        this.glRenderer = glRenderer;
        this.glViewport = viewport;
        this.config = config;
        this.mPostRenderCallback = postRenderCallback;
        this.mPrepareCallback = prepareCallback;
        this.encodedTrackCount = withAudio ? 2 : 1;

        if (withAudio) {
            this.audioProcessor = new AudioProcessor();
        }
    }

    /**
     * Tells the video recorder to start recording. (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages. The
     * encoder may not yet be fully configured.
     */
    public void prepare() {
        Log.d(TAG, "raiseEncoder::startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "raiseEncoder::encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, TAG).start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_PREPARE, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stop() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void frameAvailable(long frameTimestampNanoSec) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        if (frameTimestampNanoSec == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(
                MSG_FRAME_AVAILABLE,
                (int) (frameTimestampNanoSec >> 32),
                (int) frameTimestampNanoSec
        ));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }

        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");

        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    private void trackAdded() {
        Log.d(TAG, "trackAdded::");
        if (++addedTrackCount == encodedTrackCount) {
            muxer.start();
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<Encoder> mWeakEncoder;

        EncoderHandler(Encoder encoder) {
            mWeakEncoder = new WeakReference<>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            Encoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage::encoder is null");
                return;
            }

            switch (what) {
                case MSG_PREPARE:
                    encoder.handlePrepare((EncoderConfig) obj);
                    break;
                case MSG_STOP:
                    encoder.handleStop();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) | (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable(timestamp);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    //noinspection ConstantConditions
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface.
     *
     * @param timestampNanos The frame's timestamp.
     */
    private void handleFrameAvailable(long timestampNanos) {
        if (VERBOSE) Log.v(TAG, "handleFrameAvailable::ts=" + timestampNanos);
        lastPts = timestampNanos;

        // create audio encoder input
        if (mAudioEncoder != null) {
            if (addedTrackCount > 0) {
                mAudioEncoder.drain();
            }

            AudioProcessor.EncoderInput encoderInput = audioProcessor.processData();
            if (encoderInput != null) {
                mAudioEncoder.encode(
                        encoderInput.getByteBuffer(),
                        encoderInput.getSize(),
                        timestampNanos / 1_000L
                );
            }
        }

        // create video encoder input
        mVideoEncoder.drain(false);

        glRenderer.onDrawFrame(null);

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();

        mPostRenderCallback.onPostRender();
    }

    private void handleStop() {
        Log.d(TAG, "handleStopRecording");
        if (mAudioEncoder != null) {
            mAudioEncoder.encode(null, 0, lastPts);
            mAudioEncoder.drain();
        }

        mVideoEncoder.drain(true);

        releaseEncoder();
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        glRenderer.release();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        glRenderer.onSurfaceCreated(null, null);
        glRenderer.onSurfaceChanged(null, config.getWidth(), config.getHeight());
        glRenderer.setViewport(glViewport); // todo not needed ?
    }

    private void handlePrepare(EncoderConfig config) {
        // Create a MediaMuxer. We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies. These can only be
        // obtained from the encoder after it has started processing data.
        try {
            muxer = new MediaMuxer(config.getTempOutputPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new RuntimeException("handlePrepare::MediaMuxer creation failed", e);
        }

        if (encodedTrackCount > 1) {
            mAudioEncoder = new AudioEncoderCore(muxer, this::trackAdded);
        }

        mVideoEncoder = new VideoEncoderCore(
                config.getWidth(),
                config.getHeight(),
                config.getFps(),
                config.getBitRate(),
                config.getIFrameIntervalSecs(),
                muxer,
                this::trackAdded
        );

        mEglCore = new EglCore(config.getEglContext(), EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        glRenderer.onSurfaceCreated(null, null);
        glRenderer.onSurfaceChanged(null, config.getWidth(), config.getHeight());
        glRenderer.setViewport(glViewport);

        mPrepareCallback.onPrepared(audioProcessor != null ? audioProcessor : new AudioBufferProviderStub());
    }

    private void releaseEncoder() {
        mVideoEncoder.release();

        if (mAudioEncoder != null) {
            mAudioEncoder.release();
        }

        if (muxer != null) {
            muxer.stop();
            muxer.release();
        }

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (glRenderer != null) {
            glRenderer.release();
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}
