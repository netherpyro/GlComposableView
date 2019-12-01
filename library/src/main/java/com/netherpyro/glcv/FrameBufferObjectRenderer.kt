package com.netherpyro.glcv

import android.opengl.GLES20
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import com.netherpyro.glcv.shader.GlShader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author mmikhailov on 2019-11-30.
 */
internal abstract class FrameBufferObjectRenderer : GLSurfaceView.Renderer {

    private lateinit var mFramebufferObject: FramebufferObject
    private lateinit var normalShader: GlShader

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        mFramebufferObject = FramebufferObject()
        normalShader = GlShader()
        normalShader.setup()

        onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mFramebufferObject.setup(width, height)

        onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        mFramebufferObject.enable()

        glViewport(0, 0, mFramebufferObject.width, mFramebufferObject.height)

        onDrawFrame(mFramebufferObject)

        glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        glViewport(0, 0, mFramebufferObject.width, mFramebufferObject.height)
        glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        normalShader.draw(mFramebufferObject.texName, null)
    }

    @Throws(Throwable::class)
    protected fun finalize() {
    }

    abstract fun onSurfaceCreated()
    abstract fun onSurfaceChanged(width: Int, height: Int)
    abstract fun onDrawFrame(fbo: FramebufferObject)
}