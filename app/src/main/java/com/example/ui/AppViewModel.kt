package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    
    // DB Flows
    val vouchers: StateFlow<List<VoucherEntity>>
    val masterVouchers: StateFlow<List<MasterVoucherEntity>>
    val settingsFlow: StateFlow<SettingsEntity>

    // UI States
    private val _currentSession = MutableStateFlow("Morning")
    val currentSession: StateFlow<String> = _currentSession.asStateFlow()

    private val _pat19 = MutableStateFlow(true)
    val pat19: StateFlow<Boolean> = _pat19.asStateFlow()

    private val _rawInput = MutableStateFlow("")
    val rawInput: StateFlow<String> = _rawInput.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    private val _winNum = MutableStateFlow("")
    val winNum: StateFlow<String> = _winNum.asStateFlow()

    private val _modalVoucherText = MutableStateFlow<String?>(null)
    val modalVoucherText: StateFlow<String?> = _modalVoucherText.asStateFlow()

    private val parser = FormulaParser()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.dao())

        vouchers = repository.allVouchers.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        masterVouchers = repository.allMasterVouchers.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        // Set default settings if absent
        settingsFlow = repository.settingsFlow
            .filterNotNull()
            .stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5000),
                SettingsEntity(1, 200, 80, 10, "Morning, Evening")
            )

        // Sync initial session with the active list of settings
        viewModelScope.launch {
            repository.initializeSettingsIfNeeded()
            val savedSettings = repository.getSettings()
            val sessions = savedSettings.sessionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (sessions.isNotEmpty()) {
                _currentSession.value = sessions[0]
            }
        }
    }

    fun setSession(session: String) {
        _currentSession.value = session
    }

    fun setPat19(value: Boolean) {
        _pat19.value = value
    }

    fun setRawInput(value: String) {
        _rawInput.value = value
    }

    fun setWinNum(value: String) {
        if (value.length <= 2) {
            _winNum.value = value
        }
    }

    fun showModal(voucherId: Long, isMaster: Boolean) {
        viewModelScope.launch {
            val voucher = if (isMaster) {
                val mv = masterVouchers.value.find { it.id == voucherId }
                if (mv != null) {
                    val items = CustomJsonHelper.fromJson(mv.itemsJson)
                    val formatted = items.sortedBy { it.num }.joinToString("\n") { "${it.num}=${it.amt}" }
                    val total = items.sumOf { it.amt }
                    "မာစတာဘောင်ချာ (${mv.timeStr})\n----------\n$formatted\n----------\nTotal: ${String.format("%,d", total)} Ks"
                } else null
            } else {
                val v = vouchers.value.find { it.id == voucherId }
                if (v != null) {
                    val items = CustomJsonHelper.fromJson(v.itemsJson)
                    val formatted = items.sortedBy { it.num }.joinToString("\n") { "${it.num}=${it.amt}" }
                    val total = items.sumOf { it.amt }
                    "ဘောင်ချာ (${v.timeStr})\n----------\n$formatted\n----------\nTotal: ${String.format("%,d", total)} Ks"
                } else null
            }
            _modalVoucherText.value = voucher
        }
    }

    fun dismissModal() {
        _modalVoucherText.value = null
    }

    fun processEntry() {
        val raw = _rawInput.value.trim()
        val session = _currentSession.value
        val use19 = _pat19.value
        if (raw.isEmpty()) return

        _errorText.value = null
        val lines = raw.split('\n')
        val allItems = mutableListOf<VoucherItem>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            if (!parser.isValidInput(line)) {
                _errorText.value = "⚠️ ရိုက်တာမှားနေပါတယ် ပြင်ဆင်ပေးပါ!\nစာကြောင်းနံပါတ် (${i + 1}): \"$line\""
                return
            }
            allItems.addAll(parser.parseLine(line, use19))
        }

        if (allItems.isNotEmpty()) {
            viewModelScope.launch {
                val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                repository.insertVoucher(
                    timestamp = System.currentTimeMillis(),
                    timeStr = timeStr,
                    session = session,
                    rawText = raw,
                    items = allItems
                )
                _rawInput.value = ""
            }
        }
    }

    fun deleteVoucher(id: Long) {
        viewModelScope.launch {
            repository.deleteVoucherById(id)
        }
    }

    fun editVoucher(id: Long) {
        val v = vouchers.value.find { it.id == id }
        if (v != null) {
            _rawInput.value = v.rawText
            viewModelScope.launch {
                repository.deleteVoucherById(id)
            }
        }
    }

    fun deleteMasterVoucher(id: Long) {
        viewModelScope.launch {
            val mv = masterVouchers.value.find { it.id == id }
            if (mv != null) {
                val items = CustomJsonHelper.fromJson(mv.itemsJson)
                val timeStr = SimpleDateFormat("hh:mm:ss a (Res)", Locale.getDefault()).format(Date())
                // Restore back to self vouchers
                repository.insertVoucher(
                    timestamp = System.currentTimeMillis(),
                    timeStr = timeStr,
                    session = mv.session,
                    rawText = "Restored",
                    items = items
                )
                repository.deleteMasterVoucherById(id)
            }
        }
    }

    fun sendToMaster(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val session = _currentSession.value
        val limit = settingsFlow.value.limitPrice

        // Aggregate current self totals for session
        val sessionSelfVouchers = vouchers.value.filter { it.session == session }
        val totals = mutableMapOf<String, Int>()
        sessionSelfVouchers.forEach { v ->
            CustomJsonHelper.fromJson(v.itemsJson).forEach { item ->
                totals[item.num] = (totals[item.num] ?: 0) + item.amt
            }
        }

        val overLimitItems = mutableListOf<VoucherItem>()
        totals.forEach { (num, amt) ->
            val over = amt - limit
            if (over > 0) {
                overLimitItems.add(VoucherItem(num, over))
            }
        }

        if (overLimitItems.isEmpty()) {
            onError("Limit ကျော်မရှိပါ။")
            return
        }

        viewModelScope.launch {
            val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            // Insert Master Voucher
            repository.insertMasterVoucher(
                timestamp = System.currentTimeMillis(),
                timeStr = timeStr,
                session = session,
                items = overLimitItems
            )

            // Subtract from original vouchers (walk backward, newest first)
            overLimitItems.forEach { mItem ->
                var leftBytes = mItem.amt
                for (v in sessionSelfVouchers) {
                    if (leftBytes <= 0) break
                    val items = CustomJsonHelper.fromJson(v.itemsJson).toMutableList()
                    val matchIndex = items.indexOfFirst { it.num == mItem.num }
                    if (matchIndex != -1) {
                        val originalItem = items[matchIndex]
                        val sub = minOf(originalItem.amt, leftBytes)
                        val newAmt = originalItem.amt - sub
                        if (newAmt > 0) {
                            items[matchIndex] = VoucherItem(mItem.num, newAmt)
                        } else {
                            items.removeAt(matchIndex)
                        }
                        leftBytes -= sub

                        // Update or delete
                        if (items.isEmpty()) {
                            repository.deleteVoucherById(v.id)
                        } else {
                            // Update voucher entity items
                            val updatedJson = CustomJsonHelper.toJson(items)
                            val updatedEntity = v.copy(itemsJson = updatedJson)
                            repository.updateVoucherEntity(updatedEntity)
                        }
                    }
                }
            }
            onSuccess()
        }
    }

    fun updateSettings(limit: Int, payout: Int, comm: Int, sessions: String) {
        viewModelScope.launch {
            repository.updateSettings(limit, payout, comm, sessions)
            // If current selected session is no longer in sessions list, switch it
            val list = sessions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (list.isNotEmpty() && !list.contains(_currentSession.value)) {
                _currentSession.value = list[0]
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _rawInput.value = ""
            _winNum.value = ""
            _errorText.value = null
            // Switch current session back to local default
            val savedSettings = repository.getSettings()
            val sessions = savedSettings.sessionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (sessions.isNotEmpty()) {
                _currentSession.value = sessions[0]
            }
        }
    }

    fun generateBackupString(): String {
        return exportBackupJson(
            vouchers = vouchers.value,
            masterVouchers = masterVouchers.value,
            settings = settingsFlow.value
        )
    }

    fun restoreBackupString(backupJson: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Parse settings limit
                val limit = Regex("""limit"\s*:\s*(\d+)""").find(backupJson)?.groupValues?.get(1)?.toIntOrNull() ?: 200
                val payout = Regex("""payout"\s*:\s*(\d+)""").find(backupJson)?.groupValues?.get(1)?.toIntOrNull() ?: 80
                val comm = Regex("""comm"\s*:\s*(\d+)""").find(backupJson)?.groupValues?.get(1)?.toIntOrNull() ?: 10
                val sessionsMatch = Regex("""sessions"\s*:\s*\[([^]]+)\]""").find(backupJson)?.groupValues?.get(1)
                val sessionsStr = if (sessionsMatch != null) {
                    sessionsMatch.replace("\"", "").replace("'", "").trim()
                } else {
                    "Morning, Evening"
                }

                // Temporary clear database values
                repository.clearAllData()
                repository.updateSettings(limit, payout, comm, sessionsStr)

                // Retrieve and insert self vouchers
                val selfVouchersList = mutableListOf<VoucherEntity>()
                val voucherBlocks = Regex("""\{\s*"id"\s*:\s*(\d+)\s*,\s*"time"\s*:\s*"([^"]+)"\s*,\s*"session"\s*:\s*"([^"]+)"\s*,\s*"raw"\s*:\s*"((?:[^"\\]|\\.)*)"\s*,\s*"items"\s*:\s*(\[[^]]*\])\s*\}""").findAll(backupJson)
                for (block in voucherBlocks) {
                    val id = block.groupValues[1].toLongOrNull() ?: System.currentTimeMillis()
                    val time = block.groupValues[2]
                    val session = block.groupValues[3]
                    val raw = block.groupValues[4].replace("\\\"", "\"").replace("\\n", "\n")
                    val itemsJson = block.groupValues[5]
                    selfVouchersList.add(
                        VoucherEntity(
                            id = id,
                            timestamp = id,
                            timeStr = time,
                            session = session,
                            rawText = raw,
                            itemsJson = itemsJson
                        )
                    )
                }

                if (selfVouchersList.isNotEmpty()) {
                    repository.insertVouchers(selfVouchersList)
                }

                // Retrieve and insert master vouchers
                val masterVouchersList = mutableListOf<MasterVoucherEntity>()
                val masterBlocks = Regex("""\{\s*"id"\s*:\s*(\d+)\s*,\s*"time"\s*:\s*"([^"]+)"\s*,\s*"session"\s*:\s*"([^"]+)"\s*,\s*"items"\s*:\s*(\[[^]]*\])\s*\}""").findAll(backupJson)
                for (block in masterBlocks) {
                    val id = block.groupValues[1].toLongOrNull() ?: System.currentTimeMillis()
                    val time = block.groupValues[2]
                    val session = block.groupValues[3]
                    val itemsJson = block.groupValues[4]
                    masterVouchersList.add(
                        MasterVoucherEntity(
                            id = id,
                            timestamp = id,
                            timeStr = time,
                            session = session,
                            itemsJson = itemsJson
                        )
                    )
                }

                if (masterVouchersList.isNotEmpty()) {
                    repository.insertMasterVouchers(masterVouchersList)
                }

                // Sync UI state
                val sessionsArray = sessionsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (sessionsArray.isNotEmpty()) {
                    _currentSession.value = sessionsArray[0]
                }
                onSuccess()
            } catch (e: Exception) {
                onError("မှန်ကန်သော Backup File မဟုတ်ပါ။")
            }
        }
    }

    private fun exportBackupJson(
        vouchers: List<VoucherEntity>,
        masterVouchers: List<MasterVoucherEntity>,
        settings: SettingsEntity
    ): String {
        val builder = StringBuilder()
        builder.append("{\n")
        
        // Settings
        builder.append("  \"settings\": {\n")
        builder.append("    \"limit\": ${settings.limitPrice},\n")
        builder.append("    \"payout\": ${settings.payoutMultiplier},\n")
        builder.append("    \"comm\": ${settings.commissionPercentage},\n")
        val sessionsArray = settings.sessionsCsv.split(",").map { "\"${it.trim()}\"" }.joinToString(", ")
        builder.append("    \"sessions\": [$sessionsArray]\n")
        builder.append("  },\n")
        
        // Vouchers
        builder.append("  \"vouchers\": [\n")
        val voucherStrings = vouchers.map { v ->
            val cleanRaw = v.rawText.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
            """    {
      "id": ${v.id},
      "time": "${v.timeStr}",
      "session": "${v.session}",
      "raw": "$cleanRaw",
      "items": ${v.itemsJson}
    }"""
        }
        builder.append(voucherStrings.joinToString(",\n"))
        builder.append("\n  ],\n")
        
        // MasterVouchers
        builder.append("  \"masterVouchers\": [\n")
        val masterStrings = masterVouchers.map { m ->
            """    {
      "id": ${m.id},
      "time": "${m.timeStr}",
      "session": "${m.session}",
      "items": ${m.itemsJson}
    }"""
        }
        builder.append(masterStrings.joinToString(",\n"))
        builder.append("\n  ]\n")
        builder.append("}")
        return builder.toString()
    }
}
