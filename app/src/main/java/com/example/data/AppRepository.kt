package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {
    val allVouchers: Flow<List<VoucherEntity>> = dao.getAllVouchers()
    val allMasterVouchers: Flow<List<MasterVoucherEntity>> = dao.getAllMasterVouchers()
    val settingsFlow: Flow<SettingsEntity?> = dao.getSettingsFlow()

    suspend fun initializeSettingsIfNeeded() {
        if (dao.getSettingsCount() == 0) {
            dao.insertSettings(
                SettingsEntity(
                    id = 1,
                    limitPrice = 200,
                    payoutMultiplier = 80,
                    commissionPercentage = 10,
                    sessionsCsv = "Morning, Evening"
                )
            )
        }
    }

    suspend fun updateSettings(limit: Int, payout: Int, comm: Int, sessions: String) {
        dao.insertSettings(
            SettingsEntity(
                id = 1,
                limitPrice = limit,
                payoutMultiplier = payout,
                commissionPercentage = comm,
                sessionsCsv = sessions
            )
        )
    }

    suspend fun getSettings(): SettingsEntity {
        initializeSettingsIfNeeded()
        return dao.getSettings() ?: SettingsEntity(1, 200, 80, 10, "Morning, Evening")
    }

    suspend fun insertVoucher(timestamp: Long, timeStr: String, session: String, rawText: String, items: List<VoucherItem>) {
        val json = CustomJsonHelper.toJson(items)
        dao.insertVoucher(
            VoucherEntity(
                timestamp = timestamp,
                timeStr = timeStr,
                session = session,
                rawText = rawText,
                itemsJson = json
            )
        )
    }

    suspend fun insertVouchers(vouchers: List<VoucherEntity>) {
        vouchers.forEach {
            dao.insertVoucher(it)
        }
    }

    suspend fun updateVoucherEntity(entity: VoucherEntity) {
        dao.insertVoucher(entity)
    }

    suspend fun deleteVoucherById(id: Long) {
        dao.deleteVoucherById(id)
    }

    suspend fun insertMasterVoucher(timestamp: Long, timeStr: String, session: String, items: List<VoucherItem>) {
        val json = CustomJsonHelper.toJson(items)
        dao.insertMasterVoucher(
            MasterVoucherEntity(
                timestamp = timestamp,
                timeStr = timeStr,
                session = session,
                itemsJson = json
            )
        )
    }

    suspend fun insertMasterVouchers(masterVouchers: List<MasterVoucherEntity>) {
        masterVouchers.forEach {
            dao.insertMasterVoucher(it)
        }
    }

    suspend fun deleteMasterVoucherById(id: Long) {
        dao.deleteMasterVoucherById(id)
    }

    suspend fun clearAllData() {
        dao.deleteAllVouchers()
        dao.deleteAllMasterVouchers()
        dao.insertSettings(
            SettingsEntity(
                id = 1,
                limitPrice = 200,
                payoutMultiplier = 80,
                commissionPercentage = 10,
                sessionsCsv = "Morning, Evening"
            )
        )
    }
}
