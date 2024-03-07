package com.herfy.repository

import com.herfy.model.auth.LoginResponse

interface StoreRepository {

    suspend fun loadStores(phoneNumber: String, result: (LoginResponse) -> Unit)
}