package com.mineradio.player.render

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL ES 2.0 工具集 —— 编译/链接/属性绑定。
 * 引擎内部使用，无业务依赖。
 */
object GlUtils {
    private const val TAG = "MineradioGL"

    fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) throw GlException("glCreateShader failed")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw GlException("Shader compile failed: $log")
        }
        return shader
    }

    fun linkProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES20.glCreateProgram()
        if (program == 0) throw GlException("glCreateProgram failed")
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw GlException("Program link failed: $log")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return program
    }

    fun attribLocation(program: Int, name: String): Int {
        val loc = GLES20.glGetAttribLocation(program, name)
        if (loc < 0) Log.w(TAG, "Attribute not found: $name")
        return loc
    }

    fun uniformLocation(program: Int, name: String): Int {
        val loc = GLES20.glGetUniformLocation(program, name)
        if (loc < 0) Log.w(TAG, "Uniform not found: $name")
        return loc
    }

    fun checkGlError(op: String) {
        val err = GLES20.glGetError()
        if (err != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError 0x${Integer.toHexString(err)}")
        }
    }
}

class GlException(message: String) : RuntimeException(message)
