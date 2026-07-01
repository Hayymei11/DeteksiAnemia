package com.example.anemiacheck.classifier

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// Sealed class untuk mengelola status hasil deteksi Anemia
sealed class AnemiaResult {
    data class Normal(val confidence: Float) : AnemiaResult()
    data class Anaemia(val confidence: Float) : AnemiaResult()
    object Error : AnemiaResult()
}

class AnemiaClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val MODEL_FILE_NAME = "model_anemia_mobilenetv2.tflite"

    // =================================================================================
    // KONTROL KALIBRASI (Ubah bagian ini jika deteksi terbalik atau kurang sensitif)
    // =================================================================================
    
    // 1. TUKAR LABEL (BALIK): 
    // Jika mata PUCAT dibilang "Normal" (dengan skor tinggi), ubah ini jadi TRUE.
    private val isLabelSwapped = true 

    // 2. AMBANG BATAS (SENSITIVITAS):
    // Nilai standar 0.5 (50%). 
    // JIKA SUSAH DETEKSI ANEMIA: Turunkan ke 0.3 atau 0.4 agar lebih sensitif.
    private val anemiaThreshold = 0.4f
    
    // =================================================================================

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            // Memuat file model dari folder assets
            val modelBuffer = loadModelFile(context, MODEL_FILE_NAME)

            // Konfigurasi thread agar AI memproses lebih cepat
            val options = Interpreter.Options().apply {
                numThreads = 4
            }

            interpreter = Interpreter(modelBuffer, options)
            Log.d("AnemiaClassifier", "Model AI berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("AnemiaClassifier", "Gagal memuat model. Pastikan file ada di folder assets.", e)
            e.printStackTrace()
        }
    }

    // Fungsi membaca file .tflite dari folder assets menjadi MappedByteBuffer
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): AnemiaResult {
        if (interpreter == null) return AnemiaResult.Error

        try {
            // 1. PRE-PROCESSING (Sesuai Python: Resize 224x224 & Normalisasi / 255.0)
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0.0f, 255.0f))
                .build()

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 2. OUTPUT BUFFER (2 Kelas: Normal & Anaemia)
            val outputBuffer = Array(1) { FloatArray(2) }

            // 3. JALANKAN INFERENCE
            interpreter?.run(tensorImage.buffer, outputBuffer)

            // 4. HASIL (Model Anda sudah pakai Softmax, jadi output sudah 0.0 - 1.0)
            var normalScore: Float
            var anaemiaScore: Float

            if (!isLabelSwapped) {
                // Standar Python: Index 0 = Normal, Index 1 = Anaemia
                normalScore = outputBuffer[0][0]
                anaemiaScore = outputBuffer[0][1]
            } else {
                // Balik: Index 1 = Normal, Index 0 = Anaemia
                normalScore = outputBuffer[0][1]
                anaemiaScore = outputBuffer[0][0]
            }

            Log.d("AnemiaAI", "--- ANALISIS MATA ---")
            Log.d("AnemiaAI", "Keyakinan SEHAT: %.2f%%".format(normalScore * 100))
            Log.d("AnemiaAI", "Keyakinan ANEMIA: %.2f%%".format(anaemiaScore * 100))
            Log.d("AnemiaAI", "Sensitivitas Threshold: $anemiaThreshold")

            // 5. KEPUTUSAN
            // Kita pakai Threshold agar kita bisa mengatur seberapa sensitif AI ini
            return if (anaemiaScore >= anemiaThreshold) {
                AnemiaResult.Anaemia(confidence = anaemiaScore)
            } else {
                AnemiaResult.Normal(confidence = normalScore)
            }

        } catch (e: Exception) {
            Log.e("AnemiaClassifier", "Error: ${e.message}")
            return AnemiaResult.Error
        }
    }

    // Fungsi pembersihan memori saat aplikasi ditutup
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
