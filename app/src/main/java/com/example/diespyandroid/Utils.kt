package com.example.diespyandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object Utils {

    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }

//    fun assetFilePath(context: Context, asset: String): String {
//        val file = File(context.filesDir, asset)
//
//        try {
//            val inpStream: InputStream = context.assets.open(asset)
//            try {
//                val outStream = FileOutputStream(file, false)
//                val buffer = ByteArray(4 * 1024)
//                var read: Int
//
//                while (true) {
//                    read = inpStream.read(buffer)
//                    if (read == -1) {
//                        break
//                    }
//                    outStream.write(buffer, 0, read)
//                }
//                outStream.flush()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            return file.absolutePath
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return ""
//    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        // Convert ImageProxy to Bitmap (implementation depends on the input image format)
        TODO("Add ImageProxy to Bitmap conversion logic here")
    }

    fun parseDetections(output: FloatArray, imgWidth: Int, imgHeight: Int): List<RectF> {
        val boxes = mutableListOf<RectF>()
        for (i in output.indices step 6) {
            val confidence = output[i + 4]
            if (confidence > 0.5) { // Confidence threshold
                val x1 = output[i] * imgWidth
                val y1 = output[i + 1] * imgHeight
                val x2 = output[i + 2] * imgWidth
                val y2 = output[i + 3] * imgHeight
                boxes.add(RectF(x1, y1, x2, y2))
            }
        }
        return boxes
    }
}