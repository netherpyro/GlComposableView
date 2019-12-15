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

    private val borderMarginCoefficient = 0.05f
    private val borderWidthCoefficient = 0.05f

    private var aspectCoefficient: Float = 0f
    private var mvpLoc = -1
    private var ratioLoc = -1
    private var aPosLoc = -1
    private var uColorLoc = -1
    private var vertices = createVertices()

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
        glDrawArrays(GL_TRIANGLE_STRIP, 0, vertices.size / 2)

        glDisableVertexAttribArray(aPosLoc)
    }

    fun setAspect(aspect: Float) {
        aspectCoefficient = when {
            aspect > 1f -> (1f / aspect) * borderMarginCoefficient
            aspect < 1f -> -aspect * borderMarginCoefficient
            else -> 0f
        }

        vertices = createVertices()
        vertexData.clear()
        vertexData.put(vertices)
        vertexData.position(0)
    }

    private fun createVertices(): FloatArray {
        return floatArrayOf(
                /*v1*/   -1.1f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v2*/   -1.1f, 1.1f + (2 * aspectCoefficient),
                /*v3*/   -1.05f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v4*/   -1.05f, 1.1f + (2 * aspectCoefficient),
                /*v5*/   1.05f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v6*/   1.05f, 1.1f + (2 * aspectCoefficient),
                /*v7*/   1.1f, 1.1f + (2 * aspectCoefficient),
                /*v8*/   1.1f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v9*/   1.05f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v10*/   1.1f, -1.05f - aspectCoefficient,
                /*v11*/   1.05f, -1.05f - aspectCoefficient,
                /*v12*/   1.1f, -1.1f - (2 * aspectCoefficient),
                /*v13*/   1.05f, -1.1f - (2 * aspectCoefficient),
                /*v14*/   1.05f, -1.05f - aspectCoefficient,
                /*v15*/   -1.05f, -1.1f - (2 * aspectCoefficient),
                /*v16*/   -1.05f, -1.05f - aspectCoefficient,
                /*v17*/   -1.1f, -1.1f - (2 * aspectCoefficient),
                /*v18*/   -1.1f, -1.05f - aspectCoefficient,
                /*v19*/   -1.05f, -1.05f - aspectCoefficient,
                /*v20*/   -1.1f, 1.0f + borderMarginCoefficient + aspectCoefficient,
                /*v21*/   -1.05f, 1.0f + borderMarginCoefficient + aspectCoefficient
        )
    }
}