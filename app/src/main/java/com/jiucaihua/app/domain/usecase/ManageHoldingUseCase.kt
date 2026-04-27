package com.jiucaihua.app.domain.usecase

import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.repository.HoldingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageHoldingUseCase @Inject constructor(
    private val holdingRepository: HoldingRepository
) {
    fun getActiveHoldings(): Flow<List<Holding>> {
        return holdingRepository.getActiveHoldings()
    }

    suspend fun getHoldingById(id: Long): Holding? {
        return holdingRepository.getHoldingById(id)
    }

    suspend fun addHolding(holding: Holding): Long {
        return holdingRepository.addHolding(holding)
    }

    suspend fun updateHolding(holding: Holding) {
        holdingRepository.updateHolding(holding)
    }

    suspend fun deleteHolding(id: Long) {
        holdingRepository.deleteHolding(id)
    }
}
