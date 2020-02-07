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
        set(value) {
            field = value
            redComponent = value.red()
            greenComponent = value.green()
            blueComponent = value.blue()
        }

    var width = 0f
        set(value) {
            field = value
            // todo use this value to calculate border width coefficient
            rebuildVertices()
        }

    private val borderMarginCoefficient = 0.05f
    private val borderWidthCoefficient = 0.08f

    private var aspectCoefficient: Float = 0f
    private var scaleFactor: Float = 1f
    private var mvpLoc = -1
    private var ratioLoc = -1
    private var aPosLoc = -1
    private var uColorLoc = -1
    private var vertices = createVertices()

    private var redComponent = color.red()
    private var greenComponent = color.green()
    private var blueComponent = color.blue()


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

        glUniform4f(uColorLoc, redComponent, greenComponent, blueComponent, 1.0f)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, vertices.size / 2)

        glDisableVertexAttribArray(aPosLoc)
    }

    fun setAspect(aspect: Float) {
        aspectCoefficient = when {
            aspect > 1f -> 1f / aspect
            aspect < 1f -> -aspect
            else -> 0f
        }

        rebuildVertices()
    }

    fun setScale(scale: Float) {
        scaleFactor = scale

        rebuildVertices()
    }

    private fun rebuildVertices() {
        vertices = createVertices()

        vertexData.clear()
        vertexData.put(vertices)
        vertexData.position(0)
    }

    private fun createVertices(): FloatArray {
        val margin = (borderMarginCoefficient / scaleFactor).coerceAtMost(borderMarginCoefficient)
        val width = (borderWidthCoefficient / scaleFactor).coerceAtMost(borderWidthCoefficient)
        val marginAspectCoef = (aspectCoefficient * borderMarginCoefficient)
            .let { (it / scaleFactor).coerceAtLeast(it) }

        val widthAspectCoef = (aspectCoefficient * borderWidthCoefficient)
            .let { (it / scaleFactor).coerceAtLeast(it) }

        return floatArrayOf(
                /*v1*/   -1f - width, 1f + margin + marginAspectCoef,
                /*v2*/   -1f - width, 1f + width + widthAspectCoef,
                /*v3*/   -1f - margin, 1f + margin + marginAspectCoef,
                /*v4*/   -1f - margin, 1f + width + widthAspectCoef,
                /*v5*/    1f + margin, 1f + margin + marginAspectCoef,
                /*v6*/    1f + margin, 1f + width + widthAspectCoef,
                /*v7*/    1f + width, 1f + width + widthAspectCoef,
                /*v8*/    1f + width, 1f + margin + marginAspectCoef,
                /*v9*/    1f + margin, 1f + margin + marginAspectCoef,
                /*v10*/   1f + width, -1f - margin - marginAspectCoef,
                /*v11*/   1f + margin, -1f - margin - marginAspectCoef,
                /*v12*/   1f + width, -1f - width - widthAspectCoef,
                /*v13*/   1f + margin, -1f - width - widthAspectCoef,
                /*v14*/   1f + margin, -1f - margin - marginAspectCoef,
                /*v15*/  -1f - margin, -1f - width - widthAspectCoef,
                /*v16*/  -1f - margin, -1f - margin - marginAspectCoef,
                /*v17*/  -1f - width, -1f - width - widthAspectCoef,
                /*v18*/  -1f - width, -1f - margin - marginAspectCoef,
                /*v19*/  -1f - margin, -1f - margin - marginAspectCoef,
                /*v20*/  -1f - width, 1f + margin + marginAspectCoef,
                /*v21*/  -1f - margin, 1f + margin + marginAspectCoef
        )
    }
}