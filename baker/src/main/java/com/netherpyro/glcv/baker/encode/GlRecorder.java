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
public class GlRecorder implements Runnable {
    private static final String TAG = "GlRecoder";
    private static final boolean VERBOSE = Baker.Companion.getVERBOSE_LOGGING();

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    private final GlRenderer glRenderer;
    private final GlViewport glViewport;
    private final EncoderConfig config;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private final Object mReadyFence = new Object(); // guards ready/running
    private boolean mReady;
    private boolean mRunning;

    private final PostRenderCallback mPostRenderCallback;
    private final PrepareCallback mPrepareCallback;

    public GlRecorder(GlRenderer glRenderer, GlViewport viewport, EncoderConfig config, PostRenderCallback postRenderCallback, PrepareCallback prepareCallback) {
        this.glRenderer = glRenderer;
        this.glViewport = viewport;
        this.config = config;
        this.mPostRenderCallback = postRenderCallback;
        this.mPrepareCallback = prepareCallback;
    }

    /**
     * Tells the video recorder to start recording. (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages. The
     * encoder may not yet be fully configured.
     */
    public void raiseEncoder() {
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

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
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

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
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
        // Establish a Looper for this thread, and define a Handler for it.
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

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<GlRecorder> mWeakEncoder;

        EncoderHandler(GlRecorder encoder) {
            mWeakEncoder = new WeakReference<>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            GlRecorder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
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
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        prepareEncoder(config);
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

        mVideoEncoder.drain(false);

        glRenderer.onDrawFrame(null);

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();

        mPostRenderCallback.onPostRender();
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
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
        //mFullScreen.release(false);
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

    private void prepareEncoder(EncoderConfig config) {
        mVideoEncoder = new VideoEncoderCore(config.getOutputPath(), config.getWidth(), config.getHeight(), config.getFps(), config.getBitRate(), config.getIFrameIntervalSecs());
        mEglCore = new EglCore(config.getEglContext(), EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        glRenderer.onSurfaceCreated(null, null);
        glRenderer.onSurfaceChanged(null, config.getWidth(), config.getHeight());
        glRenderer.setViewport(glViewport);

        mPrepareCallback.onPrepared();
    }

    private void releaseEncoder() {
        mVideoEncoder.release();

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
