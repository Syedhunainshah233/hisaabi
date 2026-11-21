package com.syedhunain.hisaabi.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One ledger (khata) — a person or business the user deals with. */
@Entity(tableName = "khata")
data class Khata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: String = TYPE_CUSTOMER,
    val address: String = "",
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val TYPE_CUSTOMER = "CUSTOMER"
        const val TYPE_SUPPLIER = "SUPPLIER"
        const val TYPE_FRIEND = "FRIEND"
        const val TYPE_PERSONAL = "PERSONAL"
    }
}

/**
 * One money movement in a khata.
 * gave = true  -> "Diya": user gave money, so the other side owes more (balance goes up).
 * gave = false -> "Liya": user received money (balance goes down).
 * Amount is whole rupees.
 */
@Entity(tableName = "txn", indices = [Index("khataId")])
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val khataId: Long,
    val amount: Long,
    val gave: Boolean,
    val note: String = "",
    val date: Long,
    val isOpening: Boolean = false,
)

@Dao
abstract class HisaabDao {
    @Query("SELECT * FROM khata")
    abstract fun khatas(): Flow<List<Khata>>

    @Query("SELECT * FROM khata WHERE id = :id")
    abstract suspend fun khata(id: Long): Khata?

    @Query("SELECT * FROM txn ORDER BY date DESC, id DESC")
    abstract fun allTxns(): Flow<List<Txn>>

    @Query("SELECT * FROM txn WHERE khataId = :khataId ORDER BY date DESC, id DESC")
    abstract fun txnsFor(khataId: Long): Flow<List<Txn>>

    @Query("SELECT * FROM txn WHERE khataId = :khataId ORDER BY date ASC, id ASC")
    abstract suspend fun txnsForOnce(khataId: Long): List<Txn>

    @Query("SELECT * FROM txn ORDER BY date ASC, id ASC")
    abstract suspend fun allTxnsOnce(): List<Txn>

    @Query("SELECT * FROM khata")
    abstract suspend fun khatasOnce(): List<Khata>

    @Insert
    abstract suspend fun insertKhata(k: Khata): Long

    @Update
    abstract suspend fun updateKhata(k: Khata)

    @Delete
    abstract suspend fun deleteKhataRow(k: Khata)

    @Query("DELETE FROM txn WHERE khataId = :khataId")
    abstract suspend fun deleteTxnsFor(khataId: Long)

    @Transaction
    open suspend fun deleteKhata(k: Khata) {
        deleteTxnsFor(k.id)
        deleteKhataRow(k)
    }

    @Insert
    abstract suspend fun insertTxn(t: Txn): Long

    @Update
    abstract suspend fun updateTxn(t: Txn)

    @Delete
    abstract suspend fun deleteTxn(t: Txn)

    @Query("UPDATE khata SET updatedAt = :time WHERE id = :id")
    abstract suspend fun touch(id: Long, time: Long)
}

@Database(entities = [Khata::class, Txn::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): HisaabDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisaabi.db"
                ).build().also { instance = it }
            }
    }
}
