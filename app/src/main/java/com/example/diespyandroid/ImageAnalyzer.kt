package com.example.diespyandroid

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils

class MyImageAnalyzer(
    private val model: Module,
    private val overlayView: OverlayView
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bitmap = Utils.imageProxyToBitmap(image)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
        val detections = Utils.parseDetections(outputTensor.dataAsFloatArray, bitmap.width, bitmap.height)

        // Update the OverlayView with detected bounding boxes
        overlayView.boxes = detections
        overlayView.invalidate()

        image.close()
    }
}