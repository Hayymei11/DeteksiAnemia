package com.example.anemiacheck.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream

// 1. ENTITAS (Struktur Tabel Database)
@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val image: Bitmap,           // Akan dikonversi otomatis oleh Room
    val isAnaemia: Boolean,      // True jika Anemia, False jika Normal
    val timestamp: Long          // Waktu pengecekan
)

// 2. KONVERTER (Agar Room bisa menyimpan Gambar Bitmap)
class Converters {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // Menggunakan kompresi JPEG 70% agar database ringan dan tidak lag
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}

// 3. DAO (Perintah untuk Memasukkan dan Mengambil Data)
@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    // Mengambil semua riwayat dan diurutkan dari yang paling baru
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
}

// 4. DATABASE UTAMA
@Database(entities = [HistoryEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anemiacheck_database"
                )
                .fallbackToDestructiveMigration() // Menghapus data lama yang bikin crash
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
