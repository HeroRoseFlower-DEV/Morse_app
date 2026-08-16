package com.example.data.repository

import com.example.data.local.HistoryDao
import com.example.domain.model.HistoryItem
import com.example.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(private val historyDao: HistoryDao) : HistoryRepository {
    override fun getAllHistory(): Flow<List<HistoryItem>> = historyDao.getAllHistory()
    override suspend fun insertHistory(item: HistoryItem) = historyDao.insertHistory(item)
    override suspend fun deleteHistory(item: HistoryItem) = historyDao.deleteHistory(item)
    override suspend fun clearAll() = historyDao.clearAll()
}
