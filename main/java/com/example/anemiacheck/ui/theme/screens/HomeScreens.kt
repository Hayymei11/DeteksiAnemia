package com.example.anemiacheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anemiacheck.ui.theme.CardSurfaceWhite
import com.example.anemiacheck.ui.theme.MedicalBlueLight
import com.example.anemiacheck.ui.theme.MedicalBluePrimary
import com.example.anemiacheck.ui.theme.TextDark

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MedicalBlueLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Ikon Header (Melambangkan Kesehatan/Jantung/Darah)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MedicalBluePrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Logo Kesehatan",
                tint = MedicalBluePrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Judul Aplikasi
        Text(
            text = "AnemiaCheck",
            color = TextDark,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Deskripsi Edukatif
        Text(
            text = "Deteksi dini indikasi anemia melalui pemindaian konjungtiva mata bawah Anda.",
            color = Color.Gray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Tombol 1: Ambil Foto Mata (Kamera)
        MenuCard(
            title = "Ambil Foto Mata",
            subtitle = "Pindai konjungtiva secara langsung",
            icon = Icons.Default.CameraAlt,
            onClick = onNavigateToCamera
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol 2: Pilih dari Galeri
        MenuCard(
            title = "Pilih dari Galeri",
            subtitle = "Unggah foto mata yang sudah ada",
            icon = Icons.Default.PhotoLibrary,
            onClick = onNavigateToGallery
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol 3: Riwayat Pengecekan
        MenuCard(
            title = "Riwayat Pengecekan",
            subtitle = "Pantau grafik kesehatan mingguan Anda",
            icon = Icons.Default.DateRange,
            onClick = onNavigateToHistory
        )
    }
}

// Komponen UI kustom untuk merapikan desain tombol menu
@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kotak Ikon di sebelah kiri
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MedicalBlueLight, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MedicalBluePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Teks Judul dan Subjudul di sebelah kanan
            Column {
                Text(
                    text = title,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}