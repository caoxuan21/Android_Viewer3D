package com.mars.viewer3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.acos
import kotlin.math.sqrt


class Render: GLSurfaceView.Renderer {
    // Shader program
    private lateinit var vertexShaderString:String
    private lateinit var fragmentShaderString:String
    private var program: Int = -1

    // Camera posVertex, look at and model rotation
    private var projMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    private var modelMatrix = FloatArray(16)
    private var MVP = FloatArray(16)

    // Model pose
    var scale = 1.0f
    var rotMatrix = FloatArray(16)
    var tx = 0.0f
    var ty = 0.0f

    // The obj mesh data
    private lateinit var mesh: Mesh

    //// ------------------ Render Override Functions ------------------ ////
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.9f,0.9f,0.9f,1.0f)
        GLES20.glClearDepthf(1.0f)
        // Initialize shader program
        initProgram()
        // Set camera posVertex and look at
        setCameraMatrix()
        // Set model rotation matrix
        this.rotMatrix = getRotMatrix(0.0f, 1.0f, 0.0f, 0.0f)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(this.program)

        // Send vertex buffer to vertex shader
        val posVertex = GLES20.glGetAttribLocation(this.program, "vertex")
        GLES20.glEnableVertexAttribArray(posVertex)
        GLES20.glVertexAttribPointer(posVertex, 3, GLES20.GL_FLOAT, false, 3 * 4, this.mesh.vertex)

        // Send normal buffer to vertex shader
        val posNormal = GLES20.glGetAttribLocation(this.program, "normal")
        GLES20.glEnableVertexAttribArray(posNormal)
        GLES20.glVertexAttribPointer(posNormal, 3, GLES20.GL_FLOAT, false, 3 * 4, this.mesh.normal)

        // Update MVP matrix
        setModelMatrix(this.scale,this.rotMatrix, this.tx, this.ty)
        setMVPMatrix()
        val posRotMat = GLES20.glGetUniformLocation(this.program, "rotMat")
        GLES20.glUniformMatrix4fv(posRotMat, 1, false, this.rotMatrix, 0)
        val posMVP = GLES20.glGetUniformLocation(this.program, "MVP")
        GLES20.glUniformMatrix4fv(posMVP, 1, false, this.MVP, 0)

        // Draw the triangular face
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, this.mesh.numTriface * 3, GLES20.GL_UNSIGNED_SHORT, this.mesh.triface)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }


    //// ------------------ Control Functions ------------------ ////
    // Load the mesh data
    fun loadMeshData(mesh: Mesh){
        this.mesh = mesh
    }

    // Load shader file
    fun loadShaderFile(vertexShaderString:String, fragmentShaderString:String){
        this.vertexShaderString = vertexShaderString
        this.fragmentShaderString = fragmentShaderString
    }

    fun getRotArcBall(startX:Float, startY:Float, endX:Float, endY:Float, viewW:Float, viewH:Float):FloatArray{
        // get 3d coordinate of start point
        var ax = (2*startX - viewW) / viewW
        var ay = (2*startY - viewH) / viewH
        var az = 0.0
        var la2 = ax*ax + ay*ay
        if (la2 < 1.0){
            az = sqrt(1.0 - la2)
        }
        else{
            var la = sqrt(la2)
            ax /= la
            ay /= la
        }
        // get 3d coordinate of end point
        var bx = (2*endX - viewW) / viewW
        var by = (2*endY - viewH) / viewH
        var bz = 0.0
        var lb2 = bx*bx + by*by
        if (lb2 < 1.0f){
            bz = sqrt(1.0 - lb2)
        }
        else{
            var lb = sqrt(lb2)
            bx /= lb
            by /= lb
        }
        // get rotation angle
        var dotAB = (ax*bx + ay*by + az*bz) / (sqrt(ax*ax + ay*ay + az*az) * sqrt(bx*bx + by*by + bz*bz))
        var rotAngle = acos(dotAB) * 180.0f / kotlin.math.PI
        // get rotation axis
        var axisX = ay * bz - az * by
        var axisY = az * bx - ax * bz
        var axisZ = ax * by - ay * bx
        var length = sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ).toFloat()
        axisX = axisX / length
        axisY = axisY / length
        axisZ = axisZ / length

        var outArray = floatArrayOf(rotAngle.toFloat(), axisX.toFloat(), axisY.toFloat(), axisZ.toFloat())
        return outArray
    }


    //// ------------------ Support Functions ------------------ ////
    // Initialize shader program
    fun initProgram(){
        // Create vertex shader
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShader, this.vertexShaderString)

        // Create fragment shader
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShader, this.fragmentShaderString)

        // Compile shaders
        GLES20.glCompileShader(vertexShader)
        GLES20.glCompileShader(fragmentShader)

        // Create shader program
        this.program = GLES20.glCreateProgram()
        GLES20.glAttachShader(this.program, vertexShader)
        GLES20.glAttachShader(this.program, fragmentShader)
        GLES20.glLinkProgram(this.program)
        GLES20.glValidateProgram(this.program)
        GLES20.glUseProgram(this.program)
    }

    // Set the camera parameters
    fun setCameraMatrix(){
        Matrix.orthoM(
            this.projMatrix,0,
            -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f
        )
//        Matrix.perspectiveM(
//            this.projMatrix,0,
//            10.0f, 1.0f, 1.0f, 100.0f
//        )
        Matrix.setLookAtM(
            this.viewMatrix,0,
            0.0f, 0.0f, -2.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f
        )
    }

    // Get the matrix of rotation rotAngle along axis (rotAxisX, rotAxisY, rotAxisZ)
    fun getRotMatrix(rotAngle:Float, rotAxisX:Float, rotAxisY:Float, rotAxisZ:Float):FloatArray{
        var rotMatrix = FloatArray(16)
        Matrix.setIdentityM(rotMatrix, 0)
        Matrix.setRotateM(rotMatrix, 0, rotAngle, rotAxisX, rotAxisY, rotAxisZ)
        return rotMatrix
    }

    // Set the model scale, rotation and translation
    fun setModelMatrix(scale:Float, rotMatrix:FloatArray, tx:Float, ty:Float){
        var scaleMatrix = FloatArray(16)
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.scaleM(scaleMatrix,0, scale, scale, scale)

        var transMatrix = FloatArray(16)
        Matrix.setIdentityM(transMatrix, 0)
        Matrix.translateM(transMatrix, 0, tx, ty, 0.0f)

        var tempMatrix = FloatArray(16)
        Matrix.multiplyMM(
            tempMatrix, 0,
            transMatrix, 0,
            rotMatrix, 0
        )
        Matrix.multiplyMM(
            this.modelMatrix, 0,
            tempMatrix, 0,
            scaleMatrix, 0
        )
    }

    // Set MVP matrix
    fun setMVPMatrix(){
        var VPMatrix = FloatArray(16)
        Matrix.multiplyMM(
            VPMatrix, 0,
            this.projMatrix, 0,
            this.viewMatrix, 0
        )
        Matrix.multiplyMM(
            this.MVP, 0,
            VPMatrix, 0,
            this.modelMatrix, 0
        )
    }

}