package com.example.anemiacheck.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.anemiacheck.ui.theme.MedicalBluePrimary
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Status flash, di-set TRUE (menyala) secara default sesuai aturan medis kita
    var isFlashOn by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // ImageCapture digunakan agar kita bisa memotret dengan resolusi tinggi saat tombol ditekan
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Memastikan lampu flash menyala/mati sesuai state
    LaunchedEffect(isFlashOn, camera) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // LAPISAN 1: TAMPILAN KAMERA REAL-TIME
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val mainExecutor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture // Memasang modul penangkap gambar
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Gagal membuka kamera", exc)
                    }
                }, mainExecutor)

                previewView
            }
        )

        // LAPISAN 2: OVERLAY KOTAK PANDUAN (UI MEDIS)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Ukuran kotak panduan untuk mata (Dibuat Persegi agar pas dengan input AI 224x224)
            val rectSize = canvasWidth * 0.7f
            val topLeft = Offset((canvasWidth - rectSize) / 2f, (canvasHeight - rectSize) / 2f - 100f)

            // Menggambar latar belakang agak gelap
            drawRect(color = Color.Black.copy(alpha = 0.6f))

            // "Melubangi" bagian tengah agar kamera terlihat jelas (BlendMode.Clear)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = topLeft,
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(24f, 24f),
                blendMode = BlendMode.Clear
            )

            // Memberi garis tepi (border) pada kotak panduan
            drawRoundRect(
                color = Color.White,
                topLeft = topLeft,
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // LAPISAN 3: INSTRUKSI TEKS
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pindai Mata",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tarik perlahan kelopak mata bawah Anda. Posisikan area berwarna merah/pucat tepat di dalam kotak.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // LAPISAN 4: TOMBOL FLASH
        IconButton(
            onClick = { isFlashOn = !isFlashOn },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 24.dp)
                .size(56.dp)
                .background(if (isFlashOn) Color(0xFFFFD54F) else Color.DarkGray.copy(alpha = 0.7f), shape = CircleShape)
        ) {
            Icon(
                imageVector = if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                contentDescription = "Toggle Flash",
                tint = if (isFlashOn) Color.Black else Color.White
            )
        }

        // LAPISAN 5: TOMBOL AMBIL GAMBAR (SHUTTER)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
                .clickable {
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                // Ekstrak bitmap dari hasil jepretan
                                val bitmap = image.toBitmap()

                                // 2. Rotasi gambar jika posisinya miring dari sensor kamera
                                val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                // 3. POTONG GAMBAR (Crop) Agar fokus ke area kotak panduan saja
                                // Kita ambil bagian tengah gambar agar sinkron dengan gambar pelatihan (eye close-up)
                                val croppedBitmap = cropToCenter(rotatedBitmap)

                                image.close()

                                // Kirim gambar ke otak AI (AnemiaClassifier)
                                onImageCaptured(croppedBitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraScreen", "Gagal memotret", exception)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Lingkaran dalam tombol shutter
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MedicalBluePrimary)
            )
        }
    }
}

/**
 * Fungsi Pembantu: Memotong gambar tepat di tengah (Center Crop) agar AI fokus ke mata saja.
 */
fun cropToCenter(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val newDimension = if (width < height) width else height
    val left = (width - newDimension) / 2
    val top = (height - newDimension) / 2
    return Bitmap.createBitmap(bitmap, left, top, newDimension, newDimension)
}
