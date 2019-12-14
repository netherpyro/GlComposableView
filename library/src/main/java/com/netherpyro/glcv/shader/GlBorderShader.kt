package com.netherpyro.glcv.shader

import android.graphics.Color
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import android.opengl.GLES20.glDisableVertexAttribArray
import android.opengl.GLES20.glDrawArrays
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glUniform1f
import android.opengl.GLES20.glUniform4f
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES20.glVertexAttribPointer
import androidx.annotation.ColorInt
import com.netherpyro.glcv.extensions.blue
import com.netherpyro.glcv.extensions.green
import com.netherpyro.glcv.extensions.red
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author mmikhailov on 2019-12-14.
 */
internal class GlBorderShader : GlShader(VERTEX_SHADER, FRAGMENT_SHADER) {

    companion object {
        private const val VERTEX_SHADER = "" +
                "uniform mat4 uMVPMatrix;\n" +
                "uniform float uCRatio;\n" +

                "attribute vec4 aPosition;\n" +

                "void main() {\n" +
                "   vec4 scaledPos = aPosition;\n" +
                "   scaledPos.x = scaledPos.x * uCRatio;\n" +
                "   gl_Position = uMVPMatrix * scaledPos;\n" +
                "}\n"

        private const val FRAGMENT_SHADER = "" +
                "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +

                "void main() {\n" +
                "   gl_FragColor = uColor;\n" +
                "}\n"
    }

    @ColorInt
    var color: Int = Color.BLUE
    var width = 0f
        set(value) {
            field = value
            // todo recalculate 'vertices' values
        }

    private var mvpLoc = -1
    private var ratioLoc = -1
    private var aPosLoc = -1
    private var uColorLoc = -1
    private var vertices = floatArrayOf(
            -1.1f, 1.05f,
            -1.1f, 1.1f,
            -1.05f, 1.05f,
            -1.05f, 1.1f,
            1.05f, 1.05f,
            1.05f, 1.1f,
            1.1f, 1.1f,
            1.1f, 1.05f,
            1.05f, 1.05f,
            1.1f, -1.05f,
            1.05f, -1.05f,
            1.1f, -1.1f,
            1.05f, -1.1f,
            1.05f, -1.05f,
            -1.05f, -1.1f,
            -1.05f, -1.05f,
            -1.1f, -1.1f,
            -1.1f, -1.05f,
            -1.05f, -1.05f,
            -1.1f, 1.05f,
            -1.05f, 1.05f
    )

    private var vertexData = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    override fun setup() {
        super.setup()

        vertexData.clear()
        vertexData.put(vertices)
        vertexData.position(0)

        mvpLoc = getHandle("uMVPMatrix")
        ratioLoc = getHandle("uCRatio")
        aPosLoc = getHandle("aPosition")
        uColorLoc = getHandle("uColor")
    }

    fun draw(mvpMatrix: FloatArray?, aspectRatio: Float) {
        useProgram()

        glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        glUniform1f(ratioLoc, aspectRatio)

        glEnableVertexAttribArray(aPosLoc)
        glVertexAttribPointer(aPosLoc, 2, GL_FLOAT, false, 0, vertexData)

        glUniform4f(uColorLoc, color.red(), color.green(), color.blue(), 1.0f)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 21)

        glDisableVertexAttribArray(aPosLoc)
    }
}