package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM vouchers ORDER BY timestamp DESC")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(vouchers: List<VoucherEntity>)

    @Delete
    suspend fun deleteVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE id = :id")
    suspend fun deleteVoucherById(id: Long)

    @Query("DELETE FROM vouchers WHERE id IN (:ids)")
    suspend fun deleteVouchersByIds(ids: List<Long>)

    @Query("DELETE FROM vouchers")
    suspend fun deleteAllVouchers()

    // Master Vouchers
    @Query("SELECT * FROM master_vouchers ORDER BY timestamp DESC")
    fun getAllMasterVouchers(): Flow<List<MasterVoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterVoucher(masterVoucher: MasterVoucherEntity): Long

    @Delete
    suspend fun deleteMasterVoucher(masterVoucher: MasterVoucherEntity)

    @Query("DELETE FROM master_vouchers WHERE id = :id")
    suspend fun deleteMasterVoucherById(id: Long)

    @Query("DELETE FROM master_vouchers")
    suspend fun deleteAllMasterVouchers()

    // Settings
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT COUNT(*) FROM settings WHERE id = 1")
    suspend fun getSettingsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Transaction
    suspend fun sendToMasterTransaction(
        masterVoucher: MasterVoucherEntity,
        vouchersToUpdate: List<VoucherEntity>,
        voucherIdsToDelete: List<Long>
    ) {
        insertMasterVoucher(masterVoucher)
        if (vouchersToUpdate.isNotEmpty()) {
            insertVouchers(vouchersToUpdate)
        }
        if (voucherIdsToDelete.isNotEmpty()) {
            deleteVouchersByIds(voucherIdsToDelete)
        }
    }
}
