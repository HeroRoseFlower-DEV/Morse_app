package com.example.domain.repository

import com.example.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun insertHistory(item: HistoryItem)
    suspend fun deleteHistory(item: HistoryItem)
    suspend fun clearAll()
}
