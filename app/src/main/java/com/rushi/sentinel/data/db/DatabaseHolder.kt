package com.rushi.sentinel.data.db

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseHolder @Inject constructor(
    private val context: Context
) {
    private var database: SentinelDatabase? = null

    /**
     * Retrieves the active database instance.
     * Throws IllegalStateException if the database is not open (i.e. vault is locked).
     */
    @Synchronized
    fun getDatabase(): SentinelDatabase {
        return database ?: throw IllegalStateException("Vault is locked or database not initialized")
    }

    /**
     * Dynamic initialization: Opens the SQLCipher-encrypted database using the derived passphrase.
     */
    @Synchronized
    fun openDatabase(passphrase: ByteArray) {
        if (database == null) {
            val factory = SupportOpenHelperFactory(passphrase)
            database = Room.databaseBuilder(
                context.applicationContext,
                SentinelDatabase::class.java,
                "sentinel.db"
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }

    /**
     * Safely closes the database and clears the instance from memory.
     */
    @Synchronized
    fun closeDatabase() {
        database?.close()
        database = null
    }

    /**
     * Checks if the database is open.
     */
    @Synchronized
    fun isOpened(): Boolean {
        return database != null
    }
}
