package com.diespy.app

import android.content.Context
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer

/**
 * Utility object for extracting class names from a TensorFlow Lite model's metadata
 * or an external label file.
 */
object MetaData {

    /**
     * Extracts class names from the metadata of a TensorFlow Lite model.
     */
    fun extractClassNamesFromMetadata(model: MappedByteBuffer): List<String> {
        return try {
            val metadataExtractor = MetadataExtractor(model)
            val inputStream = metadataExtractor.getAssociatedFile("temp_meta.txt")

            // Read metadata file content if available
            val metadataContent = inputStream?.bufferedReader()?.use { it.readText() } ?: return emptyList()

            // Regex to extract the 'names' section from metadata
            val regex = Regex("'names': \\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(metadataContent)
            val extractedNames = match?.groups?.get(1)?.value ?: return emptyList()

            // Extract individual class names from metadata
            val nameRegex = Regex("\"([^\"]*)\"|'([^']*)'")
            nameRegex.findAll(extractedNames)
                .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                .toList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    /**
     * Reads class labels from a label file stored in the app's assets.
     */
    fun extractClassNamesFromLabelFile(context: Context, labelFilePath: String): List<String> {
        return try {
            val inputStream: InputStream = context.assets.open(labelFilePath)
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() }
                    .toList()
            }
        } catch (exception: IOException) {
            emptyList()
        }
    }

    /**
     * Default placeholder labels (1,000 class names) for use when no labels are found.
     * Example: "Class1", "Class2", ..., "Class1000"
     */
    val DEFAULT_CLASSES = List(1000) { "Class${it + 1}" }
}
