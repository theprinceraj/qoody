package com.qoody.app.data

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @androidx.room3.PrimaryKey val id: Long,
    val payload: String,
)

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @androidx.room3.PrimaryKey val id: Int = SETTINGS_ID,
    val payload: String,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observe(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAllNow(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("DELETE FROM transactions")
    fun deleteAllNow()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = :id")
    fun observe(id: Int = SETTINGS_ID): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = :id")
    suspend fun get(id: Int = SETTINGS_ID): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertNow(settings: SettingsEntity)
}

@Database(
    entities = [TransactionEntity::class, SettingsEntity::class],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class QoodyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun settingsDao(): SettingsDao
}

private const val DATABASE_VERSION = 1
const val SETTINGS_ID = 1
