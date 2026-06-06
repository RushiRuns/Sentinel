package com.rushi.sentinel.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rushi.sentinel.data.db.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY isFavorite DESC, name ASC")
    fun getEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<EntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: EntryEntity): Long

    @Update
    fun updateEntry(entry: EntryEntity): Int

    @Delete
    fun deleteEntry(entry: EntryEntity): Int

    @Query("SELECT * FROM entries WHERE name LIKE :query || '%' OR username LIKE :query || '%' OR url LIKE :query || '%' ORDER BY isFavorite DESC, name ASC")
    fun searchEntries(query: String): Flow<List<EntryEntity>>
}
