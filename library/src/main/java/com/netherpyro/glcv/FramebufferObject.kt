package com.netherpyro.glcv

import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_DEPTH_ATTACHMENT
import android.opengl.GLES20.GL_DEPTH_COMPONENT16
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_FRAMEBUFFER_BINDING
import android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_MAX_RENDERBUFFER_SIZE
import android.opengl.GLES20.GL_MAX_TEXTURE_SIZE
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_RENDERBUFFER
import android.opengl.GLES20.GL_RENDERBUFFER_BINDING
import android.opengl.GLES20.GL_RGBA
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_BINDING_2D
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glBindRenderbuffer
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glCheckFramebufferStatus
import android.opengl.GLES20.glDeleteFramebuffers
import android.opengl.GLES20.glDeleteRenderbuffers
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glFramebufferRenderbuffer
import android.opengl.GLES20.glFramebufferTexture2D
import android.opengl.GLES20.glGenFramebuffers
import android.opengl.GLES20.glGenRenderbuffers
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glGetIntegerv
import android.opengl.GLES20.glRenderbufferStorage
import android.opengl.GLES20.glTexImage2D
import com.netherpyro.glcv.util.EglUtil

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class FramebufferObject {

    var width = 0
        private set
    var height = 0
        private set

    var texName = 0
        private set

    private var frameBufferName = 0

    private var renderBufferName = 0

    fun setup(width: Int, height: Int) {
        val args = IntArray(1)

        glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0)

        require(!(width > args[0] || height > args[0])) { "GL_MAX_TEXTURE_SIZE " + args[0] }

        glGetIntegerv(GL_MAX_RENDERBUFFER_SIZE, args, 0)

        require(!(width > args[0] || height > args[0])) { "GL_MAX_RENDERBUFFER_SIZE " + args[0] }

        glGetIntegerv(GL_FRAMEBUFFER_BINDING, args, 0)
        val saveFramebuffer = args[0]
        glGetIntegerv(GL_RENDERBUFFER_BINDING, args, 0)
        val saveRenderbuffer = args[0]
        glGetIntegerv(GL_TEXTURE_BINDING_2D, args, 0)
        val saveTexName = args[0]

        release()

        try {
            this.width = width
            this.height = height

            glGenFramebuffers(args.size, args, 0)
            frameBufferName = args[0]
            glBindFramebuffer(GL_FRAMEBUFFER, frameBufferName)

            glGenRenderbuffers(args.size, args, 0)
            renderBufferName = args[0]
            glBindRenderbuffer(GL_RENDERBUFFER, renderBufferName)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBufferName)

            glGenTextures(args.size, args, 0)
            texName = args[0]
            glBindTexture(GL_TEXTURE_2D, texName)

            EglUtil.setupSampler(GL_TEXTURE_2D, GL_LINEAR, GL_NEAREST)

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texName, 0)

            val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Failed to initialize framebuffer object $status")
            }

        } catch (e: RuntimeException) {
            release()
            throw e
        }

        glBindFramebuffer(GL_FRAMEBUFFER, saveFramebuffer)
        glBindRenderbuffer(GL_RENDERBUFFER, saveRenderbuffer)
        glBindTexture(GL_TEXTURE_2D, saveTexName)
    }

    fun release() {
        val args = IntArray(1)

        args[0] = texName
        glDeleteTextures(args.size, args, 0)
        texName = 0

        args[0] = renderBufferName
        glDeleteRenderbuffers(args.size, args, 0)
        renderBufferName = 0

        args[0] = frameBufferName
        glDeleteFramebuffers(args.size, args, 0)
        frameBufferName = 0
    }

    fun enable() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferName)
    }
}