package com.netherpyro.glcv.util

import android.graphics.Bitmap
import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_COMPILE_STATUS
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_NO_ERROR
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.GL_TRUE
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glDeleteProgram
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glGenBuffers
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glGetError
import android.opengl.GLES20.glGetProgramiv
import android.opengl.GLES20.glGetShaderInfoLog
import android.opengl.GLES20.glGetShaderiv
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLException
import android.opengl.GLUtils
import android.util.Log
import com.netherpyro.glcv.BuildConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @author mmikhailov on 2019-11-30.
 */
object EglUtil {

    const val NO_TEXTURE = -1
    private const val FLOAT_SIZE_BYTES = 4

    fun loadShader(strSource: String?, iType: Int): Int {
        val compiled = IntArray(1)
        val iShader = glCreateShader(iType)

        glShaderSource(iShader, strSource)
        glCompileShader(iShader)
        glGetShaderiv(iShader, GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {
            Log.e("EglUtil", "Load shader failed compilation\n${glGetShaderInfoLog(iShader)}")
            return 0
        }

        return iShader
    }

    @Throws(GLException::class)
    fun createProgram(vertexShader: Int, pixelShader: Int): Int {

        val program = glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Could not create program")
        }

        glAttachShader(program, vertexShader)
        glAttachShader(program, pixelShader)
        glLinkProgram(program)

        val linkStatus = IntArray(1)

        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] != GL_TRUE) {
            glDeleteProgram(program)
            throw RuntimeException("Could not link program")
        }

        return program
    }

    fun checkEglError(operation: String) {
        if (!BuildConfig.DEBUG) return
        var error: Int

        while (glGetError().also { error = it } != GL_NO_ERROR) {
            throw RuntimeException("$operation: glError $error")
        }
    }

    fun setupSampler(target: Int, mag: Int, min: Int) {
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, mag)
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, min)
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    fun createBuffer(data: FloatArray): Int {
        return createBuffer(toFloatBuffer(data))
    }

    fun createBuffer(data: FloatBuffer): Int {
        val buffers = IntArray(1)
        glGenBuffers(buffers.size, buffers, 0)
        updateBufferData(buffers[0], data)

        return buffers[0]
    }

    fun toFloatBuffer(data: FloatArray): FloatBuffer {
        val buffer = ByteBuffer
            .allocateDirect(data.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(data)
            .position(0)

        return buffer
    }

    fun updateBufferData(bufferName: Int, data: FloatBuffer) {
        glBindBuffer(GL_ARRAY_BUFFER, bufferName)
        glBufferData(GL_ARRAY_BUFFER, data.capacity() * FLOAT_SIZE_BYTES, data, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun loadTexture(img: Bitmap, usedTexId: Int, recycle: Boolean): Int {
        val textures = IntArray(1)
        if (usedTexId == NO_TEXTURE) {
            glGenTextures(1, textures, 0)
            glBindTexture(GL_TEXTURE_2D, textures[0])
            setupSampler(GL_TEXTURE_2D, GL_LINEAR, GL_LINEAR)
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, img, 0)
        } else {
            glBindTexture(GL_TEXTURE_2D, usedTexId)
            GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, img)
            textures[0] = usedTexId
        }

        if (recycle) {
            img.recycle()
        }

        return textures[0]
    }

    fun genBlankTexture(target: Int): Int {
        val args = IntArray(1)

        glGenTextures(args.size, args, 0)
        glBindTexture(target, args[0])
        setupSampler(target, GL_LINEAR, GL_NEAREST)
        glBindTexture(GL_TEXTURE_2D, 0)

        return args[0]
    }

    fun deleteTextures(vararg texIds: Int) {
        glDeleteTextures(texIds.size, texIds, 0)
    }
}