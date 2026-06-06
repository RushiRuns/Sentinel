package com.rushi.sentinel.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rushi.sentinel.data.db.entity.CategoryEntity
import com.rushi.sentinel.data.db.entity.EntryEntity

import com.rushi.sentinel.data.db.dao.EntryDao

@Database(
    entities = [EntryEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
