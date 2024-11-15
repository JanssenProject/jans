package com.example.fido2.repository.settings

import com.example.fido2.database.local.LocalDataSource

class AllDataRepositoryImpl(private val localDataSource: LocalDataSource): AllDataRepository {
    override suspend fun clearAll() {
        localDataSource.clearAll()
    }
}