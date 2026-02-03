package me.siddheshkothadi.codexdroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ThreadEntity::class], version = 4, exportSchema = false)
abstract class CodexDroidDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
}
