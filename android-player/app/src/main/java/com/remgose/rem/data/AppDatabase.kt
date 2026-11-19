package com.remgose.rem.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class, RuntimeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun runtimeDao(): RuntimeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "remgose_database").build()
                .also { instance = it }
        }
    }
}
