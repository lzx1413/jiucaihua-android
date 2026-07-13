package com.jiucaihua.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["code", "marketType"]),
        Index(value = ["tradeDate"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String? = null,
    val name: String? = null,
    val marketType: String? = null,
    val type: String,
    val tradeDate: Long,
    val quantity: Double = 0.0,
    val price: Double = 0.0,
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val tax: Double = 0.0,
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
