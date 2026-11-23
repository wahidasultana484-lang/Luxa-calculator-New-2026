package com.example.history.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CalculationHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LuxaDatabase : RoomDatabase() {
    abstract fun calculationHistoryDao(): CalculationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LuxaDatabase? = null

        fun getInstance(context: Context): LuxaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LuxaDatabase::class.java,
                    "luxa_calculator.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
