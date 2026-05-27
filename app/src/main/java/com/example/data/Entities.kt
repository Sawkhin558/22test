package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val timeStr: String,
    val session: String,
    val rawText: String,
    val itemsJson: String
)

@Entity(tableName = "master_vouchers")
data class MasterVoucherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val timeStr: String,
    val session: String,
    val itemsJson: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val limitPrice: Int,
    val payoutMultiplier: Int,
    val commissionPercentage: Int,
    val sessionsCsv: String
)
