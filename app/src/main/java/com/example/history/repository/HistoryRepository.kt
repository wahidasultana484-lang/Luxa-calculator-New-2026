package com.example.history.repository

import com.example.history.database.CalculationHistoryDao
import com.example.history.database.CalculationHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: CalculationHistoryDao) {

    val allHistory: Flow<List<CalculationHistoryEntity>> = dao.getAllHistory()

    val historyCount: Flow<Int> = dao.getHistoryCount()

    suspend fun saveCalculation(
        expression: String,
        result: String,
        formattedResult: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        return dao.insert(
            CalculationHistoryEntity(
                expression = expression,
                result = result,
                formattedResult = formattedResult,
                timestamp = timestamp
            )
        )
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
