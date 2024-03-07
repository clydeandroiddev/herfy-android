package com.herfy.repository

import com.herfy.model.auth.LoginResponse
import javax.inject.Inject

interface StoreRepository {

    suspend fun loadStores(phoneNumber: String, result: (LoginResponse) -> Unit)
}

class StoreRepositoryImpl @Inject constructor(
) : StoreRepository {
    override suspend fun loadStores(phoneNumber: String, result: (LoginResponse) -> Unit) {

    }

}