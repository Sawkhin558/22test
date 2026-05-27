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
                    sessionsCsv = "Morning, Evening",
                    trialStartDate = System.currentTimeMillis(),
                    isActivated = false,
                    activationCode = "",
                    firebaseUrl = "https://twodsmartpro-eb96d-default-rtdb.firebaseio.com"
                )
            )
        } else {
            val s = dao.getSettings()
            if (s != null && s.trialStartDate == 0L) {
                dao.insertSettings(s.copy(trialStartDate = System.currentTimeMillis()))
            }
        }
    }

    suspend fun updateSettings(limit: Int, payout: Int, comm: Int, sessions: String) {
        val current = dao.getSettings() ?: SettingsEntity(
            id = 1,
            limitPrice = limit,
            payoutMultiplier = payout,
            commissionPercentage = comm,
            sessionsCsv = sessions,
            trialStartDate = System.currentTimeMillis()
        )
        dao.insertSettings(
            current.copy(
                limitPrice = limit,
                payoutMultiplier = payout,
                commissionPercentage = comm,
                sessionsCsv = sessions
            )
        )
    }

    suspend fun updateActivation(isActivated: Boolean, activationCode: String) {
        val current = getSettings()
        dao.insertSettings(
            current.copy(
                isActivated = isActivated,
                activationCode = activationCode
            )
        )
    }

    suspend fun updateFirebaseUrl(url: String) {
        val current = getSettings()
        dao.insertSettings(
            current.copy(
                firebaseUrl = url
            )
        )
    }

    suspend fun getSettings(): SettingsEntity {
        initializeSettingsIfNeeded()
        return dao.getSettings() ?: SettingsEntity(
            id = 1, 
            limitPrice = 200, 
            payoutMultiplier = 80, 
            commissionPercentage = 10, 
            sessionsCsv = "Morning, Evening", 
            trialStartDate = System.currentTimeMillis()
        )
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
        dao.insertVouchers(vouchers)
    }

    suspend fun updateVoucherEntity(entity: VoucherEntity) {
        dao.insertVoucher(entity)
    }

    suspend fun deleteVoucherById(id: Long) {
        dao.deleteVoucherById(id)
    }

    suspend fun sendToMasterTransaction(
        masterVoucher: MasterVoucherEntity,
        vouchersToUpdate: List<VoucherEntity>,
        voucherIdsToDelete: List<Long>
    ) {
        dao.sendToMasterTransaction(masterVoucher, vouchersToUpdate, voucherIdsToDelete)
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
        val current = dao.getSettings() ?: SettingsEntity(
            id = 1,
            limitPrice = 200,
            payoutMultiplier = 80,
            commissionPercentage = 10,
            sessionsCsv = "Morning, Evening",
            trialStartDate = System.currentTimeMillis()
        )
        dao.insertSettings(
            current.copy(
                limitPrice = 200,
                payoutMultiplier = 80,
                commissionPercentage = 10,
                sessionsCsv = "Morning, Evening"
            )
        )
    }
}
