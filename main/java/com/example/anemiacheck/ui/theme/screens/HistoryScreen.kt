package com.example.anemiacheck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.anemiacheck.data.HistoryEntity
import com.example.anemiacheck.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<HistoryEntity>,
    onNavigateBack: () -> Unit
) {
    // State untuk mengontrol Pop-up Dialog
    var selectedItem by remember { mutableStateOf<HistoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MedicalBlueLight)
    ) {
        // App Bar Navigasi Atas
        TopAppBar(
            title = { Text("Riwayat Pengecekan", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MedicalBluePrimary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        // Logika jika Database masih kosong
        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada riwayat pengecekan.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            // List Riwayat (LazyColumn)
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyList) { item ->
                    HistoryCard(item = item) {
                        // Saat card diklik, ubah selectedItem untuk memunculkan dialog
                        selectedItem = item
                    }
                }
            }
        }
    }

    // Menampilkan Pop-up Dialog jika ada item yang dipilih
    selectedItem?.let { item ->
        HistoryDetailDialog(item = item, onDismiss = { selectedItem = null })
    }
}

// Komponen Card untuk setiap baris riwayat
@Composable
fun HistoryCard(item: HistoryEntity, onClick: () -> Unit) {
    val statusColor = if (item.isAnaemia) StatusAnemiaRed else StatusNormalGreen
    val statusText = if (item.isAnaemia) "Indikasi Anemia" else "Normal"
    val statusIcon = if (item.isAnaemia) Icons.Default.Warning else Icons.Default.CheckCircle

    // Format Tanggal (Misal: 15 Agust 2024, 14:30)
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateString = dateFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Foto Mata
            Image(
                bitmap = item.image.asImageBitmap(),
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Teks Info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateString, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusText, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Ikon Status Kanan
            Icon(
                imageVector = statusIcon,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// Komponen Pop-up Dialog (Muncul saat riwayat disentuh)
@Composable
fun HistoryDetailDialog(item: HistoryEntity, onDismiss: () -> Unit) {
    val statusColor = if (item.isAnaemia) StatusAnemiaRed else StatusNormalGreen
    val statusText = if (item.isAnaemia) "Indikasi Anemia" else "Normal"

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateString = dateFormat.format(Date(item.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Detail Pengecekan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Menampilkan Foto Asli secara penuh
                Image(
                    bitmap = item.image.asImageBitmap(),
                    contentDescription = "Foto Pengecekan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = dateString, color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // Badge Status
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tombol Tutup Dialog
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
                ) {
                    Text("Tutup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}