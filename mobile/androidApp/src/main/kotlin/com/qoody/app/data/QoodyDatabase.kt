package com.qoody.app.data

import androidx.room3.AutoMigration
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["dedupeKey"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val payload: String,
    /** Set for captured payments so the same payment is stored once; `null` for manual entries. */
    val dedupeKey: String? = null,
)

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ID,
    val payload: String,
)

@Entity(
    tableName = "unparsed_captures",
    indices = [Index(value = ["dedupeKey"], unique = true)],
)
data class UnparsedCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val reason: String,
    val dedupeKey: String,
)

/** A category the user chose for a merchant, keyed by [com.qoody.shared.domain.model.MerchantKey]. */
@Entity(tableName = "merchant_categories")
data class MerchantCategoryEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observe(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Query("SELECT MAX(id) FROM transactions")
    suspend fun maxId(): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE dedupeKey = :dedupeKey)")
    suspend fun hasDedupeKey(dedupeKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface UnparsedCaptureDao {
    @Query("SELECT * FROM unparsed_captures ORDER BY postedAt DESC, id DESC")
    fun observeAll(): Flow<List<UnparsedCaptureEntity>>

    @Query("SELECT * FROM unparsed_captures")
    suspend fun getAll(): List<UnparsedCaptureEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM unparsed_captures WHERE dedupeKey = :dedupeKey)")
    suspend fun hasDedupeKey(dedupeKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(capture: UnparsedCaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(captures: List<UnparsedCaptureEntity>)

    /** Deletes everything except the [keep] most recent entries. */
    @Query(
        "DELETE FROM unparsed_captures WHERE id NOT IN " +
            "(SELECT id FROM unparsed_captures ORDER BY postedAt DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM unparsed_captures WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM unparsed_captures")
    suspend fun deleteAll()
}

@Dao
interface MerchantCategoryDao {
    @Query("SELECT * FROM merchant_categories WHERE merchantKey = :merchantKey")
    suspend fun get(merchantKey: String): MerchantCategoryEntity?

    @Query("SELECT * FROM merchant_categories")
    suspend fun getAll(): List<MerchantCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MerchantCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<MerchantCategoryEntity>)

    @Query("DELETE FROM merchant_categories")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = :id")
    fun observe(id: Int = SETTINGS_ID): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = :id")
    suspend fun get(id: Int = SETTINGS_ID): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}

@Database(
    entities = [
        TransactionEntity::class,
        SettingsEntity::class,
        UnparsedCaptureEntity::class,
        MerchantCategoryEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class QoodyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun settingsDao(): SettingsDao

    abstract fun unparsedCaptureDao(): UnparsedCaptureDao

    abstract fun merchantCategoryDao(): MerchantCategoryDao
}

const val DATABASE_VERSION = 3
const val SETTINGS_ID = 1
