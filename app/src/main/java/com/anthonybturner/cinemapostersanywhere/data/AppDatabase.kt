package com.anthonybturner.cinemapostersanywhere.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anthonybturner.cinemapostersanywhere.Models.Movie
import com.anthonybturner.cinemapostersanywhere.utilities.Converters

@Database(entities = [Movie::class], version = 8)  // Make sure the version matches your current schema version
@TypeConverters(Converters::class)  // Correct syntax for Kotlin
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton to prevent multiple instances of the database being created
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "movie_database"
                )
                    .fallbackToDestructiveMigration()  // This will destroy and recreate the database when version changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
