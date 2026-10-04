package com.qoody.shared.home

interface HomeRepository {
    suspend fun greeting(): String
}

class DefaultHomeRepository : HomeRepository {
    override suspend fun greeting(): String = "Welcome to Qoody"
}
