package com.mars.viewer3d

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.IOUtils
import java.nio.charset.Charset


class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnTouchListener{

    private var obj = Obj()
    private var render = Render()

    /////////----------------------- Button listener -----------------------/////////
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_reset -> {
                this.render.scale = 1.0f
                this.render.rotMatrix = this.render.getRotMatrix(0.0f, 1.0f, 0.0f, 0.0f)
                this.render.tx = 0.0f
                this.render.ty = 0.0f
            }
            R.id.btn_open -> {
                val intent = Intent()
                intent.setType("*/*")
                intent.setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(Intent.createChooser(intent, "Select a 3d model file"), 111)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 111 && resultCode == Activity.RESULT_OK){
            val selectFile = data?.data
            Toast.makeText(this, "selectFile", Toast.LENGTH_SHORT).show()
        }
    }

    /////////----------------------- Touch listener -----------------------/////////
    // 0: rotation;  1: scale;  2: translation
    var touchMode:Int = -1
    var start0X = 0.0f
    var start0Y = 0.0f
    var start1X = 0.0f
    var start1Y = 0.0f
    var rotM_old = FloatArray(16)
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                this.start0X = event.rawX
                this.start0Y = event.rawY
                touchMode = 0
            }
            MotionEvent.ACTION_POINTER_DOWN ->{
                this.start1X = event.rawX
                this.start1Y = event.rawY
                touchMode = 1
            }
            MotionEvent.ACTION_UP -> {
                Matrix.setIdentityM(rotM_old, 0)
            }
            MotionEvent.ACTION_POINTER_UP ->{

            }
            MotionEvent.ACTION_MOVE -> {
                if(touchMode==0){
                    var currX = event.rawX
                    var currY = event.rawY
                    var rotAngleAxis = this.render.getRotArcBall(this.start0X, this.start0Y, currX, currY, 1500.0f, 3000.0f)
                    var rotM_curr = this.render.getRotMatrix(rotAngleAxis[0], rotAngleAxis[1], rotAngleAxis[2], rotAngleAxis[3])
                    var rotM_old_inv = FloatArray(16)
                    Matrix.invertM(rotM_old_inv, 0, rotM_old, 0)
                    var rotTemp = FloatArray(16)
                    Matrix.multiplyMM(rotTemp, 0, rotM_curr, 0, rotM_old_inv, 0)
                    Matrix.multiplyMM(this.render.rotMatrix, 0, rotTemp, 0, this.render.rotMatrix, 0)
                    rotM_old = rotM_curr
                }
                else if(touchMode==1){
                    var currX = event.rawX
                    var currY = event.rawY
                }
            }
        }
        return true
    }



//    var tranX_old = 0.0f
//    var tranY_old = 0.0f
//    var tranX_new = 0.0f
//    var tranY_new = 0.0f
//    override fun onTouch(v: View?, event: MotionEvent): Boolean {
//        when(event.action){
//            MotionEvent.ACTION_DOWN -> {
//                this.start0X = event.rawX
//                this.start0Y = event.rawY
//            }
//            MotionEvent.ACTION_UP -> {
////                this.tranX_old = 0.0f
////                this.tranY_old = 0.0f
////                this.tranX_new = 0.0f
////                this.tranY_new = 0.0f
//            }rotAngle:Float, rotAxisX:Float, rotAxisY:Float, rotAxisZ:Float
//            MotionEvent.ACTION_MOVE -> {
//                var currX = event.rawX
//                var currY = event.rawY
//                var rotAngleAxis = getRotArcBall(this.start0X, this.start0Y, currX, currY, 1000.0f, 1000.0f)
//                this.rotAngle = rotAngleAxis[0]
//                this.rotAxisX = rotAngleAxis[1]
//                this.rotAxisY = rotAngleAxis[2]
//                this.rotAxisZ = rotAngleAxis[3]
//                Log.i("rotAngle", rotAngle.toString())
////                this.tranX_new = currX - this.start0X
////                this.tranY_new = currY - this.start0Y
//            }
//        }
////        this.tx -= 0.001f * (tranX_new - tranX_old)
////        this.ty -= 0.001f * (tranY_new - tranY_old)
////        tranX_old = tranX_new
////        tranY_old = tranY_new
//        render.loadModelPose(this.scale, this.rotAngle,this.rotAxisX, this.rotAxisY, this.rotAxisZ, this.tx, this.ty)
//        return true
//    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize
        Matrix.setIdentityM(rotM_old, 0)

        // Set button listener
        btn_open.setOnClickListener(this)
        btn_reset.setOnClickListener(this)

        // Set touch listener
        viewZone.setOnTouchListener(this)

        // Sent mesh data to render
        var mesh = this.obj.loadObj(assets.open("male.obj"))
        var meshNorm = this.obj.normalizeVertex(mesh)
        meshNorm = this.obj.calcVertexNormal(meshNorm)
        this.render.loadMeshData(meshNorm)

        // Sent the shader file to render mesh
        val vertexShaderStream = resources.openRawResource(R.raw.mesh_vert)
        val vertexShaderString = IOUtils.toString(vertexShaderStream, Charset.defaultCharset())
        vertexShaderStream.close()
        val fragmentShaderStream = resources.openRawResource(R.raw.mesh_frag)
        val fragmentShaderString = IOUtils.toString(fragmentShaderStream, Charset.defaultCharset())
        fragmentShaderStream.close()
        this.render.loadShaderFile(vertexShaderString, fragmentShaderString)

        // Set render
        viewZone.setEGLContextClientVersion(2)
        viewZone.setRenderer(this.render)
        viewZone.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}

