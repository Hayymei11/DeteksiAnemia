package com.example.anemiacheck.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anemiacheck.classifier.AnemiaResult
import com.example.anemiacheck.ui.theme.CardSurfaceWhite
import com.example.anemiacheck.ui.theme.MedicalBlueLight
import com.example.anemiacheck.ui.theme.MedicalBluePrimary
import com.example.anemiacheck.ui.theme.StatusAnemiaRed
import com.example.anemiacheck.ui.theme.StatusNormalGreen
import com.example.anemiacheck.ui.theme.TextDark

@Composable
fun ResultScreen(
    result: AnemiaResult,
    capturedImage: Bitmap,
    // Callback ini akan dipanggil otomatis untuk menyimpan ke Room DB
    onSaveToDatabase: (Bitmap, Boolean, Long) -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // LOGIKA OTOMATIS: Menyimpan riwayat saat layar ini pertama kali dirender
    LaunchedEffect(Unit) {
        if (result !is AnemiaResult.Error) {
            val isAnaemia = result is AnemiaResult.Anaemia
            val timestamp = System.currentTimeMillis()
            // Mengirim data ke MainActivity/ViewModel untuk disimpan ke Room Database
            onSaveToDatabase(capturedImage, isAnaemia, timestamp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MedicalBlueLight)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // FOTO MATA PASIEN (Ditampilkan dalam bingkai lingkaran)
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(4.dp, MedicalBluePrimary, CircleShape)
                .background(Color.LightGray)
        ) {
            Image(
                bitmap = capturedImage.asImageBitmap(),
                contentDescription = "Foto Mata Pasien",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ANALISIS LOGIKA OUTPUT (Normal atau Anemia)
        when (result) {
            is AnemiaResult.Normal -> {
                ResultCard(
                    isAnaemia = false,
                    confidence = result.confidence,
                    title = "Normal",
                    description = "Konjungtiva Anda terlihat segar dan memiliki intensitas warna merah muda yang sehat.",
                    educationText = "Tetap jaga konsumsi nutrisi harian Anda! Makan makanan bergizi seperti bayam, brokoli, dan protein tinggi untuk mempertahankan kesehatan darah."
                )
            }
            is AnemiaResult.Anaemia -> {
                ResultCard(
                    isAnaemia = true,
                    confidence = result.confidence,
                    title = "Indikasi Anemia",
                    description = "Terdeteksi indikasi kepucatan pada konjungtiva mata Anda yang mengarah pada gejala Anemia.",
                    educationText = "Tingkatkan konsumsi makanan kaya zat besi (daging merah, hati ayam, sayuran hijau). Aplikasi ini adalah alat skrining awal, bukan diagnosis final laboratorium."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // TOMBOL AKSI MEDIS (Buka Google Maps mencari klinik)
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=klinik+terdekat")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            // Fallback jika Google Maps tidak terinstal
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/klinik+terdekat")))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAnemiaRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocalHospital, contentDescription = "Klinik")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cari Klinik Terdekat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            AnemiaResult.Error -> {
                Text("Terjadi kesalahan saat memproses gambar.", color = StatusAnemiaRed)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TOMBOL KEMBALI KE BERANDA
        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = "Beranda")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembali ke Beranda", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Komponen Card UI Khusus untuk menyajikan data secara profesional
@Composable
fun ResultCard(
    isAnaemia: Boolean,
    confidence: Float,
    title: String,
    description: String,
    educationText: String
) {
    val statusColor = if (isAnaemia) StatusAnemiaRed else StatusNormalGreen
    val statusIcon = if (isAnaemia) Icons.Default.Warning else Icons.Default.CheckCircle

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = statusColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tingkat Keyakinan AI: ${(confidence * 100).format(1)}%",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MedicalBlueLight, thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                color = TextDark,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = educationText,
                    color = statusColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Justify,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// Helper untuk memformat desimal
fun Float.format(digits: Int) = String.format("%.${digits}f", this)