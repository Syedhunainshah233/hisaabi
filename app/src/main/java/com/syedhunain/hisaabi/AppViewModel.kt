package com.syedhunain.hisaabi

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syedhunain.hisaabi.data.AppDatabase
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.data.Prefs
import com.syedhunain.hisaabi.data.Txn
import com.syedhunain.hisaabi.util.BackupInfo
import com.syedhunain.hisaabi.util.BackupJson
import com.syedhunain.hisaabi.util.GoogleDrive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A khata plus its live balance. balance > 0: user will get; < 0: user will give. */
data class KhataItem(val khata: Khata, val balance: Long, val lastActivity: Long, val count: Int)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    val prefs = Prefs(app)

    /** True once the PIN has been entered in this session. */
    var unlocked by mutableStateOf(false)
    var lastStoppedAt: Long = 0L

    val txns: StateFlow<List<Txn>> =
        dao.allTxns().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val khatas: StateFlow<List<KhataItem>> =
        combine(dao.khatas(), dao.allTxns()) { ks, ts ->
            val byKhata = ts.groupBy { it.khataId }
            ks.map { k ->
                val list = byKhata[k.id].orEmpty()
                KhataItem(
                    khata = k,
                    balance = list.sumOf { if (it.gave) it.amount else -it.amount },
                    lastActivity = maxOf(k.updatedAt, list.maxOfOrNull { it.date } ?: 0L),
                    count = list.size,
                )
            }.sortedByDescending { it.lastActivity }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- Google Drive backup ----------

    enum class BackupResult { OK, NO_BACKUP, ERROR }

    /** True while a sign-in, backup or restore is running. */
    var cloudBusy by mutableStateOf(false)
        private set

    /** A backup found in Drive that the user may restore (shown as a dialog). */
    var restoreOffer by mutableStateOf<BackupInfo?>(null)
        private set
    private var offerToken: String? = null

    /** After Google sign-in: save the account and look for an existing backup. */
    fun onSignedIn(token: String, lookForBackup: Boolean, onDone: (BackupResult) -> Unit) {
        viewModelScope.launch {
            cloudBusy = true
            try {
                val (email, name) = GoogleDrive.userInfo(token)
                prefs.setGoogleAccount(email, name)
                if (lookForBackup) {
                    val info = GoogleDrive.findBackup(token)
                    if (info != null) {
                        offerToken = token
                        restoreOffer = info
                    }
                }
                onDone(BackupResult.OK)
            } catch (e: Exception) {
                onDone(BackupResult.ERROR)
            } finally {
                cloudBusy = false
            }
        }
    }

    fun backupNow(token: String, onDone: (BackupResult) -> Unit) {
        viewModelScope.launch {
            cloudBusy = true
            try {
                val json = BackupJson.encode(dao.khatasOnce(), dao.allTxnsOnce())
                GoogleDrive.upload(token, json)
                prefs.markBackup(System.currentTimeMillis())
                onDone(BackupResult.OK)
            } catch (e: Exception) {
                onDone(BackupResult.ERROR)
            } finally {
                cloudBusy = false
            }
        }
    }

    /** Quiet daily backup (no UI). */
    suspend fun backupSilently(token: String) {
        runCatching {
            val json = BackupJson.encode(dao.khatasOnce(), dao.allTxnsOnce())
            GoogleDrive.upload(token, json)
            prefs.markBackup(System.currentTimeMillis())
        }
    }

    /** "Restore" button: find the backup, then ask the user to confirm. */
    fun findBackupToRestore(token: String, onDone: (BackupResult) -> Unit) {
        viewModelScope.launch {
            cloudBusy = true
            try {
                val info = GoogleDrive.findBackup(token)
                if (info == null) {
                    onDone(BackupResult.NO_BACKUP)
                } else {
                    offerToken = token
                    restoreOffer = info
                    onDone(BackupResult.OK)
                }
            } catch (e: Exception) {
                onDone(BackupResult.ERROR)
            } finally {
                cloudBusy = false
            }
        }
    }

    fun dismissRestore() {
        restoreOffer = null
        offerToken = null
    }

    fun confirmRestore(onDone: (BackupResult) -> Unit) {
        val info = restoreOffer ?: return
        val token = offerToken ?: return
        restoreOffer = null
        viewModelScope.launch {
            cloudBusy = true
            try {
                val (ks, ts) = BackupJson.decode(GoogleDrive.download(token, info.id))
                dao.replaceAll(ks, ts)
                onDone(BackupResult.OK)
            } catch (e: Exception) {
                onDone(BackupResult.ERROR)
            } finally {
                offerToken = null
                cloudBusy = false
            }
        }
    }

    fun signOut() {
        prefs.clearGoogleAccount()
    }

    fun txnsFor(khataId: Long): Flow<List<Txn>> = dao.txnsFor(khataId)

    suspend fun txnsForOnce(khataId: Long): List<Txn> = dao.txnsForOnce(khataId)
    suspend fun allTxnsOnce(): List<Txn> = dao.allTxnsOnce()
    suspend fun khatasOnce(): List<Khata> = dao.khatasOnce()

    fun saveKhata(
        id: Long?,
        name: String,
        phone: String,
        type: String,
        address: String,
        note: String,
        opening: Long,
        openingGave: Boolean,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == null) {
                val newId = dao.insertKhata(
                    Khata(
                        name = name.trim(), phone = phone.trim(), type = type,
                        address = address.trim(), note = note.trim(),
                        createdAt = now, updatedAt = now,
                    )
                )
                if (opening > 0) {
                    dao.insertTxn(
                        Txn(khataId = newId, amount = opening, gave = openingGave, date = now, isOpening = true)
                    )
                }
                onDone(newId)
            } else {
                val old = dao.khata(id) ?: return@launch
                dao.updateKhata(
                    old.copy(
                        name = name.trim(), phone = phone.trim(), type = type,
                        address = address.trim(), note = note.trim(), updatedAt = now,
                    )
                )
                onDone(id)
            }
        }
    }

    fun deleteKhata(k: Khata, onDone: () -> Unit) {
        viewModelScope.launch {
            dao.deleteKhata(k)
            onDone()
        }
    }

    fun saveTxn(existing: Txn?, khataId: Long, amount: Long, gave: Boolean, note: String, date: Long) {
        viewModelScope.launch {
            if (existing == null) {
                dao.insertTxn(Txn(khataId = khataId, amount = amount, gave = gave, note = note.trim(), date = date))
            } else {
                dao.updateTxn(existing.copy(amount = amount, gave = gave, note = note.trim(), date = date))
            }
            dao.touch(khataId, System.currentTimeMillis())
        }
    }

    fun deleteTxn(t: Txn) {
        viewModelScope.launch { dao.deleteTxn(t) }
    }
}
