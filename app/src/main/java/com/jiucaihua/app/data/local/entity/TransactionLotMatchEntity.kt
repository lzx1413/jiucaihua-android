package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transaction_lot_matches",
    indices = [
        Index(value = ["code", "marketType"]),
        Index(value = ["sellTransactionId"]),
        Index(value = ["buyTransactionId"]),
    ],
)
data class TransactionLotMatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val marketType: String,
    val sellTransactionId: Long,
    val buyTransactionId: Long,
    val quantity: Double,
    val buyUnitCostCny: Double,
    val sellUnitProceedsCny: Double,
    val costBasisCny: Double,
    val proceedsCny: Double,
    val realizedPnlCny: Double,
    val createdAt: Long = System.currentTimeMillis(),
)
