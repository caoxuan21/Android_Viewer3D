package com.mars.viewer3d

import android.util.Log
import java.io.InputStream
import java.nio.*
import java.util.Scanner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


data class Mesh(
    var vertex: FloatBuffer,
    var numVertex: Int,
    var normal: FloatBuffer,
    var numNormal: Int,
    var triface: ShortBuffer,
    var numTriface: Int
)


class Obj{
    fun loadObj(path: InputStream):Mesh{
        // read obj file
        var vertexLine = mutableListOf<String>()
        var trifaceLine = mutableListOf<String>()
        val scanner = Scanner(path)
        while (scanner.hasNextLine()){
            val line = scanner.nextLine()
            if (line.startsWith("v")){
                vertexLine.add(line)
            }
            else if (line.startsWith("f")){
                trifaceLine.add(line)
            }
        }
        scanner.close()

        // allocate buffer for vertex
        val bufferVertex:ByteBuffer = ByteBuffer.allocateDirect(vertexLine.size * 3 * 4)
        bufferVertex.order(ByteOrder.nativeOrder())
        var vertex = bufferVertex.asFloatBuffer()
        for (line in vertexLine){
            val vxyz = line.split(" ")
            val x = vxyz[1].toFloat()
            val y = vxyz[2].toFloat()
            val z = vxyz[3].toFloat()
            vertex.put(x)
            vertex.put(y)
            vertex.put(-z)
        }
        vertex.position(0)

        // allocate buffer for vertex
        val bufferNormal:ByteBuffer = ByteBuffer.allocateDirect(vertexLine.size * 3 * 4)
        bufferNormal.order(ByteOrder.nativeOrder())
        var normal = bufferNormal.asFloatBuffer()

        // allocate buffer for triface
        var bufferTriface:ByteBuffer = ByteBuffer.allocateDirect(trifaceLine.size * 3 * 2)
        bufferTriface.order(ByteOrder.nativeOrder())
        var triface = bufferTriface.asShortBuffer()
        for (line in trifaceLine){
            val f123 = line.split(" ")
            val f1 = (f123[1].toShort() - 1).toShort()
            val f2 = (f123[2].toShort() - 1).toShort()
            val f3 = (f123[3].toShort() - 1).toShort()
            triface.put(f1)
            triface.put(f2)
            triface.put(f3)
        }
        triface.position(0)

        // return mesh data
        var mesh = Mesh(vertex, vertexLine.size, normal, vertexLine.size, triface, trifaceLine.size)
        return mesh
    }

    fun normalizeVertex(mesh: Mesh):Mesh{
        var sumX = 0.0f
        var sumY = 0.0f
        var sumZ = 0.0f
        var minX = 99999.0f
        var maxX = -99999.0f
        for (ith in 0 until mesh.numVertex){
            var x = mesh.vertex.get(ith*3)
            var y = mesh.vertex.get(ith*3 + 1)
            var z = mesh.vertex.get(ith*3 + 2)
            sumX += x
            sumY += y
            sumZ += z
            minX = min(minX, x)
            maxX = max(maxX, x)
        }
        var centerX = sumX / mesh.numVertex
        var centerY = sumY / mesh.numVertex
        var centerZ = sumZ / mesh.numVertex
        var scale = 1.0f / (maxX - minX)

        var meshNorm = mesh
        for (ith in 0 until mesh.numVertex){
            var x = mesh.vertex.get(ith*3)
            var y = mesh.vertex.get(ith*3 + 1)
            var z = mesh.vertex.get(ith*3 + 2)
            meshNorm.vertex.put(ith*3, scale*(x-centerX))
            meshNorm.vertex.put(ith*3 + 1, scale*(y-centerY))
            meshNorm.vertex.put(ith*3 + 2, scale*(z-centerZ))
        }
        return meshNorm
    }

    fun calcVertexNormal(mesh: Mesh):Mesh{
        var bufferVertexNormal:ByteBuffer = ByteBuffer.allocateDirect(mesh.numVertex * 3 * 4)
        bufferVertexNormal.order(ByteOrder.nativeOrder())
        var vertexNormal = bufferVertexNormal.asFloatBuffer()
        var bufferLengthNormal:ByteBuffer = ByteBuffer.allocateDirect(mesh.numVertex * 4)
        bufferLengthNormal.order(ByteOrder.nativeOrder())
        var lengthNormal = bufferLengthNormal.asFloatBuffer()
        for (ith in 0 until mesh.numTriface){
            var f1 = mesh.triface.get(ith*3).toInt()
            var f2 = mesh.triface.get(ith*3 + 1).toInt()
            var f3 = mesh.triface.get(ith*3 + 2).toInt()
            var x1 = mesh.vertex.get(f1*3)
            var y1 = mesh.vertex.get(f1*3 + 1)
            var z1 = mesh.vertex.get(f1*3 + 2)
            var x2 = mesh.vertex.get(f2*3)
            var y2 = mesh.vertex.get(f2*3 + 1)
            var z2 = mesh.vertex.get(f2*3 + 2)
            var x3 = mesh.vertex.get(f3*3)
            var y3 = mesh.vertex.get(f3*3 + 1)
            var z3 = mesh.vertex.get(f3*3 + 2)
            var vecAx = x2 - x1
            var vecAy = y2 - y1
            var vecAz = z2 - z1
            var vecBx = x3 - x1
            var vecBy = y3 - y1
            var vecBz = z3 - z1
            var normX = vecAy * vecBz - vecAz * vecBy
            var normY = vecAz * vecBx - vecAx * vecBz
            var normZ = vecAx * vecBy - vecAy * vecBx
            var length = sqrt(normX*normX + normY*normY + normZ*normZ)
            normX = normX / length
            normY = normY / length
            normZ = normZ / length
            vertexNormal.put(f1*3, vertexNormal.get(f1*3) + normX)
            vertexNormal.put(f1*3+1, vertexNormal.get(f1*3+1) + normY)
            vertexNormal.put(f1*3+2, vertexNormal.get(f1*3+2) + normZ)
            lengthNormal.put(f1, lengthNormal.get(f1) + 1.0f)
            vertexNormal.put(f2*3, vertexNormal.get(f2*3) + normX)
            vertexNormal.put(f2*3+1, vertexNormal.get(f2*3+1) + normY)
            vertexNormal.put(f2*3+2, vertexNormal.get(f2*3+2) + normZ)
            lengthNormal.put(f2, lengthNormal.get(f2) + 1.0f)
            vertexNormal.put(f3*3, vertexNormal.get(f3*3) + normX)
            vertexNormal.put(f3*3+1, vertexNormal.get(f3*3+1) + normY)
            vertexNormal.put(f3*3+2, vertexNormal.get(f3*3+2) + normZ)
            lengthNormal.put(f3, lengthNormal.get(f3) + 1.0f)
        }

        for (ith in 0 until mesh.numVertex) {
            vertexNormal.put(ith*3, vertexNormal.get(ith*3) / lengthNormal.get(ith))
            vertexNormal.put(ith*3+1, vertexNormal.get(ith*3+1) / lengthNormal.get(ith))
            vertexNormal.put(ith*3+2, vertexNormal.get(ith*3+2) / lengthNormal.get(ith))
        }

        var meshOut = mesh
        meshOut.normal = vertexNormal
        return meshOut
    }
}