package com.netherpyro.glcv.shader

import android.graphics.Bitmap
import android.opengl.GLES20
import com.netherpyro.glcv.util.EglUtil

/**
 * @author mmikhailov on 2019-12-04.
 */
internal class GlImageShader(private val bitmap: Bitmap) : GlShader(VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER) {

    companion object {
        private const val VERTEX_SHADER = "" +
                "uniform mat4 uMVPMatrix;\n" +
                "uniform float uCRatio;\n" +

                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying highp vec2 vTextureCoord;\n" +

                "void main() {\n" +
                    "vec4 scaledPos = aPosition;\n" +
                    "scaledPos.x = scaledPos.x * uCRatio;\n" +
                    "gl_Position = uMVPMatrix * scaledPos;\n" +
                    "vTextureCoord = vec2(aTextureCoord.x, 1.0 - aTextureCoord.y);\n" +
                "}\n"
    }

    private var texName = EglUtil.NO_TEXTURE

    override fun setup() {
        super.setup()

        if (!bitmap.isRecycled) {
            texName = EglUtil.loadTexture(bitmap, texName, false)
        } else throw IllegalArgumentException("Provided bitmap is recycled!")
    }

    override fun release() {
        super.release()

        if (texName != EglUtil.NO_TEXTURE) {
            EglUtil.deleteTextures(texName)
            texName = EglUtil.NO_TEXTURE
        }
    }

    fun draw(mvpMatrix: FloatArray?, aspectRatio: Float) {
        useProgram()

        GLES20.glUniformMatrix4fv(getHandle("uMVPMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(getHandle("uCRatio"), aspectRatio)
        GLES20.glUniform1f(getHandle(UNIFORM_OPACITY), opacity)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferName)
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"))
        GLES20.glVertexAttribPointer(getHandle("aPosition"), VERTICES_DATA_POS_SIZE, GLES20.GL_FLOAT,
                false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET)
        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"))
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), VERTICES_DATA_UV_SIZE,
                GLES20.GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texName)
        GLES20.glUniform1i(getHandle(DEFAULT_UNIFORM_SAMPLER), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(getHandle("aPosition"))
        GLES20.glDisableVertexAttribArray(getHandle("aTextureCoord"))
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }


}