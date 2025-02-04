package com.diespy.app.ml.detector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.diespy.app.ml.metadata.MetaData
import com.diespy.app.ml.metadata.MetaData.extractClassNamesFromLabelFile
import com.diespy.app.ml.metadata.MetaData.extractClassNamesFromMetadata
import com.diespy.app.ml.models.DiceBoundingBox
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

/**
 * Detector class responsible for running inference using a TensorFlow Lite model.
 * Detects dice faces in images and returns bounding boxes.
 */
class DiceDetector(
    private val context: Context, // Context for resource access
    private val modelFilePath: String, // Path to TFLite model
    private val labelFilePath: String?, // Optional label file path
    private val listener: DetectorListener, // Callback listener for detection events
    private val showMessage: (String) -> Unit // Function to display messages
) {

    private var interpreter: Interpreter // TFLite model interpreter
    private val labels = mutableListOf<String>() // List of class labels

    // Model input/output properties
    private var inputWidth = 0
    private var inputHeight = 0
    private var numChannels = 0
    private var numDetections = 0

    /**
     * Image pre-processing pipeline (Normalizes and converts input format)
     */
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STD_DEV)) // Normalize pixel values (0-1 range)
        .add(CastOp(INPUT_DATA_TYPE)) // Convert to required data type
        .build()

    init {
        val options = Interpreter.Options().apply {
            numThreads = THREAD_COUNT // Optimize inference speed
        }

        // Load TFLite model
        val modelFile = FileUtil.loadMappedFile(context, modelFilePath)
        interpreter = Interpreter(modelFile, options)

        // Load labels (if available)
        labels.addAll(extractClassNamesFromMetadata(modelFile))
        if (labels.isEmpty()) {
            if (labelFilePath == null) {
                showMessage("Model metadata missing. Provide LABELS_PATH in Constants.kt")
                labels.addAll(MetaData.DEFAULT_CLASSES) // Default labels
            } else {
                labels.addAll(extractClassNamesFromLabelFile(context, labelFilePath))
            }
        }

        labels.forEach(::println) // Debug: Print loaded labels

        // Retrieve model input/output dimensions
        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        if (inputShape != null) {
            inputWidth = inputShape[1]
            inputHeight = inputShape[2]

            // Handle different input shape formats
            if (inputShape[1] == 3) {
                inputWidth = inputShape[2]
                inputHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numDetections = outputShape[1] // Number of detected objects
            numChannels = outputShape[2] // Data per object (e.g., bounding box + confidence)
        }
    }

    /**
     * Restarts the detector with optional GPU acceleration.
     */
    fun restart(useGpu: Boolean) {
        interpreter.close() // Close existing interpreter

        val options = if (useGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply {
                if (compatList.isDelegateSupportedOnThisDevice) {
                    addDelegate(GpuDelegate(compatList.bestOptionsForThisDevice)) // Enable GPU
                } else {
                    numThreads = THREAD_COUNT // Fallback to CPU
                }
            }
        } else {
            Interpreter.Options().apply {
                numThreads = THREAD_COUNT // Use CPU with multi-threading
            }
        }

        val modelFile = FileUtil.loadMappedFile(context, modelFilePath)
        interpreter = Interpreter(modelFile, options)
    }

    /**
     * Closes the TFLite interpreter to free resources.
     */
    fun close() {
        interpreter.close()
    }

    /**
     * Runs object detection on the provided bitmap image.
     */
    fun detect(image: Bitmap) {
        if (inputWidth == 0 || inputHeight == 0 || numChannels == 0 || numDetections == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        // Resize the image to match model input dimensions
        val resizedImage = Bitmap.createScaledBitmap(image, inputWidth, inputHeight, false)

        // Convert image to tensor format
        val tensorImage = TensorImage(INPUT_DATA_TYPE)
        tensorImage.load(resizedImage)
        val processedImage = imageProcessor.process(tensorImage)
        val inputBuffer = processedImage.buffer

        // Prepare output buffer
        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, numChannels, numDetections), OUTPUT_DATA_TYPE
        )
        interpreter.run(inputBuffer, outputBuffer.buffer)

        // Process model output
        val detectedBoxes = extractBoundingBoxes(outputBuffer.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // Notify listener
        if (detectedBoxes.isEmpty()) {
            listener.onEmptyDetect()
        } else {
            listener.onDetect(detectedBoxes, inferenceTime)
        }
    }

    /**
     * Extracts bounding boxes from the model output.
     */
    private fun extractBoundingBoxes(outputArray: FloatArray): List<DiceBoundingBox> {
        val diceBoundingBoxes = mutableListOf<DiceBoundingBox>()

        for (i in 0 until numDetections) {
            val confidence = outputArray[i * numChannels + 4] // Confidence score

            if (confidence > CONFIDENCE_THRESHOLD) {
                val x1 = outputArray[i * numChannels]
                val y1 = outputArray[i * numChannels + 1]
                val x2 = outputArray[i * numChannels + 2]
                val y2 = outputArray[i * numChannels + 3]
                val classIndex = outputArray[i * numChannels + 5].toInt()
                val className = labels[classIndex]

                diceBoundingBoxes.add(
                    DiceBoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        confidence = confidence, classIndex = classIndex, className = className
                    )
                )
            }
        }
        return diceBoundingBoxes
    }

    /**
     * Interface for handling detection results.
     */
    interface DetectorListener {
        fun onEmptyDetect() // No objects detected
        fun onDetect(diceBoundingBoxes: List<DiceBoundingBox>, inferenceTime: Long) // Objects detected
    }

    /**
     * Companion object for constants.
     */
    companion object {
        private const val THREAD_COUNT = 4 // Number of threads for CPU inference
        private const val INPUT_MEAN = 0f // Mean for input normalization
        private const val INPUT_STD_DEV = 255f // Standard deviation for normalization
        private val INPUT_DATA_TYPE = DataType.FLOAT32 // Input tensor data type
        private val OUTPUT_DATA_TYPE = DataType.FLOAT32 // Output tensor data type
        private const val CONFIDENCE_THRESHOLD = 0.5F // Minimum confidence score
    }
}
