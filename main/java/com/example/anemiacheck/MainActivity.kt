package com.example.anemiacheck

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.anemiacheck.classifier.AnemiaClassifier
import com.example.anemiacheck.classifier.AnemiaResult
import com.example.anemiacheck.data.AppDatabase
import com.example.anemiacheck.data.HistoryEntity
import com.example.anemiacheck.ui.screens.CameraScreen
import com.example.anemiacheck.ui.screens.HistoryScreen
import com.example.anemiacheck.ui.screens.HomeScreen
import com.example.anemiacheck.ui.screens.ResultScreen
import com.example.anemiacheck.ui.theme.AnemiaCheckTheme
import kotlinx.coroutines.launch

// Daftar Halaman Aplikasi Kita
enum class Screen { HOME, CAMERA, RESULT, HISTORY }

class MainActivity : ComponentActivity() {

    // Menyimpan referensi Otak AI dan Database secara global
    private lateinit var classifier: AnemiaClassifier
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Menghidupkan AI dan Database saat aplikasi pertama kali dibuka
        classifier = AnemiaClassifier(this)
        database = AppDatabase.getDatabase(this)

        setContent {
            AnemiaCheckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnemiaApp(classifier, database)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Membersihkan memori AI saat aplikasi ditutup agar HP tidak lag
        classifier.close()
    }
}

@Composable
fun AnemiaApp(classifier: AnemiaClassifier, database: AppDatabase) {
    // Helper Fungsi untuk mengecilkan ukuran gambar agar database tidak crash (SQLiteBlobTooBigException)
    fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        return if (bitmapRatio > 1) {
            val finalWidth = maxSize
            val finalHeight = (finalWidth / bitmapRatio).toInt()
            Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        } else {
            val finalHeight = maxSize
            val finalWidth = (finalHeight * bitmapRatio).toInt()
            Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        }
    }

    // Helper Fungsi untuk memotong gambar tepat di tengah (Center Crop)
    // Digunakan agar foto dari Galeri juga fokus ke area mata saja
    fun cropToCenter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newDimension = if (width < height) width else height
        val left = (width - newDimension) / 2
        val top = (height - newDimension) / 2
        return Bitmap.createBitmap(bitmap, left, top, newDimension, newDimension)
    }

    // State Pengontrol Navigasi & Data Mentah
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var classificationResult by remember { mutableStateOf<AnemiaResult?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Menarik data riwayat dari Room Database secara reaktif (Otomatis update)
    val historyList by database.historyDao().getAllHistory().collectAsState(initial = emptyList())

    // Helper Fungsi: Menjalankan AI saat gambar masuk
    fun processImage(bitmap: Bitmap) {
        capturedBitmap = bitmap
        classificationResult = classifier.classify(bitmap)
        currentScreen = Screen.RESULT // Pindah ke layar hasil
    }

    // 2. Fitur Pilih Dari Galeri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Mengubah URI Galeri menjadi format Bitmap agar bisa dibaca AI
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    // POTONG GAMBAR (Crop) Agar fokus ke area tengah (mata)
                    // Sama seperti fitur kamera agar AI tidak bingung melihat seluruh wajah
                    val croppedBitmap = cropToCenter(bitmap)
                    processImage(croppedBitmap)
                } else {
                    Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 3. Fitur Pengecekan Izin Kamera (Otomatis Minta Izin ke Pengguna)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentScreen = Screen.CAMERA
        } else {
            Toast.makeText(context, "Izin kamera wajib diberikan untuk fitur ini", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. Jembatan Navigasi (Router Utama)
    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                onNavigateToCamera = {
                    val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        currentScreen = Screen.CAMERA
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onNavigateToGallery = {
                    galleryLauncher.launch("image/*") // Membuka file explorer bawaan HP
                },
                onNavigateToHistory = {
                    currentScreen = Screen.HISTORY
                }
            )
        }
        Screen.CAMERA -> {
            CameraScreen(
                onImageCaptured = { bitmap ->
                    processImage(bitmap)
                }
            )
        }
        Screen.RESULT -> {
            if (capturedBitmap != null && classificationResult != null) {
                ResultScreen(
                    result = classificationResult!!,
                    capturedImage = capturedBitmap!!,
                    onSaveToDatabase = { bitmap, isAnaemia, timestamp ->
                        // Proses penyimpanan ke SQLite Database di background thread
                        coroutineScope.launch {
                            // Kecilkan gambar sebelum disimpan ke DB agar tidak kena SQLiteBlobTooBigException
                            val smallBitmap = resizeBitmap(bitmap, 800)
                            val entity = HistoryEntity(
                                image = smallBitmap,
                                isAnaemia = isAnaemia,
                                timestamp = timestamp
                            )
                            database.historyDao().insertHistory(entity)
                        }
                    },
                    onNavigateHome = {
                        // Reset data dan kembali
                        capturedBitmap = null
                        classificationResult = null
                        currentScreen = Screen.HOME
                    }
                )
            } else {
                currentScreen = Screen.HOME
            }
        }
        Screen.HISTORY -> {
            HistoryScreen(
                historyList = historyList,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
    }
}